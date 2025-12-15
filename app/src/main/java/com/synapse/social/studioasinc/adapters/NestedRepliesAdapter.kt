package com.synapse.social.studioasinc.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.ItemNestedReplyBinding
import com.synapse.social.studioasinc.model.CommentWithUser
import com.synapse.social.studioasinc.util.TimeUtils

class NestedRepliesAdapter(
    private val onUserClick: (String) -> Unit,
    private val onLikeClick: (CommentWithUser) -> Unit,
    private val onOptionsClick: (CommentWithUser) -> Unit,
    private val onReactionPickerClick: (CommentWithUser) -> Unit
) : ListAdapter<CommentWithUser, NestedRepliesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNestedReplyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemNestedReplyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(reply: CommentWithUser) {
            // Avatar
            Glide.with(binding.root.context)
                .load(reply.user?.profileImageUrl)
                .placeholder(R.drawable.avatar)
                .into(binding.ivAvatar)

            // User info
            binding.tvUsername.text = reply.user?.displayName ?: reply.user?.username ?: "Unknown"
            binding.ivVerified.isVisible = reply.user?.isVerified ?: false

            // Content
            if (reply.isDeleted) {
                binding.tvContent.text = binding.root.context.getString(R.string.deleted_comment)
                binding.tvContent.alpha = 0.5f
            } else {
                binding.tvContent.text = reply.content
                binding.tvContent.alpha = 1f
            }

            // Media
            binding.ivMedia.isVisible = !reply.mediaUrl.isNullOrEmpty()
            reply.mediaUrl?.let {
                Glide.with(binding.root.context).load(it).into(binding.ivMedia)
            }

            // Time and edited
            binding.tvTime.text = TimeUtils.formatTimestamp(reply.createdAt?.toLongOrNull() ?: System.currentTimeMillis())
            binding.tvEdited.isVisible = reply.isEdited

            // Reactions
            val totalReactions = reply.reactionSummary.values.sum()
            binding.reactionBadge.isVisible = totalReactions > 0
            if (totalReactions > 0) {
                val topEmoji = reply.reactionSummary.entries
                    .maxByOrNull { it.value }?.key?.emoji ?: "👍"
                binding.tvReactionEmoji.text = topEmoji
                binding.tvReactionCount.text = totalReactions.toString()
            }

            // Like button state
            if (reply.userReaction != null) {
                binding.tvLikeAction.setTextColor(
                    binding.root.context.getColor(R.color.colorPrimary)
                )
            } else {
                binding.tvLikeAction.setTextColor(
                    binding.root.context.getColor(R.color.colorOnSurface)
                )
            }

            // Click listeners
            binding.ivAvatar.setOnClickListener { onUserClick(reply.userId) }
            binding.tvUsername.setOnClickListener { onUserClick(reply.userId) }
            binding.tvLikeAction.setOnClickListener { onLikeClick(reply) }
            binding.tvLikeAction.setOnLongClickListener { 
                onReactionPickerClick(reply)
                true 
            }
            binding.cardReply.setOnLongClickListener {
                onOptionsClick(reply)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CommentWithUser>() {
        override fun areItemsTheSame(old: CommentWithUser, new: CommentWithUser) = old.id == new.id
        override fun areContentsTheSame(old: CommentWithUser, new: CommentWithUser) = old == new
    }
}
