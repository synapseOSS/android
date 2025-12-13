package com.synapse.social.studioasinc

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.synapse.social.studioasinc.animations.AnimationConfig
import com.synapse.social.studioasinc.animations.PostCardAnimations
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.util.NumberFormatter.formatCount
import io.noties.markwon.Markwon

/**
 * Adapter for displaying posts in a RecyclerView.
 * Supports markdown rendering, user interactions, efficient list updates via DiffUtil,
 * and Material Design 3 animations.
 */
class PostsAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val markwon: Markwon? = null,
    private val onLikeClicked: ((Post) -> Unit)? = null,
    private val onCommentClicked: ((Post) -> Unit)? = null,
    private val onShareClicked: ((Post) -> Unit)? = null,
    private val onMoreOptionsClicked: ((Post) -> Unit)? = null,
    private val onFavoriteClicked: ((Post) -> Unit)? = null,
    private val onUserClicked: ((String) -> Unit)? = null,
    private val enableMD3Animations: Boolean = true,
    private val animationConfig: AnimationConfig = AnimationConfig.DEFAULT
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_md3, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }
    
    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelAnimations()
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardContainer: View = itemView.findViewById(R.id.cardContainer)
        private val contentText: TextView = itemView.findViewById(R.id.postContent)
        private val authorText: TextView = itemView.findViewById(R.id.authorName)
        private val authorAvatar: View = itemView.findViewById(R.id.authorAvatar)
        private val likeButton: View = itemView.findViewById(R.id.likeButton)
        private val commentButton: View = itemView.findViewById(R.id.commentButton)
        private val shareButton: View = itemView.findViewById(R.id.shareButton)
        private val moreButton: View = itemView.findViewById(R.id.postOptions)
        private val postTimestamp: TextView? = itemView.findViewById(R.id.postTimestamp)
        
        // Track if entrance animation has been played
        private var hasAnimatedEntrance = false
        
        // Track previous state for detecting changes
        private var previousPostText: String? = null
        private var previousLikesCount: Int = 0
        private var previousCommentsCount: Int = 0
        private var isLiked: Boolean = false

        fun bind(post: Post, position: Int) {
            // Detect content changes and animate if needed
            val postContent = post.postText ?: ""
            val hasContentChanged = previousPostText != null && previousPostText != postContent

            if (hasContentChanged && enableMD3Animations) {
                // Animate content update with cross-fade
                PostCardAnimations.animateContentUpdate(contentText, animationConfig) {
                    updatePostContent(postContent)
                }
            } else {
                // No animation needed for first bind or when animations disabled
                updatePostContent(postContent)
            }
            previousPostText = postContent

            val authorUsername = post.username ?: "@${post.authorUid}"
            authorText.text = authorUsername

            // Load author avatar
            if (authorAvatar is ImageView) {
                com.synapse.social.studioasinc.util.ImageLoader.loadImage(
                    context = context,
                    url = post.avatarUrl,
                    imageView = authorAvatar,
                    placeholder = R.drawable.avatar
                )
            }

            // Set accessibility content description for card
            cardContainer.contentDescription = context.getString(R.string.post_by_user, authorUsername)
            
            // Set accessibility content description for avatar
            authorAvatar.contentDescription = context.getString(R.string.author_avatar) + " " + authorUsername
            
            // Detect and animate like count changes
            val hasLikesChanged = previousLikesCount != 0 && previousLikesCount != post.likesCount
            if (hasLikesChanged && enableMD3Animations) {
                PostCardAnimations.animateCountChange(likeButton, animationConfig) {
                    updateLikeButton(post.likesCount, isLiked)
                }
            } else {
                updateLikeButton(post.likesCount, isLiked)
            }
            previousLikesCount = post.likesCount
            
            // Detect and animate comment count changes
            val hasCommentsChanged = previousCommentsCount != 0 && previousCommentsCount != post.commentsCount
            if (hasCommentsChanged && enableMD3Animations) {
                PostCardAnimations.animateCountChange(commentButton, animationConfig) {
                    updateCommentButton(post.commentsCount)
                }
            } else {
                updateCommentButton(post.commentsCount)
            }
            previousCommentsCount = post.commentsCount
            
            // Set timestamp accessibility
            postTimestamp?.let { timestamp ->
                val timeText = timestamp.text.toString()
                timestamp.contentDescription = context.getString(R.string.post_timestamp, timeText)
            }

            // Set click listeners with null safety and button animations
            likeButton.setOnClickListener { 
                // Toggle like state
                isLiked = !isLiked
                
                // Update accessibility content description
                updateLikeButton(post.likesCount, isLiked)
                
                if (enableMD3Animations) {
                    PostCardAnimations.animateLikeStateChange(it, isLiked, animationConfig)
                }
                onLikeClicked?.invoke(post)
            }
            
            commentButton.setOnClickListener { 
                if (enableMD3Animations) {
                    PostCardAnimations.animateButtonClick(it, animationConfig)
                }
                onCommentClicked?.invoke(post)
            }
            
            shareButton.setOnClickListener { 
                if (enableMD3Animations) {
                    PostCardAnimations.animateButtonClick(it, animationConfig)
                }
                onShareClicked?.invoke(post)
            }
            
            moreButton.setOnClickListener { onMoreOptionsClicked?.invoke(post) }
            authorText.setOnClickListener { onUserClicked?.invoke(post.authorUid) }
            authorAvatar.setOnClickListener { onUserClicked?.invoke(post.authorUid) }
            
            // Apply entrance animation if enabled and not yet animated
            if (enableMD3Animations && !hasAnimatedEntrance) {
                PostCardAnimations.animateEntrance(cardContainer, position, animationConfig)
                hasAnimatedEntrance = true
            }
            
            // Add touch listener for press/release animations on card
            if (enableMD3Animations) {
                setupCardTouchListener()
            }
        }
        
        /**
         * Update post content with markdown support
         */
        private fun updatePostContent(content: String) {
            if (markwon != null && content.isNotEmpty()) {
                markwon.setMarkdown(contentText, content)
            } else {
                contentText.text = content
            }
        }
        
        /**
         * Update like button text with formatted count and accessibility
         */
        private fun updateLikeButton(count: Int, liked: Boolean) {
            if (likeButton is TextView) {
                likeButton.text = if (count > 0) formatCount(count) else ""
            }
            
            // Update accessibility content description
            likeButton.contentDescription = if (liked) {
                context.getString(R.string.like_post_liked, count)
            } else {
                context.getString(R.string.like_post_with_count, count)
            }
        }
        
        /**
         * Update comment button text with formatted count and accessibility
         */
        private fun updateCommentButton(count: Int) {
            if (commentButton is TextView) {
                commentButton.text = if (count > 0) formatCount(count) else ""
            }
            
            // Update accessibility content description
            commentButton.contentDescription = context.getString(R.string.comment_on_post_with_count, count)
        }
        
        /**
         * Setup touch listener for card press/release animations
         */
        private fun setupCardTouchListener() {
            cardContainer.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        PostCardAnimations.animatePress(view, animationConfig)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        PostCardAnimations.animateRelease(view, animationConfig)
                    }
                }
                false // Don't consume the event, let it propagate
            }
        }
        
        /**
         * Cancel all animations to prevent memory leaks
         */
        fun cancelAnimations() {
            PostCardAnimations.cancelAnimations(cardContainer)
            PostCardAnimations.cancelAnimations(contentText)
            PostCardAnimations.cancelAnimations(likeButton)
            PostCardAnimations.cancelAnimations(commentButton)
            PostCardAnimations.cancelAnimations(shareButton)
            
            // Reset entrance animation flag for reuse
            hasAnimatedEntrance = false
            
            // Reset state tracking
            previousPostText = null
            previousLikesCount = 0
            previousCommentsCount = 0
            isLiked = false
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
