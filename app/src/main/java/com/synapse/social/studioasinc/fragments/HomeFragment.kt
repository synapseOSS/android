package com.synapse.social.studioasinc.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.home.HeaderAdapter
import com.synapse.social.studioasinc.home.HomeViewModel
import com.synapse.social.studioasinc.FeedPostsAdapter
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.ReactionPickerBottomSheet
import com.synapse.social.studioasinc.ReactedUsersBottomSheet
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.PostDetailActivity
import com.synapse.social.studioasinc.PostStatisticsBottomSheet
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.BuildConfig
import android.content.Intent
import androidx.paging.LoadState
import com.synapse.social.studioasinc.adapters.PostLoadStateAdapter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.auth
class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var postAdapter: FeedPostsAdapter
    private lateinit var headerAdapter: HeaderAdapter

    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var publicPostsList: RecyclerView
    private lateinit var loadingBar: ProgressBar
    private lateinit var shimmerContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    
    // Reuse single PostRepository instance to avoid creating new ones on each reaction
    private val postRepository by lazy { PostRepository(AppDatabase.getDatabase(requireContext()).postDao()) }
    private val pollRepository by lazy { com.synapse.social.studioasinc.data.repository.PollRepository() }

    private val SHIMMER_ITEM_COUNT = 5

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView()
        setupViewModel()
        setupListeners()
        setupFragmentResultListener()
    }

    private fun setupFragmentResultListener() {
        parentFragmentManager.setFragmentResultListener("post_action", viewLifecycleOwner) { key, bundle ->
            if (key == "post_action") {
                val action = bundle.getString("action")
                if (action == "delete") {
                    postAdapter.refresh()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Save scroll position when navigating away
        val layoutManager = publicPostsList.layoutManager as? LinearLayoutManager
        layoutManager?.let {
            val position = it.findFirstVisibleItemPosition()
            val view = it.findViewByPosition(position)
            val offset = view?.top ?: 0
            viewModel.saveScrollPosition(position, offset)
        }
    }

    override fun onResume() {
        super.onResume()
        // Restore scroll position when returning
        val scrollPosition = viewModel.restoreScrollPosition()
        scrollPosition?.let {
            val layoutManager = publicPostsList.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(it.position, it.offset)
        }
    }

    private fun initializeViews(view: View) {
        swipeLayout = view.findViewById(R.id.swipeLayout)
        publicPostsList = view.findViewById(R.id.PublicPostsList)
        loadingBar = view.findViewById(R.id.loading_bar)
        shimmerContainer = view.findViewById(R.id.shimmer_container)
        emptyState = view.findViewById(R.id.empty_state)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(HomeViewModel::class.java)
        observePosts()
        observeStories()
    }

    private fun setupRecyclerView() {
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        val currentUserId = currentUser?.id ?: ""

        postAdapter = FeedPostsAdapter(
            context = requireContext(),
            currentUserId = currentUserId,
            onPostClicked = { post -> 
                PostDetailActivity.start(requireContext(), post.id, post.authorUid)
            },
            onLikeClicked = { post ->
                // Default like action (toggle LIKE reaction)
                toggleReaction(post, ReactionType.LIKE)
            },
            onCommentClicked = { post -> showCommentsDialog(post) },
            onShareClicked = { post -> sharePost(post) },
            onUserClicked = { userId ->
                val intent = Intent(requireContext(), com.synapse.social.studioasinc.ProfileActivity::class.java)
                intent.putExtra("uid", userId)
                startActivity(intent)
            },
            onReactionSelected = { post, reactionType ->
                toggleReaction(post, reactionType)
            },
            onReactionSummaryClicked = { post ->
                showReactedUsers(post)
            },
            onReactionPickerRequested = { post, _ ->
                showReactionPicker(post)
            },
            onPollOptionClicked = { post, optionIndex ->
                submitPollVote(post, optionIndex)
            },
            onMoreOptionsClicked = { post ->
                showMoreOptionsDialog(post)
            }
        )
        headerAdapter = HeaderAdapter(requireContext(), this)

        publicPostsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ConcatAdapter(headerAdapter, postAdapter.withLoadStateHeaderAndFooter(
                header = PostLoadStateAdapter { postAdapter.retry() },
                footer = PostLoadStateAdapter { postAdapter.retry() }
            ))
        }

        postAdapter.addLoadStateListener { loadState ->
            val isListEmpty = loadState.refresh is LoadState.NotLoading && postAdapter.itemCount == 0
            emptyState.visibility = if (isListEmpty) View.VISIBLE else View.GONE
            
            // Handle loading state
            swipeLayout.isRefreshing = loadState.refresh is LoadState.Loading
            
            // Handle error state
            if (loadState.refresh is LoadState.Error) {
                val error = (loadState.refresh as LoadState.Error).error
                Toast.makeText(requireContext(), "Error loading posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReactionPicker(post: Post) {
        val picker = ReactionPickerBottomSheet.newInstance { reactionType ->
            toggleReaction(post, reactionType)
        }
        picker.show(parentFragmentManager, "ReactionPicker")
    }

    private fun setupListeners() {
        // Set Material Design 3 color scheme for SwipeRefreshLayout
        swipeLayout.setColorSchemeResources(
            R.color.md_theme_primary,
            R.color.md_theme_secondary,
            R.color.md_theme_tertiary
        )
        
        swipeLayout.setOnRefreshListener {
            // Announce refresh action for accessibility
            swipeLayout.announceForAccessibility(getString(R.string.refreshing_posts))
            postAdapter.refresh()
        }
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.posts.collectLatest {
                postAdapter.submitData(it)
            }
        }
    }

    private fun observeStories() {
        // Stories functionality can be implemented later
        // For now, we'll just show empty stories
    }

    private fun showShimmer() {
        shimmerContainer.removeAllViews()
        shimmerContainer.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(context)
        for (i in 0 until SHIMMER_ITEM_COUNT) {
            val shimmerView = inflater.inflate(R.layout.post_placeholder_layout, shimmerContainer, false)
            shimmerContainer.addView(shimmerView)
        }
    }

    private fun hideShimmer() {
        shimmerContainer.visibility = View.GONE
    }
    
    /**
     * Announce loading state changes for accessibility
     * @param isLoading Whether content is currently loading
     * @param isRefresh Whether this is a refresh operation (pull-to-refresh)
     */
    private fun announceLoadingState(isLoading: Boolean, isRefresh: Boolean) {
        val message = when {
            isLoading && isRefresh -> getString(R.string.loading_more_posts)
            isLoading && !isRefresh -> getString(R.string.loading_more_posts)
            !isLoading && isRefresh -> getString(R.string.posts_loaded)
            else -> getString(R.string.posts_loaded)
        }
        requireView().announceForAccessibility(message)
    }
    
    /**
     * Announce when end of list is reached for accessibility
     */
    private fun announceEndOfList() {
        requireView().announceForAccessibility(getString(R.string.no_more_posts_available))
    }
    
    private fun showMoreOptionsDialog(post: Post) {
        com.synapse.social.studioasinc.PostMoreBottomSheetDialog.newInstance(
            post.id,
            post.authorUid,
            post.postType ?: "text",
            post.postImage,
            post.postText
        ).show(parentFragmentManager, "PostMoreOptions")
    }
    
    private fun showCommentsDialog(post: Post) {
        val intent = Intent(requireContext(), PostDetailActivity::class.java).apply {
            putExtra("postKey", post.id)
            putExtra("postAuthorUid", post.authorUid)
        }
        startActivity(intent)
    }
    
    private fun sharePost(post: Post) {
        val shareText = "${post.postText}\n\nShared via Synapse"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share post via"))
    }
    
    private fun editPost(post: Post) {
        val editIntent = Intent(requireActivity(), com.synapse.social.studioasinc.EditPostActivity::class.java).apply {
            putExtra("postKey", post.id)
            putExtra("postText", post.postText)
            putExtra("postImg", post.postImage)
        }
        startActivity(editIntent)
    }
    
    private fun deletePost(post: Post) {
        lifecycleScope.launch {
            try {
                // Delete from Supabase
                SupabaseClient.client.from("posts").delete {
                    filter {
                        eq("id", post.id)
                    }
                }
                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show()
                // Refresh posts
                postAdapter.refresh()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to delete post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun copyPostLink(post: Post) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Post Link", "${BuildConfig.APP_DOMAIN}/post/${post.id}")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Link copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    private fun showPostStatistics(post: Post) {
        val statisticsSheet = PostStatisticsBottomSheet.newInstance(post)
        statisticsSheet.show(parentFragmentManager, "PostStatistics")
    }
    
    private fun reportPost(post: Post) {
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser != null) {
                    val reportData = mapOf(
                        "reporter_uid" to currentUser.id,
                        "reported_post_id" to post.id,
                        "reported_user_uid" to post.authorUid,
                        "report_reason" to "Inappropriate content",
                        "created_at" to System.currentTimeMillis()
                    )
                    SupabaseClient.client.from("reports").insert(reportData)
                    Toast.makeText(requireContext(), "Post reported. Thank you for keeping Synapse safe.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to report post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun hidePost(post: Post) {
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser != null) {
                    val hideData = mapOf(
                        "user_uid" to currentUser.id,
                        "hidden_post_id" to post.id,
                        "hidden_at" to System.currentTimeMillis()
                    )
                    SupabaseClient.client.from("hidden_posts").insert(hideData)
                    Toast.makeText(requireContext(), "Post hidden. You won't see posts like this.", Toast.LENGTH_SHORT).show()
                    // Refresh posts
                    postAdapter.refresh()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to hide post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitPollVote(post: Post, optionIndex: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = pollRepository.submitVote(post.id, optionIndex)
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), getString(R.string.poll_vote_submitted), Toast.LENGTH_SHORT).show()
                    // postAdapter.refresh() // Disabled to prevent list flashing. UI is updated optimistically.
                } else {
                    val errorMessage = getString(R.string.poll_vote_failed, result.exceptionOrNull()?.message)
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Failed to submit vote", e)
            }
        }
    }

    private fun toggleReaction(post: Post, reactionType: ReactionType) {
        val currentUser = SupabaseClient.client.auth.currentUserOrNull() ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = postRepository.toggleReaction(post.id, currentUser.id, reactionType)
                
                if (result.isSuccess) {
                    // Refresh posts to update UI
                    // Ideally we should just update the single item in the adapter
                    postAdapter.refresh()
                } else {
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Failed to react", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showReactedUsers(post: Post) {
        val bottomSheet = ReactedUsersBottomSheet.newInstance(
            postId = post.id,
            onUserClicked = { userId ->
                val intent = Intent(requireContext(), com.synapse.social.studioasinc.ProfileActivity::class.java)
                intent.putExtra("uid", userId)
                startActivity(intent)
            },
            onLoadReactions = { postId, reactionType ->
                loadReactionsForSheet(postId, reactionType)
            }
        )
        bottomSheet.show(parentFragmentManager, "ReactedUsersBottomSheet")
    }

    private fun loadReactionsForSheet(postId: String, reactionType: ReactionType?) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = postRepository.getUsersWhoReacted(postId, reactionType)
                
                // Check if fragment is still attached before accessing views
                if (!isAdded || context == null) return@launch
                
                val sheet = parentFragmentManager.findFragmentByTag("ReactedUsersBottomSheet") as? ReactedUsersBottomSheet
                
                if (result.isSuccess) {
                    sheet?.setReactions(result.getOrDefault(emptyList()))
                } else {
                    Toast.makeText(requireContext(), "Failed to load reactions", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}


