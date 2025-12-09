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

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.gridlayout.widget.GridLayout
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class TextViewHolder(view: View) : BaseMessageViewHolder(view)

class MediaViewHolder(view: View) : BaseMessageViewHolder(view) {
    val mediaGridLayout: GridLayout? = view.findViewById(R.id.mediaGridLayout)
    val mediaContainerCard: CardView? = view.findViewById(R.id.mediaContainerCard)
    
    // These might be null if layout doesn't contain them, handle gracefully
    val mediaCarouselContainer: LinearLayout?
    val mediaCarouselRecyclerView: RecyclerView?
    val viewAllImagesButton: MaterialButton?
    
    init {
        var carouselContainer: LinearLayout? = null
        var carouselRecyclerView: RecyclerView? = null
        var allImagesButton: MaterialButton? = null
        
        try {
            carouselContainer = view.findViewById(R.id.mediaCarouselContainer)
            carouselRecyclerView = view.findViewById(R.id.mediaCarouselRecyclerView)
            allImagesButton = view.findViewById(R.id.viewAllImagesButton)
        } catch (e: Exception) {
            // Layout might not contain these newer elements
        }
        
        mediaCarouselContainer = carouselContainer
        mediaCarouselRecyclerView = carouselRecyclerView
        viewAllImagesButton = allImagesButton
    }
}

class VideoViewHolder(view: View) : BaseMessageViewHolder(view) {
    val videoThumbnail: ImageView? = view.findViewById(R.id.videoThumbnail)
    val playButton: ImageView? = view.findViewById(R.id.playButton)
    val videoContainerCard: MaterialCardView? = view.findViewById(R.id.videoContainerCard)
}

class TypingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val lottieTyping: LottieAnimationView? = view.findViewById(R.id.lottie_typing)
    val mProfileCard: CardView? = view.findViewById(R.id.mProfileCard)
    val mProfileImage: ImageView? = view.findViewById(R.id.mProfileImage)
    val messageBG: LinearLayout? = view.findViewById(R.id.messageBG)
}

class LinkPreviewViewHolder(view: View) : BaseMessageViewHolder(view) {
    val linkPreviewContainer: MaterialCardView? = view.findViewById(R.id.linkPreviewContainer)
    val linkPreviewImage: ImageView? = view.findViewById(R.id.linkPreviewImage)
    val linkPreviewTitle: TextView? = view.findViewById(R.id.linkPreviewTitle)
    val linkPreviewDescription: TextView? = view.findViewById(R.id.linkPreviewDescription)
    val linkPreviewDomain: TextView? = view.findViewById(R.id.linkPreviewDomain)
}

class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val loadingMoreProgressBar: ProgressBar? = view.findViewById(R.id.loadingMoreProgressBar)
}

class VoiceMessageViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
    val playPauseButton: ImageView? = itemView.findViewById(R.id.play_pause_button)
    val seekBar: SeekBar? = itemView.findViewById(R.id.voice_seekbar)
    val duration: TextView? = itemView.findViewById(R.id.voice_duration)
    val mediaPlayer: MediaPlayer = MediaPlayer()
    val handler: Handler = Handler(Looper.getMainLooper())
}
