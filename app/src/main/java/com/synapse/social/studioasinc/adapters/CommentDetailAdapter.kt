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
    private val onReactionPickerClick: (CommentWithUser) -> Unit
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
            // Avatar
            Glide.with(binding.root.context)
                .load(comment.user?.profileImageUrl)
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
            binding.rvReplies.isVisible = repliesExpanded

            if (repliesExpanded) {
                binding.tvViewReplies.text = binding.root.context.getString(R.string.hide_replies)
                // Load replies - in real implementation, fetch from repository
                if (repliesAdapter == null) {
                    repliesAdapter = NestedRepliesAdapter(onUserClick, onLikeClick, onOptionsClick, onReactionPickerClick)
                    binding.rvReplies.layoutManager = LinearLayoutManager(binding.root.context)
                    binding.rvReplies.adapter = repliesAdapter
                }
            } else {
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

private class NestedRepliesAdapter(
    private val onUserClick: (String) -> Unit,
    private val onLikeClick: (CommentWithUser) -> Unit,
    private val onOptionsClick: (CommentWithUser) -> Unit,
    private val onReactionPickerClick: (CommentWithUser) -> Unit
) : ListAdapter<CommentWithUser, NestedRepliesAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<CommentWithUser>() {
        override fun areItemsTheSame(old: CommentWithUser, new: CommentWithUser) = old.id == new.id
        override fun areContentsTheSame(old: CommentWithUser, new: CommentWithUser) = old == new
    }
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommentDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCommentDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reply: CommentWithUser) {
            Glide.with(binding.root.context).load(reply.user?.profileImageUrl).placeholder(R.drawable.avatar).into(binding.ivAvatar)
            binding.tvUsername.text = reply.user?.displayName ?: reply.user?.username ?: "Unknown"
            binding.tvContent.text = reply.content
            binding.tvTime.text = TimeUtils.formatTimestamp(reply.createdAt?.toLongOrNull() ?: System.currentTimeMillis())
            binding.reactionBadge.isVisible = reply.reactionSummary.values.sum() > 0
            binding.viewRepliesContainer.isVisible = false
            binding.ivAvatar.setOnClickListener { reply.userId?.let { onUserClick(it) } }
            binding.tvLikeAction.setOnClickListener { onLikeClick(reply) }
            binding.tvLikeAction.setOnLongClickListener { 
                onReactionPickerClick(reply)
                true 
            }
        }
    }
}
