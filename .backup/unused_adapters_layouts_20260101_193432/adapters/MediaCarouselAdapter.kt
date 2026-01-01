package com.synapse.social.studioasinc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType

/**
 * Adapter for displaying media carousel (images and videos) in posts
 */
class MediaCarouselAdapter(
    private val context: Context,
    private val mediaItems: List<MediaItem>,
    private val onMediaClicked: ((MediaItem, Int) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_IMAGE = 0
        private const val VIEW_TYPE_VIDEO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (mediaItems[position].type) {
            MediaType.IMAGE -> VIEW_TYPE_IMAGE
            MediaType.VIDEO -> VIEW_TYPE_VIDEO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_media_image, parent, false)
                ImageViewHolder(view)
            }
            VIEW_TYPE_VIDEO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_media_video, parent, false)
                VideoViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mediaItem = mediaItems[position]
        when (holder) {
            is ImageViewHolder -> holder.bind(mediaItem, position)
            is VideoViewHolder -> holder.bind(mediaItem, position)
        }
    }

    override fun getItemCount(): Int = mediaItems.size

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mediaImage: ImageView = itemView.findViewById(R.id.mediaImageView)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.imageProgressBar)

        fun bind(mediaItem: MediaItem, position: Int) {
            progressBar.visibility = View.VISIBLE

            Glide.with(context)
                .load(mediaItem.url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.default_image)
                .error(R.drawable.default_image)
                .into(mediaImage)
                .also {
                    progressBar.visibility = View.GONE
                }

            mediaImage.setOnClickListener {
                onMediaClicked?.invoke(mediaItem, position)
            }

            // Accessibility
            mediaImage.contentDescription = "Image ${position + 1} of ${mediaItems.size}"
        }
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerView: PlayerView = itemView.findViewById(R.id.playerView)
        private val videoThumbnail: ImageView? = itemView.findViewById(R.id.videoThumbnail)
        private val playButton: ImageView? = itemView.findViewById(R.id.playPauseButton)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.videoProgressBar)

        fun bind(mediaItem: MediaItem, position: Int) {
            // Load video thumbnail if available
            if (!mediaItem.thumbnailUrl.isNullOrEmpty()) {
                videoThumbnail?.let { thumbnail ->
                    progressBar.visibility = View.VISIBLE

                    Glide.with(context)
                        .load(mediaItem.thumbnailUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.default_image)
                        .error(R.drawable.default_image)
                        .into(thumbnail)
                        .also {
                            progressBar.visibility = View.GONE
                        }
                }
            }

            // Show play button overlay
            playButton?.visibility = View.VISIBLE

            itemView.setOnClickListener {
                onMediaClicked?.invoke(mediaItem, position)
            }

            // Accessibility
            itemView.contentDescription = "Video ${position + 1} of ${mediaItems.size}"
        }

        fun playVideo(url: String) {
            playerView.player?.play()
            playButton?.visibility = View.GONE
        }

        fun pauseVideo() {
            playerView.player?.pause()
            playButton?.visibility = View.VISIBLE
        }
    }

    /**
     * Pause all videos (call when swiping between pages)
     */
    fun pauseAllVideos(recyclerView: RecyclerView) {
        for (i in 0 until mediaItems.size) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
            if (viewHolder is VideoViewHolder) {
                viewHolder.pauseVideo()
            }
        }
    }
}
