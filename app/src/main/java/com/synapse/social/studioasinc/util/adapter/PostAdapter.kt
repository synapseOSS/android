package com.synapse.social.studioasinc.util.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.PostRepository
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
    private val onShareClicked: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var posts = mutableListOf<Post>()

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
    }

    override fun getItemCount(): Int = posts.size

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
            postContent.text = post.postText ?: ""
            
            post.postImage?.let { imageUrl ->
                postImage.visibility = View.VISIBLE
                Glide.with(context).load(imageUrl).into(postImage)
            } ?: run {
                postImage.visibility = View.GONE
            }

            lifecycleOwner.lifecycleScope.launch {
                userRepository.getUserById(post.authorUid)
                    .onSuccess { user -> authorName.text = user?.username ?: "Unknown User" }
                    .onFailure { authorName.text = "Unknown User" }
            }
            
            loadLikeStatus(post)
            
            likeButton.setOnClickListener { handleLikeClick(post) }
            commentButton.setOnClickListener { onCommentClicked?.invoke(post) }
            shareButton.setOnClickListener { onShareClicked?.invoke(post) }
            moreButton.setOnClickListener { onMoreOptionsClicked?.invoke(post) }
            commentCount.text = post.commentsCount.toString()
        }
        
        private fun loadLikeStatus(post: Post) {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    if (authRepository.isUserLoggedIn()) {
                        val currentUserId = authRepository.getCurrentUserId()
                        if (currentUserId != null) {
                            postRepository.getUserReaction(post.id, currentUserId)
                                .onSuccess { reaction -> updateLikeIcon(reaction != null) }
                        }
                    }
                    
                    postRepository.getReactionSummary(post.id)
                        .onSuccess { summary -> likeCount.text = summary.values.sum().toString() }
                } catch (e: Exception) {
                    android.util.Log.e("PostAdapter", "Failed to load like status", e)
                }
            }
        }
        
        private fun handleLikeClick(post: Post) {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    if (!authRepository.isUserLoggedIn()) {
                        android.widget.Toast.makeText(context, "Please login to like posts", android.widget.Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    
                    val currentUserId = authRepository.getCurrentUserId()
                    if (currentUserId == null) {
                        android.widget.Toast.makeText(context, "Failed to get user information", android.widget.Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    
                    postRepository.toggleReaction(post.id, currentUserId, ReactionType.LIKE)
                        .onSuccess {
                            postRepository.getUserReaction(post.id, currentUserId)
                                .onSuccess { reaction -> updateLikeIcon(reaction != null) }
                            postRepository.getReactionSummary(post.id)
                                .onSuccess { summary -> likeCount.text = summary.values.sum().toString() }
                        }
                        .onFailure { error ->
                            android.util.Log.e("PostAdapter", "Failed to toggle reaction", error)
                            android.widget.Toast.makeText(context, "Failed to like post: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                } catch (e: Exception) {
                    android.util.Log.e("PostAdapter", "Error handling like click", e)
                    android.widget.Toast.makeText(context, "An error occurred", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        private fun updateLikeIcon(isLiked: Boolean) {
            if (isLiked) {
                likeIcon.setImageResource(R.drawable.post_icons_1_2)
                likeIcon.setColorFilter(context.getColor(android.R.color.holo_red_light))
            } else {
                likeIcon.setImageResource(R.drawable.post_icons_1_1)
                likeIcon.clearColorFilter()
            }
        }
    }
}
