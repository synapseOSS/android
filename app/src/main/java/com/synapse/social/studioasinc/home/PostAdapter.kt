package com.synapse.social.studioasinc.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.ReactionType
import kotlinx.coroutines.launch
import android.widget.LinearLayout

class PostAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val authRepository: AuthRepository = AuthRepository(),
    private val postRepository: PostRepository = PostRepository(AppDatabase.getDatabase(context).postDao()),
    private val userRepository: UserRepository = UserRepository(AppDatabase.getDatabase(context).userDao()),
    private val onMoreOptionsClicked: ((Post) -> Unit)? = null,
    private val onCommentClicked: ((Post) -> Unit)? = null,
    private val onShareClicked: ((Post) -> Unit)? = null,
    private val onPostClicked: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_POST = 0
        private const val VIEW_TYPE_LOADING = 1
        private const val VIEW_TYPE_END_OF_LIST = 2
    }

    private var posts = mutableListOf<Post>()
    private var isLoadingMore = false
    private var isAtEnd = false

    /**
     * DiffUtil.ItemCallback for efficient list updates
     * Implements proper areItemsTheSame() and areContentsTheSame()
     */
    private class PostDiffCallback(
        private val oldList: List<Post>,
        private val newList: List<Post>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldPost = oldList[oldItemPosition]
            val newPost = newList[newItemPosition]
            return oldPost.postText == newPost.postText &&
                   oldPost.postImage == newPost.postImage &&
                   oldPost.likesCount == newPost.likesCount &&
                   oldPost.commentsCount == newPost.commentsCount &&
                   oldPost.timestamp == newPost.timestamp
        }
    }

    /**
     * Update posts using DiffUtil for efficient list updates without full refresh
     */
    fun updatePosts(newPosts: List<Post>) {
        val diffCallback = PostDiffCallback(posts, newPosts)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        posts.clear()
        posts.addAll(newPosts)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setLoadingMore(loading: Boolean) {
        val wasLoading = isLoadingMore
        isLoadingMore = loading
        
        if (loading && !wasLoading) {
            notifyItemInserted(itemCount)
        } else if (!loading && wasLoading) {
            notifyItemRemoved(itemCount)
        }
    }

    fun setEndOfList(atEnd: Boolean) {
        val wasAtEnd = isAtEnd
        isAtEnd = atEnd
        
        if (atEnd && !wasAtEnd) {
            // Remove loading indicator if present
            if (isLoadingMore) {
                isLoadingMore = false
            }
            notifyItemInserted(itemCount)
        } else if (!atEnd && wasAtEnd) {
            notifyItemRemoved(itemCount)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < posts.size -> VIEW_TYPE_POST
            isAtEnd -> VIEW_TYPE_END_OF_LIST
            isLoadingMore -> VIEW_TYPE_LOADING
            else -> VIEW_TYPE_POST
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LOADING -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_loading_indicator, parent, false)
                LoadingViewHolder(view)
            }
            VIEW_TYPE_END_OF_LIST -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_end_of_list, parent, false)
                EndOfListViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
                PostViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PostViewHolder && position < posts.size) {
            val post = posts[position]
            holder.bind(post)
        }
        // LoadingViewHolder doesn't need binding
    }

    override fun getItemCount(): Int {
        var count = posts.size
        if (isLoadingMore) count++
        if (isAtEnd) count++
        return count
    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    
    class EndOfListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postContent: TextView = itemView.findViewById(R.id.postContent)
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)
        private val authorName: TextView = itemView.findViewById(R.id.authorName)
        private val likeButton: LinearLayout = itemView.findViewById(R.id.likeButton)
        private val likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
        private val likeCount: TextView = itemView.findViewById(R.id.likeCount)
        private val commentButton: LinearLayout = itemView.findViewById(R.id.commentButton)
        private val commentCount: TextView = itemView.findViewById(R.id.commentCount)
        private val shareButton: ImageView = itemView.findViewById(R.id.shareButton)
        private val moreButton: ImageView = itemView.findViewById(R.id.postOptions)

        fun bind(post: Post) {
            // Set post content
            postContent.text = post.postText ?: ""
            
            // Load post image if available
            val imageUrl = post.postImage
            if (!imageUrl.isNullOrBlank()) {
                postImage.visibility = View.VISIBLE
                Glide.with(context)
                    .load(imageUrl)
                    .into(postImage)
            } else {
                postImage.visibility = View.GONE
            }

            // Load author information
            lifecycleOwner.lifecycleScope.launch {
                userRepository.getUserById(post.authorUid)
                    .onSuccess { user ->
                        authorName.text = user?.username ?: "Unknown User"
                    }
                    .onFailure {
                        authorName.text = "Unknown User"
                    }
            }
            
            // Load like status and count
            loadLikeStatus(post)
            
            // Set up click listeners
            likeButton.setOnClickListener {
                handleLikeClick(post)
            }
            
            commentButton.setOnClickListener {
                onCommentClicked?.invoke(post)
            }
            
            shareButton.setOnClickListener {
                onShareClicked?.invoke(post)
            }
            
            moreButton.setOnClickListener {
                onMoreOptionsClicked?.invoke(post)
            }
            
            // Open detailed post view when clicking on post content or image
            postContent.setOnClickListener {
                onPostClicked?.invoke(post)
            }
            
            postImage.setOnClickListener {
                onPostClicked?.invoke(post)
            }
            
            // Set comment count
            commentCount.text = post.commentsCount.toString()
        }
        
        private fun loadLikeStatus(post: Post) {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    // Check if user is logged in first
                    if (authRepository.isUserLoggedIn()) {
                        val currentUserUid = authRepository.getCurrentUserUid()
                        if (currentUserUid != null) {
                            // Check if user has reacted to this post
                            postRepository.getUserReaction(post.id, currentUserUid)
                                .onSuccess { reaction ->
                                    updateLikeIcon(reaction != null)
                                }
                        }
                    }
                    
                    // Get reaction count (always show this regardless of login status)
                    postRepository.getReactionSummary(post.id)
                        .onSuccess { summary ->
                            likeCount.text = summary.values.sum().toString()
                        }
                } catch (e: Exception) {
                    android.util.Log.e("PostAdapter", "Failed to load like status", e)
                }
            }
        }
        
        private fun handleLikeClick(post: Post) {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    android.util.Log.d("PostAdapter", "=== LIKE BUTTON CLICKED ===")
                    android.util.Log.d("PostAdapter", "Post ID: ${post.id}")
                    
                    val isLoggedIn = authRepository.isUserLoggedIn()
                    if (!isLoggedIn) {
                        android.widget.Toast.makeText(context, "Please login to like posts", android.widget.Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    
                    val currentUserUid = authRepository.getCurrentUserUid()
                    if (currentUserUid == null) {
                        android.widget.Toast.makeText(context, "Failed to get user information", android.widget.Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    
                    // Toggle reaction using PostRepository
                    postRepository.toggleReaction(post.id, currentUserUid, ReactionType.LIKE)
                        .onSuccess {
                            android.util.Log.d("PostAdapter", "Reaction toggled successfully")
                            // Refresh like status
                            postRepository.getUserReaction(post.id, currentUserUid)
                                .onSuccess { reaction -> updateLikeIcon(reaction != null) }
                            
                            // Update reaction count
                            postRepository.getReactionSummary(post.id)
                                .onSuccess { summary -> likeCount.text = summary.values.sum().toString() }
                        }
                        .onFailure { error ->
                            android.util.Log.e("PostAdapter", "Failed to toggle reaction: ${error.message}", error)
                            android.widget.Toast.makeText(context, "Failed to like post: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                } catch (e: Exception) {
                    android.util.Log.e("PostAdapter", "Error handling like click: ${e.message}", e)
                    android.widget.Toast.makeText(context, "An error occurred", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        private fun updateLikeIcon(isLiked: Boolean) {
            if (isLiked) {
                likeIcon.setImageResource(R.drawable.post_icons_1_2) // Filled heart
                likeIcon.setColorFilter(context.getColor(android.R.color.holo_red_light))
            } else {
                likeIcon.setImageResource(R.drawable.post_icons_1_1) // Outline heart
                likeIcon.clearColorFilter()
            }
        }
    }
}
