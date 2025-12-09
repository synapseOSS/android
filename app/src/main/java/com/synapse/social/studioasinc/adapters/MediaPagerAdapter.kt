package com.synapse.social.studioasinc.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType

class MediaPagerAdapter(
    private val items: List<MediaItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<MediaPagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_pager, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.ivMedia)
        private val playButton: View = itemView.findViewById(R.id.ivPlayButton)

        fun bind(item: MediaItem, position: Int) {
            Glide.with(itemView.context)
                .load(item.url)
                .centerCrop()
                .into(imageView)

            playButton.isVisible = item.type == MediaType.VIDEO

            itemView.setOnClickListener { onItemClick(position) }
        }
    }
}
