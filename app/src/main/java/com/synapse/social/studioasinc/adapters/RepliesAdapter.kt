package com.synapse.social.studioasinc.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.Reply
import com.synapse.social.studioasinc.model.User
import de.hdodenhof.circleimageview.CircleImageView

class RepliesAdapter(
    private val onReplyLiked: (Reply) -> Unit,
    private val onReplyLongClicked: (Reply) -> Unit,
    private val onReplyReactionPicker: (Reply) -> Unit
) : ListAdapter<Reply, RepliesAdapter.ReplyViewHolder>(ReplyDiffCallback()) {

    private var userMap: Map<String, User> = emptyMap()

    fun setUserData(userMap: Map<String, User>) {
        this.userMap = userMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.reply_comments_synapse, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        val reply = getItem(position)
        val user = userMap[reply.uid]
        holder.bind(reply, user, onReplyLiked, onReplyLongClicked, onReplyReactionPicker)
    }

    class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.profileImage)
        private val username: TextView = itemView.findViewById(R.id.username)
        private val replyText: TextView = itemView.findViewById(R.id.comment_text)
        private val likeCount: TextView = itemView.findViewById(R.id.like_count)
        private val likeButton: ImageView = itemView.findViewById(R.id.like_unlike_ic)
        private val badge: ImageView = itemView.findViewById(R.id.badge)
        private val timestamp: TextView = itemView.findViewById(R.id.push)

        fun bind(
            reply: Reply,
            user: User?,
            onReplyLiked: (Reply) -> Unit,
            onReplyLongClicked: (Reply) -> Unit,
            onReplyReactionPicker: (Reply) -> Unit
        ) {
            replyText.text = reply.comment
            likeCount.text = reply.like.toString()
            timestamp.text = com.synapse.social.studioasinc.util.TimeUtils.getTimeAgo(reply.push_time)

            user?.let {
                username.text = it.nickname ?: "@${it.username}"

                if (it.avatar != null && it.avatar != "null") {
                    Glide.with(itemView.context).load(Uri.parse(it.avatar)).into(profileImage)
                } else {
                    profileImage.setImageResource(R.drawable.avatar)
                }

                when (it.accountType) {
                    "admin" -> badge.setImageResource(R.drawable.admin_badge)
                    "moderator" -> badge.setImageResource(R.drawable.moderator_badge)
                    "support" -> badge.setImageResource(R.drawable.support_badge)
                    else -> if (it.verify == true) badge.setImageResource(R.drawable.verified_badge)
                }
            }

            likeButton.setOnClickListener { onReplyLiked(reply) }
            likeButton.setOnLongClickListener { 
                onReplyReactionPicker(reply)
                true 
            }
            itemView.setOnLongClickListener {
                onReplyLongClicked(reply)
                true
            }
        }
    }

    class ReplyDiffCallback : DiffUtil.ItemCallback<Reply>() {
        override fun areItemsTheSame(oldItem: Reply, newItem: Reply): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: Reply, newItem: Reply): Boolean {
            return oldItem == newItem
        }
    }
}
