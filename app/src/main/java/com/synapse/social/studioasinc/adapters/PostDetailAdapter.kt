package com.synapse.social.studioasinc.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.databinding.ItemPostDetailCaptionBinding
import com.synapse.social.studioasinc.databinding.ItemPostDetailImageBinding
import com.synapse.social.studioasinc.databinding.ItemPostDetailVideoBinding
import com.synapse.social.studioasinc.model.PostDetailItem

class PostDetailAdapter(
    private val onLikeClick: (String) -> Unit,
    private val onCommentClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<PostDetailItem>()

    companion object {
        private const val TYPE_CAPTION = 0
        private const val TYPE_IMAGE = 1
        private const val TYPE_VIDEO = 2
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is PostDetailItem.Caption -> TYPE_CAPTION
        is PostDetailItem.Image -> TYPE_IMAGE
        is PostDetailItem.Video -> TYPE_VIDEO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CAPTION -> CaptionViewHolder(ItemPostDetailCaptionBinding.inflate(inflater, parent, false))
            TYPE_IMAGE -> ImageViewHolder(ItemPostDetailImageBinding.inflate(inflater, parent, false))
            TYPE_VIDEO -> VideoViewHolder(ItemPostDetailVideoBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PostDetailItem.Caption -> (holder as CaptionViewHolder).bind(item)
            is PostDetailItem.Image -> (holder as ImageViewHolder).bind(item)
            is PostDetailItem.Video -> (holder as VideoViewHolder).bind(item)
        }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<PostDetailItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class CaptionViewHolder(private val binding: ItemPostDetailCaptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PostDetailItem.Caption) {
            binding.apply {
                tvCaption.text = item.text
                tvLikesCount.text = item.likesCount.toString()
                tvCommentsCount.text = item.commentsCount.toString()
                btnLike.isSelected = item.userHasLiked

                btnLike.setOnClickListener { onLikeClick("caption_${item.postId}") }
                btnComment.setOnClickListener { onCommentClick("caption_${item.postId}") }
            }
        }
    }

    inner class ImageViewHolder(private val binding: ItemPostDetailImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PostDetailItem.Image) {
            binding.apply {
                Glide.with(ivImage.context).load(item.mediaItem.url).into(ivImage)
                tvLikesCount.text = item.mediaItem.likesCount.toString()
                tvCommentsCount.text = item.mediaItem.commentsCount.toString()
                btnLike.isSelected = item.mediaItem.userHasLiked

                btnLike.setOnClickListener { onLikeClick(item.mediaItem.id) }
                btnComment.setOnClickListener { onCommentClick(item.mediaItem.id) }
            }
        }
    }

    inner class VideoViewHolder(private val binding: ItemPostDetailVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PostDetailItem.Video) {
            binding.apply {
                Glide.with(ivThumbnail.context).load(item.mediaItem.thumbnailUrl ?: item.mediaItem.url).into(ivThumbnail)
                tvLikesCount.text = item.mediaItem.likesCount.toString()
                tvCommentsCount.text = item.mediaItem.commentsCount.toString()
                btnLike.isSelected = item.mediaItem.userHasLiked

                btnLike.setOnClickListener { onLikeClick(item.mediaItem.id) }
                btnComment.setOnClickListener { onCommentClick(item.mediaItem.id) }
            }
        }
    }
}
