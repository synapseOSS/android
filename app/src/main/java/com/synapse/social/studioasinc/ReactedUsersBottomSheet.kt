package com.synapse.social.studioasinc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.model.UserReaction

/**
 * Bottom sheet dialog for showing users who reacted to a post
 */
class ReactedUsersBottomSheet : BottomSheetDialogFragment() {

    private lateinit var reactionTabs: TabLayout
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyState: View
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ReactedUsersAdapter

    private var postId: String = ""
    private var allReactions: List<UserReaction> = emptyList()
    private var onUserClicked: ((String) -> Unit)? = null
    private var onLoadReactions: ((String, ReactionType?) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_reacted_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postId = arguments?.getString(ARG_POST_ID) ?: ""

        initializeViews(view)
        setupRecyclerView()
        setupTabs()
        setupSwipeRefresh()

        loadReactions()
    }

    private fun initializeViews(view: View) {
        reactionTabs = view.findViewById(R.id.reactionTabs)
        usersRecyclerView = view.findViewById(R.id.usersRecyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        emptyState = view.findViewById(R.id.emptyState)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        adapter = ReactedUsersAdapter(requireContext()) { userId ->
            onUserClicked?.invoke(userId)
            dismiss()
        }

        usersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        usersRecyclerView.adapter = adapter
    }

    private fun setupTabs() {
        // Add "All" tab
        reactionTabs.addTab(reactionTabs.newTab().setText("All"))

        // Add tabs for each reaction type that has reactions
        ReactionType.getAllReactions().forEach { reactionType ->
            val tab = reactionTabs.newTab()
                .setText("${reactionType.emoji} ${reactionType.displayName}")
            reactionTabs.addTab(tab)
        }

        reactionTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterReactions(tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadReactions()
        }
    }

    private fun loadReactions() {
        showLoading()
        val selectedTab = reactionTabs.selectedTabPosition
        val reactionType = if (selectedTab == 0) null else ReactionType.getAllReactions()[selectedTab - 1]
        onLoadReactions?.invoke(postId, reactionType)
    }

    private fun filterReactions(tabPosition: Int) {
        if (allReactions.isEmpty()) {
            showEmptyState()
            return
        }

        val filtered = if (tabPosition == 0) {
            // Show all reactions
            allReactions
        } else {
            // Filter by specific reaction type
            val reactionType = ReactionType.getAllReactions()[tabPosition - 1]
            allReactions.filter { it.reactionType == reactionType.name }
        }

        if (filtered.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            adapter.submitList(filtered)
        }
    }

    fun setReactions(reactions: List<UserReaction>) {
        allReactions = reactions
        swipeRefresh.isRefreshing = false
        hideLoading()

        if (reactions.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            filterReactions(reactionTabs.selectedTabPosition)
            updateTabCounts(reactions)
        }
    }

    private fun updateTabCounts(reactions: List<UserReaction>) {
        // Update tab texts with counts
        reactionTabs.getTabAt(0)?.text = "All (${reactions.size})"

        ReactionType.getAllReactions().forEachIndexed { index, reactionType ->
            val count = reactions.count { it.reactionType == reactionType.name }
            if (count > 0) {
                reactionTabs.getTabAt(index + 1)?.text = "${reactionType.emoji} $count"
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        usersRecyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        usersRecyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        usersRecyclerView.visibility = View.VISIBLE
    }

    fun setOnUserClickedListener(listener: (String) -> Unit) {
        onUserClicked = listener
    }

    fun setOnLoadReactionsListener(listener: (String, ReactionType?) -> Unit) {
        onLoadReactions = listener
    }

    companion object {
        private const val ARG_POST_ID = "post_id"

        fun newInstance(
            postId: String,
            onUserClicked: (String) -> Unit,
            onLoadReactions: (String, ReactionType?) -> Unit
        ): ReactedUsersBottomSheet {
            return ReactedUsersBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_POST_ID, postId)
                }
                this.onUserClicked = onUserClicked
                this.onLoadReactions = onLoadReactions
            }
        }
    }
}
