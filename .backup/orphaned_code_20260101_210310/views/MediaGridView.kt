package com.synapse.social.studioasinc.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType

/**
 * Custom FrameLayout that displays media items in Facebook-style grid layouts.
 * Supports 1-5+ media items with different layout configurations.
 */
class MediaGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * Interface for handling media item clicks
     */
    interface OnMediaClickListener {
        fun onMediaClick(mediaItems: List<MediaItem>, position: Int)
    }

    var onMediaClickListener: OnMediaClickListener? = null
    private var mediaItems: List<MediaItem> = emptyList()
    
    // Dimensions
    private val gridSpacing: Int = resources.getDimensionPixelSize(R.dimen.spacing_tiny) // 2dp
    private val cornerRadius: Int = resources.getDimensionPixelSize(R.dimen.spacing_normal) // 8dp
    private val maxSingleImageHeight: Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 400f, resources.displayMetrics
    ).toInt()

    /**
     * Sets the media items to display in the grid
     */
    fun setMediaItems(items: List<MediaItem>) {
        this.mediaItems = items
        removeAllViews()
        
        when (items.size) {
            0 -> {
                // No media, hide view
                visibility = View.GONE
            }
            1 -> createSingleItemLayout(items)
            2 -> createTwoItemLayout(items)
            3 -> createThreeItemLayout(items)
            4 -> createFourItemLayout(items)
            else -> createFivePlusItemLayout(items)
        }
    }

    /**
     * Creates layout for a single media item with original aspect ratio
     */
    private fun createSingleItemLayout(items: List<MediaItem>) {
        visibility = View.VISIBLE
        val mediaItem = items[0]
        
        val mediaView = createMediaImageView(mediaItem, 0)
        mediaView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            height = maxSingleImageHeight
        }
        
        addView(mediaView)
    }

    /**
     * Creates 1x2 horizontal layout for 2 media items
     */
    private fun createTwoItemLayout(items: List<MediaItem>) {
        visibility = View.VISIBLE
        
        // Calculate dimensions
        val totalWidth = resources.displayMetrics.widthPixels - 
            (resources.getDimensionPixelSize(R.dimen.spacing_medium) * 2)
        val itemWidth = (totalWidth - gridSpacing) / 2
        val itemHeight = itemWidth // Square aspect ratio
        
        val container = FrameLayout(context)
        container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, itemHeight)
        
        // Left image
        val leftMedia = createMediaImageView(items[0], 0)
        leftMedia.layoutParams = LayoutParams(itemWidth, itemHeight).apply {
            gravity = Gravity.START
        }
        container.addView(leftMedia)
        
        // Right image
        val rightMedia = createMediaImageView(items[1], 1)
        rightMedia.layoutParams = LayoutParams(itemWidth, itemHeight).apply {
            gravity = Gravity.END
        }
        container.addView(rightMedia)
        
        addView(container)
    }

    /**
     * Creates asymmetric layout for 3 media items:
     * 1 large item on left, 2 stacked items on right
     */
    private fun createThreeItemLayout(items: List<MediaItem>) {
        visibility = View.VISIBLE
        
        // Calculate dimensions
        val totalWidth = resources.displayMetrics.widthPixels - 
            (resources.getDimensionPixelSize(R.dimen.spacing_medium) * 2)
        val leftWidth = (totalWidth * 2 / 3) - (gridSpacing / 2)
        val rightWidth = (totalWidth / 3) - (gridSpacing / 2)
        val totalHeight = leftWidth // Make it square
        val rightItemHeight = (totalHeight - gridSpacing) / 2
        
        val container = FrameLayout(context)
        container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, totalHeight)
        
        // Left large image
        val leftMedia = createMediaImageView(items[0], 0)
        leftMedia.layoutParams = LayoutParams(leftWidth, totalHeight).apply {
            gravity = Gravity.START or Gravity.TOP
        }
        container.addView(leftMedia)
        
        // Top right image
        val topRightMedia = createMediaImageView(items[1], 1)
        topRightMedia.layoutParams = LayoutParams(rightWidth, rightItemHeight).apply {
            gravity = Gravity.END or Gravity.TOP
        }
        container.addView(topRightMedia)
        
        // Bottom right image
        val bottomRightMedia = createMediaImageView(items[2], 2)
        bottomRightMedia.layoutParams = LayoutParams(rightWidth, rightItemHeight).apply {
            gravity = Gravity.END or Gravity.BOTTOM
        }
        container.addView(bottomRightMedia)
        
        addView(container)
    }

    /**
     * Creates 2x2 grid layout for 4 media items
     */
    private fun createFourItemLayout(items: List<MediaItem>) {
        visibility = View.VISIBLE
        
        // Calculate dimensions
        val totalWidth = resources.displayMetrics.widthPixels - 
            (resources.getDimensionPixelSize(R.dimen.spacing_medium) * 2)
        val itemWidth = (totalWidth - gridSpacing) / 2
        val itemHeight = itemWidth // Square aspect ratio
        val totalHeight = itemHeight * 2 + gridSpacing
        
        val container = FrameLayout(context)
        container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, totalHeight)
        
        // Top left
        val topLeftMedia = createMediaImageView(items[0], 0)
        topLeftMedia.layoutParams = LayoutParams(itemWidth, itemHeight).apply {
            gravity = Gravity.START or Gravity.TOP
        }
        container.addView(topLeftMedia)
        
        // Top right
        val topRightMedia = createMediaImageView(items[1], 1)
        topRightMedia.layoutParams = LayoutParams(itemWidth, itemHeight).apply {
            gravity = Gravity.END or Gravity.TOP
        }
        container.addView(topRightMedia)
        
        // Bottom left
        val bottomLeftMedia = createMediaImageView(items[2], 2)
        bottomLeftMedia.layoutParams = LayoutParams(itemWidth, itemHeight).apply {
            gravity = Gravity.START or Gravity.BOTTOM
        }
        container.addView(bottomLeftMedia)
        
        // Bottom right
        val bottomRightMedia = createMediaImageView(items[3], 3)
        bottomRightMedia.layoutParams = LayoutParams(itemWidth, itemHeight).apply {
            gravity = Gravity.END or Gravity.BOTTOM
        }
        container.addView(bottomRightMedia)
        
        addView(container)
    }

    /**
     * Creates 2x2 grid layout for 5+ media items with "+N" overlay on 4th item
     */
    private fun createFivePlusItemLayout(items: List<MediaItem>) {
        visibility = View.VISIBLE
        
        // Calculate dimensions
        val totalWidth = resources.displayMetrics.widthPixels - 
            (resources.getDimensionPixelSize(R.dimen.spacing_medium) * 2)
        val itemWidth = (totalWidth - gridSpacing) / 2
        val itemHeight = itemWidth // Square aspect ratio
        val totalHeight = itemHeight * 2 + gridSpacing
        
        val container = FrameLayout(context)
        container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, totalHeight)
        
        // Top left
        val topLeftMedia = createMediaImageView(items[0], 0)
        topLeftMedia.layoutParams = LayoutParams(itemWidth, itemHeight).apply {
            gravity = Gravity.START or Gravity.TOP
        }
        container.addView(topLeftMedia)
        
        // Top right
        val topRightMedia = createMediaImageView(items[1], 1)
        topRightMedia.layoutParams = LayoutParams(itemWidth, itemHeight).apply {
            gravity = Gravity.END or Gravity.TOP
        }
        container.addView(topRightMedia)
        
        // Bottom left
        val bottomLeftMedia = createMediaImageView(items[2], 2)
        bottomLeftMedia.layoutParams = LayoutParams(itemWidth, itemHeight).apply {
            gravity = Gravity.START or Gravity.BOTTOM
        }
        container.addView(bottomLeftMedia)
        
        // Bottom right with overlay
        val bottomRightContainer = FrameLayout(context)
        bottomRightContainer.layoutParams = LayoutParams(itemWidth, itemHeight).apply {
            gravity = Gravity.END or Gravity.BOTTOM
        }
        
        val bottomRightMedia = createMediaImageView(items[3], 3)
        bottomRightMedia.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        bottomRightContainer.addView(bottomRightMedia)
        
        // Add overlay with "+N" text
        val remainingCount = items.size - 4
        val overlay = createOverlayView(remainingCount)
        bottomRightContainer.addView(overlay)
        
        container.addView(bottomRightContainer)
        
        addView(container)
    }

    /**
     * Creates an ImageView for a media item with proper styling and click handling
     */
    private fun createMediaImageView(mediaItem: MediaItem, position: Int): View {
        val imageView = ImageView(context)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        
        // Load image with Glide and apply rounded corners
        Glide.with(context)
            .load(mediaItem.url)
            .transform(CenterCrop(), RoundedCorners(cornerRadius))
            .placeholder(R.drawable.default_image)
            .error(R.drawable.default_image)
            .into(imageView)
        
        // Add play icon overlay for videos
        if (mediaItem.type == MediaType.VIDEO) {
            val container = FrameLayout(context)
            container.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            
            imageView.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            container.addView(imageView)
            
            val playIconOverlay = createPlayIconOverlay()
            container.addView(playIconOverlay)
            
            container.setOnClickListener {
                onMediaClickListener?.onMediaClick(mediaItems, position)
            }
            
            return container
        }
        
        // Set click listener
        imageView.setOnClickListener {
            onMediaClickListener?.onMediaClick(mediaItems, position)
        }
        
        return imageView
    }

    /**
     * Creates a play icon overlay for video thumbnails
     */
    private fun createPlayIconOverlay(): ImageView {
        val playIcon = ImageView(context)
        playIcon.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        playIcon.setImageResource(R.drawable.ic_play_circle_filled)
        playIcon.scaleType = ImageView.ScaleType.CENTER
        
        // Scale up the icon
        val iconSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics
        ).toInt()
        playIcon.layoutParams.width = iconSize
        playIcon.layoutParams.height = iconSize
        
        return playIcon
    }

    /**
     * Creates an overlay view with "+N" text for 5+ media items
     */
    private fun createOverlayView(remainingCount: Int): View {
        val overlay = FrameLayout(context)
        overlay.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        overlay.setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
        overlay.alpha = 0.6f
        
        val textView = TextView(context)
        textView.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        textView.text = "+$remainingCount"
        textView.setTextColor(ContextCompat.getColor(context, android.R.color.white))
        textView.textSize = 24f
        textView.gravity = Gravity.CENTER
        
        overlay.addView(textView)
        
        return overlay
    }
}
