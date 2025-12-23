package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
// import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.synapse.social.studioasinc.adapters.CommentDetailAdapter
import com.synapse.social.studioasinc.adapters.MediaPagerAdapter
import com.synapse.social.studioasinc.databinding.ActivityPostDetailBinding
import com.synapse.social.studioasinc.model.*
import com.synapse.social.studioasinc.util.TimeUtils
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

class PostDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private val viewModel: PostDetailViewModel by viewModels()
    private lateinit var commentsAdapter: CommentDetailAdapter
    private var replyToCommentId: String? = null
    private var replyToUsername: String? = null
    private var currentPost: Post? = null

    companion object {
        const val EXTRA_POST_ID = "post_id"
        const val EXTRA_AUTHOR_UID = "author_uid"

        fun start(context: Context, postId: String, authorUid: String? = null) {
            context.startActivity(Intent(context, PostDetailActivity::class.java).apply {
                putExtra(EXTRA_POST_ID, postId)
                putExtra(EXTRA_AUTHOR_UID, authorUid)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCommentsRecyclerView()
        setupCommentInput()
        setupClickListeners()
        observeState()

        intent.getStringExtra(EXTRA_POST_ID)?.let { postId ->
            viewModel.loadPost(postId)
            viewModel.loadComments(postId)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupCommentsRecyclerView() {
        commentsAdapter = CommentDetailAdapter(
            onReplyClick = { comment -> setReplyMode(comment) },
            onLikeClick = { comment -> viewModel.toggleCommentReaction(comment.id, ReactionType.LIKE) },
            onUserClick = { userId -> navigateToProfile(userId) },
            onOptionsClick = { comment -> showCommentOptions(comment) },
            onReactionPickerClick = { comment -> showCommentReactionPicker(comment) },
            onLoadReplies = { commentId, callback -> 
                viewModel.loadReplies(commentId)
            }
        )
        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
            adapter = commentsAdapter
        }
    }

    private fun setupCommentInput() {
        binding.etComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.ivSend.alpha = if (s.isNullOrBlank()) 0.38f else 1f
            }
        })

        binding.ivSend.setOnClickListener {
            val content = binding.etComment.text?.toString()?.trim() ?: return@setOnClickListener
            if (content.isNotEmpty()) {
                animateSendButton()
                viewModel.addComment(content, replyToCommentId)
                binding.etComment.text?.clear()
                clearReplyMode()
            }
        }

        binding.ivCancelReply.setOnClickListener { clearReplyMode() }

        // Load current user avatar
        loadCurrentUserAvatar()
    }

    private fun setupClickListeners() {
        binding.btnLike.setOnClickListener { viewModel.toggleReaction(ReactionType.LIKE) }
        binding.btnLike.setOnLongClickListener { showReactionPicker(); true }
        binding.btnComment.setOnClickListener { binding.etComment.requestFocus() }
        binding.btnShare.setOnClickListener { sharePost() }
        binding.ivMoreOptions.setOnClickListener { showMoreOptions(it) }
        binding.ivAuthorAvatar.setOnClickListener { navigateToAuthorProfile() }
        binding.tvAuthorName.setOnClickListener { navigateToAuthorProfile() }
        binding.reactionsContainer.setOnClickListener { showReactedUsers() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.postState.collectLatest { state ->
                when (state) {
                    is PostDetailState.Loading -> showLoading(true)
                    is PostDetailState.Success -> {
                        showLoading(false)
                        displayPost(state.postDetail)
                    }
                    is PostDetailState.Error -> {
                        showLoading(false)
                        Toast.makeText(this@PostDetailActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is PostDetailState.NotFound -> {
                        showLoading(false)
                        Toast.makeText(this@PostDetailActivity, "Post not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.commentsState.collectLatest { state ->
                when (state) {
                    is CommentsState.Loading -> binding.progressComments.isVisible = true
                    is CommentsState.Success -> {
                        binding.progressComments.isVisible = false
                        commentsAdapter.submitList(state.comments)
                        binding.tvNoComments.isVisible = state.comments.isEmpty()
                    }
                    is CommentsState.Error -> {
                        binding.progressComments.isVisible = false
                        Toast.makeText(this@PostDetailActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is CommentsState.Empty -> {
                        binding.progressComments.isVisible = false
                        binding.tvNoComments.isVisible = true
                    }
                }
            }
        }
    }

    private fun displayPost(postDetail: PostDetail) {
        val post = postDetail.post
        currentPost = post
        val author = postDetail.author

        // Author info
        Glide.with(this).load(author?.avatar).placeholder(R.drawable.avatar)
            .into(binding.ivAuthorAvatar)
        binding.tvAuthorName.text = author?.displayName ?: author?.username
        binding.ivVerifiedBadge.isVisible = author?.isVerified ?: false
        val timeAgo = post.publishDate?.let { TimeUtils.getTimeAgo(it) } ?: "Just now"
        binding.tvPostTime.text = "$timeAgo · ${if (post.postVisibility == "public") getString(R.string.public_visibility) else getString(R.string.private_visibility)}"

        // Content
        binding.tvPostContent.text = post.postText
        binding.tvPostContent.isVisible = !post.postText.isNullOrBlank()

        // Media
        setupMedia(post.mediaItems)

        // Poll
        setupPoll(post, postDetail)

        // Engagement stats
        displayReactions(postDetail.reactionSummary, postDetail.userReaction)
        binding.tvCommentCount.text = getString(R.string.comments_count, post.commentsCount)
        binding.tvShareCount.text = "${post.resharesCount} shares"

        // Update like button state
        updateLikeButton(postDetail.userReaction)
    }

    private fun setupMedia(mediaItems: List<MediaItem>?) {
        if (mediaItems.isNullOrEmpty()) {
            binding.mediaContainer.isVisible = false
            return
        }

        binding.mediaContainer.isVisible = true
        val adapter = MediaPagerAdapter(mediaItems) { /* fullscreen */ }
        binding.vpMedia.adapter = adapter

        if (mediaItems.size > 1) {
            binding.tabIndicator.isVisible = true
            TabLayoutMediator(binding.tabIndicator, binding.vpMedia) { _, _ -> }.attach()
        } else {
            binding.tabIndicator.isVisible = false
        }
    }

    private fun setupPoll(post: Post, postDetail: PostDetail) {
        if (post.hasPoll != true) {
            binding.pollContainer.isVisible = false
            return
        }

        binding.pollContainer.isVisible = true
        binding.tvPollQuestion.text = post.pollQuestion

        // Poll options would be set up with a RecyclerView adapter
        val voteCount = postDetail.pollResults?.sumOf { it.voteCount } ?: 0
        binding.tvPollInfo.text = getString(R.string.vote_count, voteCount)
    }

    private fun displayReactions(summary: Map<ReactionType, Int>, userReaction: ReactionType?) {
        val total = summary.values.sum()
        if (total == 0) {
            binding.tvReactionEmojis.isVisible = false
            binding.tvReactionCount.isVisible = false
            return
        }

        binding.tvReactionEmojis.isVisible = true
        binding.tvReactionCount.isVisible = true

        val emojis = summary.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .take(3)
            .joinToString("") { it.key.emoji }

        binding.tvReactionEmojis.text = emojis
        binding.tvReactionCount.text = formatCount(total)
    }

    private fun updateLikeButton(userReaction: ReactionType?) {
        if (userReaction != null) {
            binding.tvLike.text = userReaction.displayName
            binding.tvLike.setTextColor(getColor(R.color.colorPrimary))
        } else {
            binding.tvLike.text = getString(R.string.reaction_like)
            binding.tvLike.setTextColor(getColor(R.color.colorOnSurface))
        }
    }

    private fun setReplyMode(comment: CommentWithUser) {
        replyToCommentId = comment.id
        replyToUsername = comment.user?.username
        binding.replyIndicator.isVisible = true
        binding.tvReplyingTo.text = getString(R.string.replying_to, comment.user?.username ?: "user")
        binding.etComment.hint = getString(R.string.reply_hint, comment.user?.username ?: "user")
        
        // Auto-add @mention
        val mention = "@${comment.user?.username ?: "user"} "
        binding.etComment.setText(mention)
        binding.etComment.setSelection(mention.length)
        binding.etComment.requestFocus()
    }

    private fun clearReplyMode() {
        replyToCommentId = null
        replyToUsername = null
        binding.replyIndicator.isVisible = false
        binding.etComment.hint = getString(R.string.comment_hint)
        binding.etComment.text?.clear()
    }

    private fun showReactionPicker() {
        val picker = ReactionPickerBottomSheet()
        picker.setOnReactionSelectedListener { reactionType ->
            viewModel.toggleReaction(reactionType)
        }
        picker.show(supportFragmentManager, "reaction_picker")
    }

    private fun showCommentReactionPicker(comment: CommentWithUser) {
        val picker = ReactionPickerBottomSheet()
        picker.setOnReactionSelectedListener { reactionType ->
            viewModel.toggleCommentReaction(comment.id, reactionType)
        }
        picker.show(supportFragmentManager, "comment_reaction_picker")
    }

    private fun showMoreOptions(anchor: View) {
        val post = currentPost ?: return
        com.synapse.social.studioasinc.PostMoreBottomSheetDialog.newInstance(
            post.id,
            post.authorUid,
            post.postType ?: "text",
            post.postImage,
            post.postText
        ).show(supportFragmentManager, "PostMoreOptions")
    }

    private fun sharePost() {
        val postId = intent.getStringExtra(EXTRA_POST_ID) ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this post on Synapse: synapse://post/$postId")
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
    }

    private fun showReshareDialog() {
        // Simple reshare - could be expanded to a dialog
        viewModel.createReshare(null)
        Toast.makeText(this, "Post reshared", Toast.LENGTH_SHORT).show()
    }

    private fun showReportDialog() {
        val reasons = arrayOf("Spam", "Harassment", "Hate speech", "Violence", "Other")
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.report_title)
            .setItems(reasons) { _, which ->
                viewModel.reportPost(reasons[which])
                Toast.makeText(this, R.string.report_submitted, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showReactedUsers() {
        // Could show a bottom sheet with users who reacted
    }

    private fun navigateToProfile(userId: String) {
        startActivity(Intent(this, ProfileActivity::class.java).apply {
            putExtra("user_id", userId)
        })
    }

    private fun navigateToAuthorProfile() {
        intent.getStringExtra(EXTRA_AUTHOR_UID)?.let { navigateToProfile(it) }
    }

    private fun loadCurrentUserAvatar() {
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser != null) {
                    val userProfile = SupabaseClient.client.from("users")
                        .select { filter { eq("uid", currentUser.id) } }
                        .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()
                    
                    val avatarUrl = userProfile?.get("avatar")?.jsonPrimitive?.contentOrNull
                    if (!avatarUrl.isNullOrEmpty()) {
                        com.synapse.social.studioasinc.util.ImageLoader.loadImage(
                            context = this@PostDetailActivity,
                            url = avatarUrl,
                            imageView = binding.ivUserAvatar,
                            placeholder = R.drawable.avatar
                        )
                    }
                }
            } catch (e: Exception) {
                // Silently fail, keep default avatar
            }
        }
    }

    private fun animateSendButton() {
        binding.ivSend.animate()
            .scaleX(1.2f).scaleY(1.2f)
            .setDuration(100)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                binding.ivSend.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            .start()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.scrollView.isVisible = !show
    }

    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
    
    private fun showCommentOptions(comment: CommentWithUser) {
        val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        val isOwnComment = comment.userId == currentUserId
        val postAuthorUid = intent.getStringExtra(EXTRA_AUTHOR_UID)
        val isPostAuthor = currentUserId == postAuthorUid
        
        val bottomSheet = com.synapse.social.studioasinc.ui.CommentOptionsBottomSheet(
            comment = comment,
            isOwnComment = isOwnComment,
            isPostAuthor = isPostAuthor,
            onActionSelected = { action -> handleCommentAction(action) }
        )
        bottomSheet.show(supportFragmentManager, com.synapse.social.studioasinc.ui.CommentOptionsBottomSheet.TAG)
    }
    
    private fun handleCommentAction(action: CommentAction) {
        lifecycleScope.launch {
            when (action) {
                is CommentAction.Reply -> setReplyMode(commentsAdapter.currentList.find { it.id == action.commentId } ?: return@launch)
                is CommentAction.Copy -> Toast.makeText(this@PostDetailActivity, R.string.comment_copied, Toast.LENGTH_SHORT).show()
                is CommentAction.Share -> Toast.makeText(this@PostDetailActivity, "Comment shared", Toast.LENGTH_SHORT).show()
                is CommentAction.Hide -> {
                    viewModel.hideComment(action.commentId)
                    Toast.makeText(this@PostDetailActivity, R.string.comment_hidden, Toast.LENGTH_SHORT).show()
                }
                is CommentAction.Report -> {
                    viewModel.reportComment(action.commentId, action.reason, action.description)
                    Toast.makeText(this@PostDetailActivity, R.string.comment_reported, Toast.LENGTH_SHORT).show()
                }
                is CommentAction.Pin -> {
                    viewModel.pinComment(action.commentId, action.postId)
                    Toast.makeText(this@PostDetailActivity, R.string.comment_pinned, Toast.LENGTH_SHORT).show()
                }
                is CommentAction.Delete -> {
                    showDeleteCommentDialog(action.commentId)
                }
                is CommentAction.Edit -> {
                    showEditCommentDialog(action.commentId, action.newContent)
                }
            }
        }
    }
    
    private fun showDeleteCommentDialog(commentId: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_comment)
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteComment(commentId)
                Toast.makeText(this, R.string.comment_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun showEditCommentDialog(commentId: String, currentContent: String) {
        val editText = android.widget.EditText(this).apply {
            setText(currentContent)
            setSelection(currentContent.length)
        }
        
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.edit_comment)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    viewModel.editComment(commentId, newContent)
                    Toast.makeText(this, R.string.comment_edited, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
