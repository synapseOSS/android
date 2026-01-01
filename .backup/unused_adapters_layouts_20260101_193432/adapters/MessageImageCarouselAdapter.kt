/**
 * CONFIDENTIAL AND PROPRIETARY
 * 
 * This source code is the sole property of StudioAs Inc. Synapse. (Ashik).
 * Any reproduction, modification, distribution, or exploitation in any form
 * without explicit written permission from the owner is strictly prohibited.
 * 
 * Copyright (c) 2025 StudioAs Inc. Synapse. (Ashik)
 * All rights reserved.
 */

package com.synapse.social.studioasinc

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.synapse.social.studioasinc.config.CloudinaryConfig
import com.synapse.social.studioasinc.model.Attachment

class MessageImageCarouselAdapter(
    private val context: Context,
    private val attachments: List<Attachment>,
    private val onImageClickListener: OnImageClickListener?
) : RecyclerView.Adapter<MessageImageCarouselAdapter.ImageCarouselViewHolder>() {
    
    fun interface OnImageClickListener {
        fun onImageClick(position: Int, attachments: List<Attachment>)
    }
    
    private val imageSize: Int = context.resources.getDimension(R.dimen.chat_carousel_image_size).toInt()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageCarouselViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_message_carousel_image, parent, false)
        return ImageCarouselViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ImageCarouselViewHolder, position: Int) {
        if (position < 0 || position >= attachments.size) {
            holder.imageView.setImageResource(R.drawable.ph_imgbluredsqure)
            holder.itemView.setOnClickListener(null)
            return
        }
        
        val attachment = attachments[position]
        val publicId = attachment.publicId
        
        // Set consistent size for all carousel images
        holder.cardView.layoutParams = holder.cardView.layoutParams.apply {
            width = imageSize
            height = imageSize
        }
        
        if (!publicId.isNullOrEmpty()) {
            // Use optimized image URL based on actual view size for better performance
            val densityDpi = context.resources.displayMetrics.densityDpi
            val imageUrl = CloudinaryConfig.buildCarouselImageUrl(publicId, densityDpi)
            
            val cornerRadius = context.resources.getDimension(R.dimen.gallery_image_corner_radius).toInt()
            
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ph_imgbluredsqure)
                .error(R.drawable.ph_imgbluredsqure)
                .transform(RoundedCorners(cornerRadius))
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.ph_imgbluredsqure)
        }
        
        // Set click listener to open gallery
        holder.itemView.setOnClickListener {
            if (position in attachments.indices) {
                onImageClickListener?.onImageClick(position, attachments)
            }
        }
        
        // Add ripple effect with hover animation
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        holder.cardView.foreground = context.getDrawable(outValue.resourceId)
        
        // Add subtle scale animation on touch
        val animationDuration = context.resources.getInteger(R.integer.touch_feedback_duration).toLong()
        val scaleDown = 0.95f
        val scaleNormal = 1.0f
        
        holder.itemView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(scaleDown)
                        .scaleY(scaleDown)
                        .setDuration(animationDuration)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(scaleNormal)
                        .scaleY(scaleNormal)
                        .setDuration(animationDuration)
                        .start()
                }
            }
            false // Allow the click listener to handle the click
        }
    }
    
    override fun getItemCount(): Int = attachments.size
    
    class ImageCarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.carouselImageCard)
        val imageView: ImageView = itemView.findViewById(R.id.carouselImageView)
    }
    
    companion object {
        private const val TAG = "MessageImageCarouselAdapter"
    }
}
