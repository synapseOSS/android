package com.synapse.social.studioasinc.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType

/**
 * Adapter for displaying selected media items in create post
 */
class SelectedMediaAdapter(
    private val mediaItems: MutableList<MediaItem>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedMediaAdapter.MediaViewHolder>() {

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mediaImageView: ImageView = itemView.findViewById(R.id.mediaImage)
        val removeButton: ImageView = itemView.findViewById(R.id.removeButton)
        val playIcon: ImageView? = itemView.findViewById(R.id.videoIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = mediaItems[position]
        
        // Load media thumbnail
        Glide.with(holder.itemView.context)
            .load(mediaItem.url)
            .centerCrop()
            .into(holder.mediaImageView)
        
        // Show play icon for videos
        holder.playIcon?.visibility = if (mediaItem.type == MediaType.VIDEO) View.VISIBLE else View.GONE
        
        // Handle remove button click
        holder.removeButton.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount(): Int = mediaItems.size
    
    fun removeItem(position: Int) {
        if (position in 0 until mediaItems.size) {
            mediaItems.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, mediaItems.size)
        }
    }
}
