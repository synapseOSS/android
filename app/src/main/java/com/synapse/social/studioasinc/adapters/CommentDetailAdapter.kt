package com.synapse.social.studioasinc.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.ItemCommentDetailBinding
import com.synapse.social.studioasinc.model.CommentWithUser
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.util.TimeUtils

class CommentDetailAdapter(
    private val onReplyClick: (CommentWithUser) -> Unit,
    private val onLikeClick: (CommentWithUser) -> Unit,
    private val onUserClick: (String) -> Unit,
    private val onOptionsClick: (CommentWithUser) -> Unit,
    private val onReactionPickerClick: (CommentWithUser) -> Unit,
    private val onLoadReplies: (String, (List<CommentWithUser>) -> Unit) -> Unit
) : ListAdapter<CommentWithUser, CommentDetailAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommentDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCommentDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var repliesAdapter: NestedRepliesAdapter? = null
        private var repliesExpanded = false

        fun bind(comment: CommentWithUser) {
            // Reset state for ViewHolder reuse
            repliesExpanded = false
            repliesAdapter = null
            binding.rvReplies.isVisible = false
            
            // Avatar
            Glide.with(binding.root.context)
                .load(comment.user?.avatar)
                .placeholder(R.drawable.avatar)
                .into(binding.ivAvatar)

            // User info
            binding.tvUsername.text = comment.user?.displayName ?: comment.user?.username ?: "Unknown"
            binding.ivVerified.isVisible = comment.user?.isVerified ?: false
            binding.chipAuthor.isVisible = false // Set based on post author comparison

            // Content
            if (comment.isDeleted) {
                binding.tvContent.text = binding.root.context.getString(R.string.deleted_comment)
                binding.tvContent.alpha = 0.5f
            } else {
                binding.tvContent.text = comment.content
                binding.tvContent.alpha = 1f
            }

            // Media
            binding.ivMedia.isVisible = !comment.mediaUrl.isNullOrEmpty()
            comment.mediaUrl?.let {
                Glide.with(binding.root.context).load(it).into(binding.ivMedia)
            }

            // Time and edited
            binding.tvTime.text = TimeUtils.formatTimestamp(comment.createdAt?.toLongOrNull() ?: System.currentTimeMillis())
            binding.tvEdited.isVisible = comment.isEdited

            // Reactions
            val totalReactions = comment.reactionSummary.values.sum()
            binding.reactionBadge.isVisible = totalReactions > 0
            if (totalReactions > 0) {
                val topEmoji = comment.reactionSummary.entries
                    .maxByOrNull { it.value }?.key?.emoji ?: "👍"
                binding.tvReactionEmoji.text = topEmoji
                binding.tvReactionCount.text = totalReactions.toString()
            }

            // Like button state
            if (comment.userReaction != null) {
                binding.tvLikeAction.setTextColor(
                    binding.root.context.getColor(R.color.colorPrimary)
                )
            } else {
                binding.tvLikeAction.setTextColor(
                    binding.root.context.getColor(R.color.colorOnSurface)
                )
            }

            // Replies
            binding.viewRepliesContainer.isVisible = comment.repliesCount > 0
            binding.tvViewReplies.text = binding.root.context.getString(
                R.string.view_replies, comment.repliesCount
            )

            // Click listeners
            binding.ivAvatar.setOnClickListener { onUserClick(comment.userId) }
            binding.tvUsername.setOnClickListener { onUserClick(comment.userId) }
            binding.tvReplyAction.setOnClickListener { onReplyClick(comment) }
            binding.tvLikeAction.setOnClickListener { onLikeClick(comment) }
            binding.tvLikeAction.setOnLongClickListener { 
                onReactionPickerClick(comment)
                true 
            }
            binding.cardComment.setOnLongClickListener {
                onOptionsClick(comment)
                true
            }

            binding.viewRepliesContainer.setOnClickListener {
                toggleReplies(comment)
            }
        }

        private fun toggleReplies(comment: CommentWithUser) {
            repliesExpanded = !repliesExpanded

            if (repliesExpanded) {
                binding.tvViewReplies.text = binding.root.context.getString(R.string.hide_replies)
                
                // Initialize replies adapter if not already done
                if (repliesAdapter == null) {
                    repliesAdapter = NestedRepliesAdapter(onUserClick, onLikeClick, onOptionsClick, onReactionPickerClick)
                    binding.rvReplies.layoutManager = LinearLayoutManager(binding.root.context)
                    binding.rvReplies.adapter = repliesAdapter
                }
                
                // Show the RecyclerView first
                binding.rvReplies.isVisible = true
                
                // Load replies from the repository
                onLoadReplies(comment.id) { replies ->
                    // Ensure UI update happens on main thread
                    if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                        // Already on main thread
                        android.util.Log.d("CommentDetailAdapter", "Loaded ${replies.size} replies for comment ${comment.id}")
                        repliesAdapter?.submitList(replies) {
                            // Force RecyclerView to refresh after list is submitted
                            binding.rvReplies.invalidate()
                            binding.rvReplies.requestLayout()
                        }
                        // Only hide if we actually have no replies
                        if (replies.isEmpty()) {
                            binding.rvReplies.isVisible = false
                            binding.tvViewReplies.text = binding.root.context.getString(R.string.no_replies_yet)
                        }
                    } else {
                        // Switch to main thread
                        binding.root.post {
                            android.util.Log.d("CommentDetailAdapter", "Loaded ${replies.size} replies for comment ${comment.id}")
                            repliesAdapter?.submitList(replies) {
                                // Force RecyclerView to refresh after list is submitted
                                binding.rvReplies.invalidate()
                                binding.rvReplies.requestLayout()
                            }
                            // Only hide if we actually have no replies
                            if (replies.isEmpty()) {
                                binding.rvReplies.isVisible = false
                                binding.tvViewReplies.text = binding.root.context.getString(R.string.no_replies_yet)
                            }
                        }
                    }
                }
            } else {
                binding.rvReplies.isVisible = false
                binding.tvViewReplies.text = binding.root.context.getString(
                    R.string.view_replies, comment.repliesCount
                )
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CommentWithUser>() {
        override fun areItemsTheSame(old: CommentWithUser, new: CommentWithUser) = old.id == new.id
        override fun areContentsTheSame(old: CommentWithUser, new: CommentWithUser) = old == new
    }
}
