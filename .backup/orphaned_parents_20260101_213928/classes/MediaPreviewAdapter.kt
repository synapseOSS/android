package com.synapse.social.studioasinc.chat

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.ItemSelectedMediaBinding

/**
 * Adapter for displaying selected media in preview grid
 * Shows thumbnails with remove buttons and an "add more" option
 */
class MediaPreviewAdapter(
    private val context: Context,
    private val mediaUris: List<Uri>,
    private val onRemoveClick: (Int) -> Unit,
    private val onAddMoreClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MEDIA = 0
        private const val VIEW_TYPE_ADD_MORE = 1
        private const val MAX_MEDIA_COUNT = 10
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < mediaUris.size) {
            VIEW_TYPE_MEDIA
        } else {
            VIEW_TYPE_ADD_MORE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MEDIA -> {
                val binding = ItemSelectedMediaBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                MediaViewHolder(binding)
            }
            VIEW_TYPE_ADD_MORE -> {
                val binding = ItemSelectedMediaBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AddMoreViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MediaViewHolder -> holder.bind(mediaUris[position], position)
            is AddMoreViewHolder -> holder.bind()
        }
    }

    override fun getItemCount(): Int {
        // Show media items + add more button if not at max
        return if (mediaUris.size < MAX_MEDIA_COUNT) {
            mediaUris.size + 1
        } else {
            mediaUris.size
        }
    }

    /**
     * ViewHolder for media items
     */
    inner class MediaViewHolder(
        private val binding: ItemSelectedMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri, position: Int) {
            // Load thumbnail using Glide
            Glide.with(context)
                .load(uri)
                .centerCrop()
                .placeholder(R.drawable.rounded_corner_placeholder)
                .into(binding.mediaImage)

            // Check if it's a video
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType?.startsWith("video/") == true) {
                binding.videoIndicator.visibility = View.VISIBLE
            } else {
                binding.videoIndicator.visibility = View.GONE
            }

            // Setup remove button
            binding.removeButton.visibility = View.VISIBLE
            binding.removeButton.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }

    /**
     * ViewHolder for "add more" button
     */
    inner class AddMoreViewHolder(
        private val binding: ItemSelectedMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            // Hide video indicator and remove button
            binding.videoIndicator.visibility = View.GONE
            binding.removeButton.visibility = View.GONE

            // Set add icon as image
            binding.mediaImage.setImageResource(R.drawable.ic_add_48px)
            binding.mediaImage.scaleType = android.widget.ImageView.ScaleType.CENTER

            // Set click listener
            binding.root.setOnClickListener {
                onAddMoreClick()
            }

            // Optional: Set a different background to distinguish from media items
            binding.root.alpha = 0.7f
        }
    }
}
