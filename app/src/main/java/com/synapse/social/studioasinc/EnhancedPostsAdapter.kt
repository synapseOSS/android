package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.synapse.social.studioasinc.animations.ReactionAnimations
import com.synapse.social.studioasinc.components.MediaGridView
import com.synapse.social.studioasinc.components.PollDisplay
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.util.ImageLoader
import com.synapse.social.studioasinc.util.TimeUtils.formatTimestamp

/**
 * Enhanced adapter for displaying posts with reactions and multi-media support
 */
class EnhancedPostsAdapter(
    private val context: Context,
    private val currentUserId: String,
    private val onPostClicked: ((Post) -> Unit)? = null,
    private val onLikeClicked: ((Post) -> Unit)? = null,
    private val onCommentClicked: ((Post) -> Unit)? = null,
    private val onShareClicked: ((Post) -> Unit)? = null,
    private val onUserClicked: ((String) -> Unit)? = null,
    private val onReactionSelected: ((Post, ReactionType) -> Unit)? = null,
    private val onReactionSummaryClicked: ((Post) -> Unit)? = null,
    private val onReactionPickerRequested: ((Post, View) -> Unit)? = null,
    private val onReactionToggled: ((Post, ReactionType, (Boolean) -> Unit) -> Unit)? = null,
    private val onPollOptionClicked: ((Post, Int) -> Unit)? = null,
    private val onMoreOptionsClicked: ((Post) -> Unit)? = null
) : PagingDataAdapter<Post, EnhancedPostsAdapter.PostViewHolder>(PostDiffCallback()) {

    /**
     * Update a post's reaction state optimistically
     * This updates the UI immediately before the server responds
     */
    fun updatePostReactionOptimistically(postId: String, reactionType: ReactionType, isAdding: Boolean) {
        notifyDataSetChanged()
    }

    /**
     * Revert a post's reaction state if the server operation failed
     */
    fun revertPostReaction(postId: String, originalPost: Post) {
        notifyDataSetChanged()
    }

    /**
     * Handle reaction selection from external sources (e.g., reaction picker)
     * This method provides optimistic UI updates with reversion on failure
     */
    fun handleReactionSelection(post: Post, reactionType: ReactionType, onResult: (Boolean) -> Unit) {
        // Store original post state
        val originalPost = post.copy()
        
        // Determine if we're adding or removing the reaction
        val isAdding = post.userReaction != reactionType
        
        // Perform optimistic UI update
        updatePostReactionOptimistically(post.id, reactionType, isAdding)
        
        // Notify callback with result handler
        onReactionToggled?.invoke(post, reactionType) { success ->
            if (!success) {
                // Revert on failure
                revertPostReaction(post.id, originalPost)
            }
            onResult(success)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_enhanced, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        if (post != null) {
            holder.bind(post)
        }
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postCard: MaterialCardView = itemView.findViewById(R.id.postCard)
        private val authorAvatar: ImageView = itemView.findViewById(R.id.authorAvatar)
        private val authorName: TextView = itemView.findViewById(R.id.authorName)
        private val postTimestamp: TextView = itemView.findViewById(R.id.postTimestamp)
        private val postOptions: View = itemView.findViewById(R.id.postOptions)
        private val postContent: TextView = itemView.findViewById(R.id.postContent)
        private val readMoreButton: View = itemView.findViewById(R.id.readMoreButton)
        private val pollComposeView: ComposeView = itemView.findViewById(R.id.pollComposeView)
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)
        private val mediaGridView: MediaGridView = itemView.findViewById(R.id.mediaGridView)

        private val pollPostState = androidx.compose.runtime.mutableStateOf<Post?>(null)

        init {
            pollComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            pollComposeView.setContent {
                val post = pollPostState.value
                if (post != null) {
                    PollDisplay(
                        post = post,
                        userPollVote = post.userPollVote,
                        onOptionSelected = { selectedIndex ->
                            handlePollVote(post, selectedIndex)
                        }
                    )
                }
            }
        }
        private val reactionSummaryContainer: ViewGroup = itemView.findViewById(R.id.reactionSummaryContainer)
        private val reactionSummary: View = itemView.findViewById(R.id.reactionSummary)
        private val reactionEmojis: TextView = itemView.findViewById(R.id.reactionEmojis)
        private val reactionCount: TextView = itemView.findViewById(R.id.reactionCount)
        private val commentsCountText: TextView = itemView.findViewById(R.id.commentsCountText)
        private val divider: View = itemView.findViewById(R.id.divider)
        private val likeButton: View = itemView.findViewById(R.id.likeButton)
        private val likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
        private val likeText: TextView = itemView.findViewById(R.id.likeText)
        private val commentButton: View = itemView.findViewById(R.id.commentButton)
        private val shareButton: View = itemView.findViewById(R.id.shareButton)

        private var currentPost: Post? = null
        private var isContentExpanded = false

        fun bind(post: Post) {
            currentPost = post

            // Bind author info with username from joined profile data
            authorName.text = post.username ?: "Unknown User"
            postTimestamp.text = formatTimestamp(post.timestamp)

            // Load avatar using ImageLoader with retry logic
            ImageLoader.loadImage(
                context = context,
                url = post.avatarUrl,
                imageView = authorAvatar,
                placeholder = R.drawable.avatar
            )

            // Bind post content
            bindContent(post)

            // Bind poll
            bindPoll(post)

            // Bind media using MediaGridView
            bindMedia(post)

            // Bind reactions
            bindReactions(post)

            // Setup click listeners
            setupClickListeners(post)
        }

        private fun bindContent(post: Post) {
            val content = post.postText ?: ""
            postContent.text = content

            // Show/hide read more button
            if (content.length > 200 && !isContentExpanded) {
                postContent.maxLines = 5
                readMoreButton.visibility = View.VISIBLE
                readMoreButton.setOnClickListener {
                    isContentExpanded = true
                    postContent.maxLines = Int.MAX_VALUE
                    readMoreButton.visibility = View.GONE
                }
            } else {
                postContent.maxLines = Int.MAX_VALUE
                readMoreButton.visibility = View.GONE
            }
        }

        private fun bindPoll(post: Post) {
            if (post.hasPoll == true) {
                pollComposeView.visibility = View.VISIBLE
                pollPostState.value = post
            } else {
                pollComposeView.visibility = View.GONE
            }
        }

        private fun handlePollVote(post: Post, optionIndex: Int) {
            val options = post.pollOptions?.map { it.copy() }?.toMutableList() ?: return
            val currentVote = post.userPollVote

            if (currentVote == optionIndex) return

            if (currentVote != null && currentVote < options.size) {
                val oldOption = options[currentVote]
                options[currentVote] = oldOption.copy(votes = maxOf(0, oldOption.votes - 1))
            }

            if (optionIndex < options.size) {
                val newOption = options[optionIndex]
                options[optionIndex] = newOption.copy(votes = newOption.votes + 1)
            }

            val updatedPost = post.copy(
                pollOptions = options,
                userPollVote = optionIndex
            )

            currentPost = updatedPost
            bindPoll(updatedPost)

            onPollOptionClicked?.invoke(post, optionIndex)
        }

        private fun bindMedia(post: Post) {
            val mediaItems = post.mediaItems

            if (mediaItems != null && mediaItems.isNotEmpty()) {
                // Use MediaGridView for all media (single or multiple)
                postImage.visibility = View.GONE
                mediaGridView.visibility = View.VISIBLE

                // Set media items to the grid view
                mediaGridView.setMediaItems(mediaItems)

                // Set up click listener to open full-screen viewer
                mediaGridView.onMediaClickListener = object : MediaGridView.OnMediaClickListener {
                    override fun onMediaClick(mediaItems: List<MediaItem>, position: Int) {
                        openFullScreenViewer(mediaItems, position)
                    }
                }
            } else if (!post.postImage.isNullOrEmpty()) {
                // Legacy single image support
                mediaGridView.visibility = View.GONE
                postImage.visibility = View.VISIBLE

                ImageLoader.loadImage(
                    context = context,
                    url = post.postImage,
                    imageView = postImage,
                    placeholder = R.drawable.default_image
                )
            } else {
                // No media
                postImage.visibility = View.GONE
                mediaGridView.visibility = View.GONE
            }
        }

        private fun openFullScreenViewer(mediaItems: List<MediaItem>, position: Int) {
            // Extract image URLs from media items (filter for images only)
            val imageUrls = mediaItems.filter { it.type == MediaType.IMAGE }.map { it.url }
            val thumbnailUrls = mediaItems
                .filter { it.type == MediaType.IMAGE }
                .mapNotNull { it.thumbnailUrl }
                .takeIf { it.isNotEmpty() }
            
            // Calculate the adjusted position for images only
            val imagePosition = mediaItems.take(position).count { it.type == MediaType.IMAGE }
            
            if (imageUrls.isEmpty()) {
                // No images to display
                return
            }
            
            // Launch ImageGalleryActivity with all media URLs and starting position
            val intent = com.synapse.social.studioasinc.chat.ImageGalleryActivity.createIntent(
                context = itemView.context,
                imageUrls = imageUrls,
                thumbnailUrls = thumbnailUrls,
                initialPosition = imagePosition
            )
            itemView.context.startActivity(intent)
        }

        private fun bindReactions(post: Post) {
            val totalReactions = post.getTotalReactionsCount()
            val hasReactions = totalReactions > 0
            val hasComments = post.commentsCount > 0

            if (hasReactions || hasComments) {
                reactionSummaryContainer.visibility = View.VISIBLE
                divider.visibility = View.VISIBLE

                // Show reaction summary
                if (hasReactions) {
                    val summary = post.getReactionSummary()
                    val topReactions = post.getTopReactions()

                    if (topReactions.isNotEmpty()) {
                        val emojis = topReactions.take(3).joinToString(" ") { it.first.emoji }
                        reactionEmojis.text = emojis
                        reactionCount.text = totalReactions.toString()
                        reactionSummary.visibility = View.VISIBLE
                    } else {
                        reactionSummary.visibility = View.GONE
                    }
                } else {
                    reactionSummary.visibility = View.GONE
                }

                // Show comments count
                if (hasComments) {
                    commentsCountText.visibility = View.VISIBLE
                    commentsCountText.text = "${post.commentsCount} ${if (post.commentsCount == 1) "comment" else "comments"}"
                } else {
                    commentsCountText.visibility = View.GONE
                }
            } else {
                reactionSummaryContainer.visibility = View.GONE
                divider.visibility = View.GONE
            }

            // Update like button based on user's reaction
            val userReaction = post.userReaction
            if (userReaction != null) {
                likeIcon.setImageResource(userReaction.iconRes)
                likeText.text = userReaction.displayName
                likeText.setTextColor(context.getColor(R.color.colorPrimary))
            } else {
                likeIcon.setImageResource(R.drawable.ic_reaction_like)
                likeText.text = "Like"
                likeText.setTextColor(context.getColor(R.color.text_secondary))
            }
        }

        private fun setupClickListeners(post: Post) {
            // Author click
            authorAvatar.setOnClickListener { onUserClicked?.invoke(post.authorUid) }
            authorName.setOnClickListener { onUserClicked?.invoke(post.authorUid) }

            // Post click - open detailed view
            postCard.setOnClickListener { onPostClicked?.invoke(post) }
            postContent.setOnClickListener { onPostClicked?.invoke(post) }
            postImage.setOnClickListener { onPostClicked?.invoke(post) }

            // Like button - single tap with optimistic UI update
            likeButton.setOnClickListener {
                handleReactionClick(post, ReactionType.LIKE)
            }

            // Like button - long press for reaction picker
            likeButton.setOnLongClickListener {
                // Show reaction picker and handle selection with optimistic updates
                onReactionPickerRequested?.invoke(post, likeButton)
                true
            }
            
            // Handle reaction selection from picker
            onReactionSelected?.let { callback ->
                // This will be called when user selects a reaction from the picker
                // The picker should call handleReactionClick with the selected reaction
            }

            // Reaction summary click - show who reacted
            reactionSummary.setOnClickListener {
                onReactionSummaryClicked?.invoke(post)
            }

            // Comment button
            commentButton.setOnClickListener {
                onCommentClicked?.invoke(post)
            }

            // Share button
            shareButton.setOnClickListener {
                onShareClicked?.invoke(post)
            }

            // Options button
            postOptions.setOnClickListener {
                onMoreOptionsClicked?.invoke(post)
            }
        }

        private fun handleReactionClick(post: Post, reactionType: ReactionType) {
            // Store original post state for potential reversion
            val originalPost = post.copy()
            
            // Determine if we're adding or removing the reaction
            val isAdding = post.userReaction != reactionType
            
            // Perform optimistic UI update immediately
            val updatedReactions = post.reactions?.toMutableMap() ?: mutableMapOf()
            
            val currentUserReaction = post.userReaction
            if (currentUserReaction != null) {
                // User already has a reaction - remove it first
                val oldCount = updatedReactions[currentUserReaction] ?: 0
                if (oldCount > 0) {
                    updatedReactions[currentUserReaction] = oldCount - 1
                    if (updatedReactions[currentUserReaction] == 0) {
                        updatedReactions.remove(currentUserReaction)
                    }
                }
            }
            
            if (isAdding) {
                // Add new reaction
                updatedReactions[reactionType] = (updatedReactions[reactionType] ?: 0) + 1
            }
            
            // Update the post object
            val updatedPost = post.copy(
                reactions = updatedReactions.ifEmpty { null },
                userReaction = if (isAdding) reactionType else null
            )
            
            // Update current post reference
            currentPost = updatedPost
            
            // Rebind the UI immediately with optimistic data
            bindReactions(updatedPost)
            
            // Notify the callback with success/failure handler
            onReactionToggled?.invoke(post, reactionType) { success ->
                if (!success) {
                    // Revert UI on failure
                    currentPost = originalPost
                    bindReactions(originalPost)
                }
            }
            
            // Fallback to old callback if new one not provided
            if (onReactionToggled == null) {
                onLikeClicked?.invoke(post)
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
