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
import com.google.android.material.card.MaterialCardView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.SearchResult
import java.text.SimpleDateFormat
import java.util.*

class SearchResultsAdapter(
    private val onUserClick: (SearchResult.User) -> Unit,
    private val onPostClick: (SearchResult.Post) -> Unit,
    private val onMediaClick: (SearchResult.Media) -> Unit
) : ListAdapter<SearchResult, RecyclerView.ViewHolder>(SearchResultDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_POST = 1
        private const val VIEW_TYPE_MEDIA = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchResult.User -> VIEW_TYPE_USER
            is SearchResult.Post -> VIEW_TYPE_POST
            is SearchResult.Media -> VIEW_TYPE_MEDIA
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_search_user, parent, false)
                UserViewHolder(view)
            }
            VIEW_TYPE_POST -> {
                val view = inflater.inflate(R.layout.item_search_post, parent, false)
                PostViewHolder(view)
            }
            VIEW_TYPE_MEDIA -> {
                val view = inflater.inflate(R.layout.item_search_media, parent, false)
                MediaViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SearchResult.User -> (holder as UserViewHolder).bind(item)
            is SearchResult.Post -> (holder as PostViewHolder).bind(item)
            is SearchResult.Media -> (holder as MediaViewHolder).bind(item)
        }
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardUser: MaterialCardView = itemView.findViewById(R.id.cardUser)
        private val imageAvatar: ImageView = itemView.findViewById(R.id.imageAvatar)
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        private val textUsername: TextView = itemView.findViewById(R.id.textUsername)
        private val textHandle: TextView = itemView.findViewById(R.id.textHandle)
        private val imageGenderBadge: ImageView = itemView.findViewById(R.id.imageGenderBadge)
        private val imageAccountBadge: ImageView = itemView.findViewById(R.id.imageAccountBadge)

        fun bind(user: SearchResult.User) {
            // Set username and handle
            val displayName = if (!user.nickname.isNullOrEmpty() && user.nickname != "null") {
                user.nickname
            } else {
                user.username
            }
            textUsername.text = displayName
            textHandle.text = "@${user.username}"

            // Set avatar
            if (user.isBanned) {
                imageAvatar.setImageResource(R.drawable.banned_avatar)
            } else if (user.avatar.isNullOrEmpty() || user.avatar == "null") {
                imageAvatar.setImageResource(R.drawable.avatar)
            } else {
                Glide.with(itemView.context)
                    .load(Uri.parse(user.avatar))
                    .placeholder(R.drawable.avatar)
                    .error(R.drawable.avatar)
                    .into(imageAvatar)
            }

            // Set online status
            statusIndicator.visibility = if (user.status == "online") View.VISIBLE else View.GONE

            // Set gender badge
            when (user.gender) {
                "male" -> {
                    imageGenderBadge.setImageResource(R.drawable.male_badge)
                    imageGenderBadge.visibility = View.VISIBLE
                }
                "female" -> {
                    imageGenderBadge.setImageResource(R.drawable.female_badge)
                    imageGenderBadge.visibility = View.VISIBLE
                }
                else -> imageGenderBadge.visibility = View.GONE
            }

            // Set account badge
            when (user.accountType) {
                "admin" -> {
                    imageAccountBadge.setImageResource(R.drawable.admin_badge)
                    imageAccountBadge.visibility = View.VISIBLE
                }
                "moderator" -> {
                    imageAccountBadge.setImageResource(R.drawable.moderator_badge)
                    imageAccountBadge.visibility = View.VISIBLE
                }
                "support" -> {
                    imageAccountBadge.setImageResource(R.drawable.support_badge)
                    imageAccountBadge.visibility = View.VISIBLE
                }
                "user" -> {
                    when {
                        user.isPremium -> {
                            imageAccountBadge.setImageResource(R.drawable.premium_badge)
                            imageAccountBadge.visibility = View.VISIBLE
                        }
                        user.isVerified -> {
                            imageAccountBadge.setImageResource(R.drawable.verified_badge)
                            imageAccountBadge.visibility = View.VISIBLE
                        }
                        else -> imageAccountBadge.visibility = View.GONE
                    }
                }
                else -> imageAccountBadge.visibility = View.GONE
            }

            // Set click listener
            cardUser.setOnClickListener {
                onUserClick(user)
            }
        }
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardPost: MaterialCardView = itemView.findViewById(R.id.cardPost)
        private val imageAuthorAvatar: ImageView = itemView.findViewById(R.id.imageAuthorAvatar)
        private val textAuthorName: TextView = itemView.findViewById(R.id.textAuthorName)
        private val textPostTime: TextView = itemView.findViewById(R.id.textPostTime)
        private val textPostContent: TextView = itemView.findViewById(R.id.textPostContent)
        private val textLikesCount: TextView = itemView.findViewById(R.id.textLikesCount)
        private val textCommentsCount: TextView = itemView.findViewById(R.id.textCommentsCount)

        fun bind(post: SearchResult.Post) {
            textAuthorName.text = post.authorName
            textPostContent.text = post.content
            textLikesCount.text = post.likesCount.toString()
            textCommentsCount.text = post.commentsCount.toString()
            textPostTime.text = getTimeAgo(post.timestamp)

            // Set author avatar
            if (post.authorAvatar.isNullOrEmpty() || post.authorAvatar == "null") {
                imageAuthorAvatar.setImageResource(R.drawable.avatar)
            } else {
                Glide.with(itemView.context)
                    .load(Uri.parse(post.authorAvatar))
                    .placeholder(R.drawable.avatar)
                    .error(R.drawable.avatar)
                    .into(imageAuthorAvatar)
            }

            cardPost.setOnClickListener {
                onPostClick(post)
            }
        }
    }

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardMedia: MaterialCardView = itemView.findViewById(R.id.cardMedia)
        private val imageMediaThumbnail: ImageView = itemView.findViewById(R.id.imageMediaThumbnail)
        private val imagePlayIcon: ImageView = itemView.findViewById(R.id.imagePlayIcon)
        private val imageMediaAuthorAvatar: ImageView = itemView.findViewById(R.id.imageMediaAuthorAvatar)
        private val textMediaAuthorName: TextView = itemView.findViewById(R.id.textMediaAuthorName)
        private val imageMediaTypeIcon: ImageView = itemView.findViewById(R.id.imageMediaTypeIcon)

        fun bind(media: SearchResult.Media) {
            textMediaAuthorName.text = media.authorName

            // Set media thumbnail
            Glide.with(itemView.context)
                .load(Uri.parse(media.mediaUrl))
                .placeholder(R.drawable.default_image)
                .error(R.drawable.default_image)
                .into(imageMediaThumbnail)

            // Set author avatar
            if (media.authorAvatar.isNullOrEmpty() || media.authorAvatar == "null") {
                imageMediaAuthorAvatar.setImageResource(R.drawable.avatar)
            } else {
                Glide.with(itemView.context)
                    .load(Uri.parse(media.authorAvatar))
                    .placeholder(R.drawable.avatar)
                    .error(R.drawable.avatar)
                    .into(imageMediaAuthorAvatar)
            }

            // Set media type icon and play button
            when (media.mediaType) {
                SearchResult.MediaType.VIDEO -> {
                    imagePlayIcon.visibility = View.VISIBLE
                    imageMediaTypeIcon.setImageResource(R.drawable.icon_video_chat_round)
                }
                SearchResult.MediaType.PHOTO -> {
                    imagePlayIcon.visibility = View.GONE
                    imageMediaTypeIcon.setImageResource(R.drawable.icon_image_24px)
                }
            }

            cardMedia.setOnClickListener {
                onMediaClick(media)
            }
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    class SearchResultDiffCallback : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return when {
                oldItem is SearchResult.User && newItem is SearchResult.User ->
                    oldItem.uid == newItem.uid
                oldItem is SearchResult.Post && newItem is SearchResult.Post ->
                    oldItem.postId == newItem.postId
                oldItem is SearchResult.Media && newItem is SearchResult.Media ->
                    oldItem.postId == newItem.postId && oldItem.mediaUrl == newItem.mediaUrl
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem == newItem
        }
    }
}
