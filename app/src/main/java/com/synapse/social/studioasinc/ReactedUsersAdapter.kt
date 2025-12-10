package com.synapse.social.studioasinc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.model.UserReaction
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying users who reacted to a post
 */
class ReactedUsersAdapter(
    private val context: Context,
    private val onUserClicked: (String) -> Unit
) : ListAdapter<UserReaction, ReactedUsersAdapter.ViewHolder>(UserReactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reacted_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userAvatar: ImageView = itemView.findViewById(R.id.userAvatar)
        private val userName: TextView = itemView.findViewById(R.id.userName)
        private val verifiedBadge: ImageView = itemView.findViewById(R.id.verifiedBadge)
        private val reactedTime: TextView = itemView.findViewById(R.id.reactedTime)
        private val reactionIcon: ImageView = itemView.findViewById(R.id.reactionIcon)

        fun bind(userReaction: UserReaction) {
            // Load avatar
            if (!userReaction.profileImage.isNullOrEmpty()) {
                Glide.with(context)
                    .load(userReaction.profileImage)
                    .placeholder(R.drawable.avatar)
                    .error(R.drawable.avatar)
                    .circleCrop()
                    .into(userAvatar)
            } else {
                userAvatar.setImageResource(R.drawable.avatar)
            }

            // Set username
            userName.text = userReaction.getDisplayName()

            // Show/hide verified badge
            verifiedBadge.visibility = if (userReaction.isVerified) View.VISIBLE else View.GONE

            // Set reaction icon
            val reactionType = userReaction.getReactionTypeEnum()
            reactionIcon.setImageResource(reactionType.iconRes)
            reactionIcon.contentDescription = reactionType.displayName

            // Set reacted time
            reactedTime.text = formatTime(userReaction.reactedAt)

            // Set click listener
            itemView.setOnClickListener {
                onUserClicked(userReaction.userId)
            }

            // Set accessibility
            itemView.contentDescription = "${userReaction.username} reacted with ${reactionType.displayName}"
        }

        private fun formatTime(timestamp: String?): String {
            if (timestamp.isNullOrEmpty()) return "Just now"

            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = sdf.parse(timestamp)
                val now = System.currentTimeMillis()
                val diff = now - (date?.time ?: now)

                when {
                    diff < 60_000 -> "Just now"
                    diff < 3600_000 -> "${diff / 60_000}m ago"
                    diff < 86400_000 -> "${diff / 3600_000}h ago"
                    diff < 604800_000 -> "${diff / 86400_000}d ago"
                    else -> "${diff / 604800_000}w ago"
                }
            } catch (e: Exception) {
                "Just now"
            }
        }
    }

    class UserReactionDiffCallback : DiffUtil.ItemCallback<UserReaction>() {
        override fun areItemsTheSame(oldItem: UserReaction, newItem: UserReaction): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: UserReaction, newItem: UserReaction): Boolean {
            return oldItem == newItem
        }
    }
}
