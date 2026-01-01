package com.synapse.social.studioasinc.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.gridlayout.widget.GridLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.model.ChatAttachmentImpl
import com.synapse.social.studioasinc.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying chat messages
 */
class ChatAdapter(
    private val context: Context,
    private val onMessageLongClick: (Message) -> Unit = {},
    private val onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    private val onVideoClick: (String) -> Unit = {},
    private val onAudioClick: (String) -> Unit = {},
    private val onDocumentClick: (ChatAttachmentImpl) -> Unit = {},
    private val multiSelectManager: com.synapse.social.studioasinc.chat.MultiSelectManager? = null
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val authService = SupabaseAuthenticationService()
    private val currentUserId = authService.getCurrentUserId()
    
    // User-deleted message IDs cache
    var userDeletedMessageIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    
    // Multi-select mode state
    var isMultiSelectMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    companion object {
        private const val TAG = "ChatAdapter"
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
        private const val VIEW_TYPE_IMAGE_SENT = 3
        private const val VIEW_TYPE_IMAGE_RECEIVED = 4
        private const val VIEW_TYPE_VIDEO_SENT = 5
        private const val VIEW_TYPE_VIDEO_RECEIVED = 6
        private const val VIEW_TYPE_AUDIO_SENT = 7
        private const val VIEW_TYPE_AUDIO_RECEIVED = 8
        private const val VIEW_TYPE_DOCUMENT_SENT = 9
        private const val VIEW_TYPE_DOCUMENT_RECEIVED = 10
    }

    /**
     * Enum representing the position of a message within a group
     */
    enum class MessagePosition {
        SINGLE,    // Message not part of a group
        FIRST,     // First message in a group
        MIDDLE,    // Middle message in a group
        LAST       // Last message in a group
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val isSent = message.senderId == currentUserId
        
        // Check if message has attachments
        val attachments = message.attachments
        if (!attachments.isNullOrEmpty()) {
            val firstAttachment = attachments.first()
            return when (firstAttachment.type) {
                "image" -> if (isSent) VIEW_TYPE_IMAGE_SENT else VIEW_TYPE_IMAGE_RECEIVED
                "video" -> if (isSent) VIEW_TYPE_VIDEO_SENT else VIEW_TYPE_VIDEO_RECEIVED
                "audio" -> if (isSent) VIEW_TYPE_AUDIO_SENT else VIEW_TYPE_AUDIO_RECEIVED
                "document" -> if (isSent) VIEW_TYPE_DOCUMENT_SENT else VIEW_TYPE_DOCUMENT_RECEIVED
                else -> if (isSent) VIEW_TYPE_MESSAGE_SENT else VIEW_TYPE_MESSAGE_RECEIVED
            }
        }
        
        return if (isSent) VIEW_TYPE_MESSAGE_SENT else VIEW_TYPE_MESSAGE_RECEIVED
    }

    /**
     * Checks if a message type supports grouping
     * Excludes typing indicators, error messages, and loading indicators
     */
    private fun isGroupableMessageType(viewType: Int): Boolean {
        return viewType in listOf(
            VIEW_TYPE_MESSAGE_SENT,
            VIEW_TYPE_MESSAGE_RECEIVED,
            VIEW_TYPE_IMAGE_SENT,
            VIEW_TYPE_IMAGE_RECEIVED,
            VIEW_TYPE_VIDEO_SENT,
            VIEW_TYPE_VIDEO_RECEIVED,
            VIEW_TYPE_AUDIO_SENT,
            VIEW_TYPE_AUDIO_RECEIVED,
            VIEW_TYPE_DOCUMENT_SENT,
            VIEW_TYPE_DOCUMENT_RECEIVED
        )
    }

    /**
     * Checks if the current message should group with the previous message
     * Returns true if both messages are from the same sender and are groupable types
     */
    private fun shouldGroupWithPrevious(currentPosition: Int): Boolean {
        // Bounds checking
        if (currentPosition <= 0 || currentPosition >= itemCount) {
            return false
        }

        val currentMessage = getItem(currentPosition)
        val previousMessage = getItem(currentPosition - 1)

        // Check if current message is deleted or null
        if (currentMessage.isDeleted) {
            return false
        }

        // Check if previous message is deleted or null
        if (previousMessage.isDeleted) {
            return false
        }

        // Check if both messages are groupable types
        val currentViewType = getItemViewType(currentPosition)
        val previousViewType = getItemViewType(currentPosition - 1)
        
        if (!isGroupableMessageType(currentViewType) || !isGroupableMessageType(previousViewType)) {
            return false
        }

        // Check if both messages are from the same sender
        return currentMessage.senderId == previousMessage.senderId
    }

    /**
     * Checks if the current message should group with the next message
     * Returns true if both messages are from the same sender and are groupable types
     */
    private fun shouldGroupWithNext(currentPosition: Int): Boolean {
        // Bounds checking
        if (currentPosition < 0 || currentPosition >= itemCount - 1) {
            return false
        }

        val currentMessage = getItem(currentPosition)
        val nextMessage = getItem(currentPosition + 1)

        // Check if current message is deleted or null
        if (currentMessage.isDeleted) {
            return false
        }

        // Check if next message is deleted or null
        if (nextMessage.isDeleted) {
            return false
        }

        // Check if both messages are groupable types
        val currentViewType = getItemViewType(currentPosition)
        val nextViewType = getItemViewType(currentPosition + 1)
        
        if (!isGroupableMessageType(currentViewType) || !isGroupableMessageType(nextViewType)) {
            return false
        }

        // Check if both messages are from the same sender
        return currentMessage.senderId == nextMessage.senderId
    }

    /**
     * Calculates the position of a message within its group
     * Returns SINGLE, FIRST, MIDDLE, or LAST based on adjacent messages
     */
    private fun calculateMessagePosition(position: Int): MessagePosition {
        // Bounds checking
        if (position < 0 || position >= itemCount) {
            return MessagePosition.SINGLE
        }

        val canGroupWithPrevious = shouldGroupWithPrevious(position)
        val canGroupWithNext = shouldGroupWithNext(position)

        return when {
            !canGroupWithPrevious && !canGroupWithNext -> MessagePosition.SINGLE
            !canGroupWithPrevious && canGroupWithNext -> MessagePosition.FIRST
            canGroupWithPrevious && canGroupWithNext -> MessagePosition.MIDDLE
            canGroupWithPrevious && !canGroupWithNext -> MessagePosition.LAST
            else -> MessagePosition.SINGLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_MESSAGE_SENT -> {
                val view = inflater.inflate(R.layout.chat_bubble_text, parent, false)
                SentMessageViewHolder(view, true)
            }
            VIEW_TYPE_MESSAGE_RECEIVED -> {
                val view = inflater.inflate(R.layout.chat_bubble_text, parent, false)
                ReceivedMessageViewHolder(view, false)
            }
            VIEW_TYPE_IMAGE_SENT, VIEW_TYPE_IMAGE_RECEIVED -> {
                val view = inflater.inflate(R.layout.chat_bubble_image, parent, false)
                ImageAttachmentViewHolder(view, viewType == VIEW_TYPE_IMAGE_SENT)
            }
            VIEW_TYPE_VIDEO_SENT, VIEW_TYPE_VIDEO_RECEIVED -> {
                val view = inflater.inflate(R.layout.chat_bubble_video, parent, false)
                VideoAttachmentViewHolder(view, viewType == VIEW_TYPE_VIDEO_SENT)
            }
            VIEW_TYPE_AUDIO_SENT, VIEW_TYPE_AUDIO_RECEIVED -> {
                val view = inflater.inflate(R.layout.chat_bubble_audio, parent, false)
                AudioAttachmentViewHolder(view, viewType == VIEW_TYPE_AUDIO_SENT)
            }
            VIEW_TYPE_DOCUMENT_SENT, VIEW_TYPE_DOCUMENT_RECEIVED -> {
                val view = inflater.inflate(R.layout.chat_bubble_document, parent, false)
                DocumentAttachmentViewHolder(view, viewType == VIEW_TYPE_DOCUMENT_SENT)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        val messagePosition = calculateMessagePosition(position)
        
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message, messagePosition)
            is ReceivedMessageViewHolder -> holder.bind(message, messagePosition)
            is ImageAttachmentViewHolder -> holder.bind(message, messagePosition)
            is VideoAttachmentViewHolder -> holder.bind(message, messagePosition)
            is AudioAttachmentViewHolder -> holder.bind(message, messagePosition)
            is DocumentAttachmentViewHolder -> holder.bind(message, messagePosition)
        }
    }
    
    /**
     * Base ViewHolder class with helper methods for corner radius and dynamic width
     */
    abstract inner class BaseMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        // Selection UI components
        protected val selectionCheckbox: android.widget.CheckBox? = itemView.findViewById(R.id.selection_checkbox)
        protected val selectionOverlay: View? = itemView.findViewById(R.id.selection_overlay)
        
        /**
         * Calculates the maximum bubble width as 75% of screen width
         */
        protected fun getMaxBubbleWidth(): Int {
            val displayMetrics = itemView.context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            return (screenWidth * 0.75).toInt()
        }
        
        /**
         * Updates selection UI based on multi-select mode and selection state
         */
        protected fun updateSelectionUI(message: Message) {
            val isSelected = multiSelectManager?.isMessageSelected(message.id) ?: false
            
            if (isMultiSelectMode) {
                selectionCheckbox?.visibility = View.VISIBLE
                selectionCheckbox?.isChecked = isSelected
                selectionOverlay?.visibility = if (isSelected) View.VISIBLE else View.GONE
            } else {
                selectionCheckbox?.visibility = View.GONE
                selectionOverlay?.visibility = View.GONE
            }
        }
        
        /**
         * Sets up click handlers for multi-select mode
         */
        protected fun setupClickHandlers(message: Message) {
            itemView.setOnClickListener {
                if (isMultiSelectMode) {
                    multiSelectManager?.toggleMessageSelection(message.id)
                }
            }
            
            itemView.setOnLongClickListener {
                if (!isMultiSelectMode) {
                    multiSelectManager?.enterMultiSelectMode(message.id)
                    true
                } else {
                    onMessageLongClick(message)
                    true
                }
            }
        }
        
        /**
         * Creates a GradientDrawable with custom corner radii based on message position
         * @param position The position of the message in the group
         * @param isSent Whether this is a sent message (affects color)
         * @return GradientDrawable with appropriate corners and color
         */
        protected fun createBubbleDrawable(position: MessagePosition, isSent: Boolean): android.graphics.drawable.GradientDrawable {
            val drawable = android.graphics.drawable.GradientDrawable()
            
            // Set background color based on sent/received
            val color = if (isSent) {
                itemView.context.getColor(R.color.sent_message_background)
            } else {
                itemView.context.getColor(R.color.received_message_background)
            }
            drawable.setColor(color)
            
            // Set corner radii based on position
            val cornerRadius = itemView.context.resources.getDimension(R.dimen.message_bubble_corner_radius)
            val corners = when (position) {
                MessagePosition.SINGLE -> {
                    // All corners rounded
                    floatArrayOf(
                        cornerRadius, cornerRadius,  // top-left
                        cornerRadius, cornerRadius,  // top-right
                        cornerRadius, cornerRadius,  // bottom-right
                        cornerRadius, cornerRadius   // bottom-left
                    )
                }
                MessagePosition.FIRST -> {
                    // Top corners rounded, bottom square
                    floatArrayOf(
                        cornerRadius, cornerRadius,  // top-left
                        cornerRadius, cornerRadius,  // top-right
                        0f, 0f,                      // bottom-right
                        0f, 0f                       // bottom-left
                    )
                }
                MessagePosition.MIDDLE -> {
                    // All corners square
                    floatArrayOf(
                        0f, 0f,  // top-left
                        0f, 0f,  // top-right
                        0f, 0f,  // bottom-right
                        0f, 0f   // bottom-left
                    )
                }
                MessagePosition.LAST -> {
                    // Bottom corners rounded, top square
                    floatArrayOf(
                        0f, 0f,                      // top-left
                        0f, 0f,                      // top-right
                        cornerRadius, cornerRadius,  // bottom-right
                        cornerRadius, cornerRadius   // bottom-left
                    )
                }
            }
            drawable.cornerRadii = corners
            
            return drawable
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        
        val messagePosition = calculateMessagePosition(position)
        val payload = payloads[0]
        if (payload is UploadProgressPayload) {
            when (holder) {
                is ImageAttachmentViewHolder -> holder.bind(getItem(position), messagePosition, payload)
                is VideoAttachmentViewHolder -> holder.bind(getItem(position), messagePosition, payload)
                is AudioAttachmentViewHolder -> holder.bind(getItem(position), messagePosition, payload)
                is DocumentAttachmentViewHolder -> holder.bind(getItem(position), messagePosition, payload)
                else -> super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class SentMessageViewHolder(itemView: View, private val isSent: Boolean) : BaseMessageViewHolder(itemView) {
        private val body: View = itemView.findViewById(R.id.body)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val timeText: TextView = itemView.findViewById(R.id.date)
        private val messageBG: android.widget.LinearLayout = itemView.findViewById(R.id.messageBG)
        private val deletedMessagePlaceholder: View = itemView.findViewById(R.id.deletedMessagePlaceholder)
        private val messageContentContainer: View = itemView.findViewById(R.id.messageContentContainer)
        
        init {
            // Set alignment based on sent/received
            val params = body.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (isSent) {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            } else {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            }
            body.layoutParams = params
        }

        fun bind(message: Message, messagePosition: MessagePosition) {
            // Apply dynamic width
            val maxWidth = getMaxBubbleWidth()
            val layoutParams = messageBG.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            messageBG.layoutParams = layoutParams
            
            // Apply corner radii
            messageBG.background = createBubbleDrawable(messagePosition, isSent)
            
            // Check if message is deleted for current user
            val isDeletedForCurrentUser = currentUserId?.let { userId ->
                message.isDeletedForUser(userId, userDeletedMessageIds)
            } ?: false
            
            // Handle deleted messages
            if (isDeletedForCurrentUser) {
                // Show deleted message placeholder
                messageText.text = currentUserId?.let { userId ->
                    message.getDeletedMessageText(userId, userDeletedMessageIds)
                } ?: "This message was deleted"
                
                // Update selection UI even for deleted messages
                updateSelectionUI(message)
                setupClickHandlers(message)
                
                timeText.text = formatTime(message.createdAt)
                return
            }
            
            messageText.text = message.getDisplayContent()
            timeText.text = formatTime(message.createdAt)
            
            // Update selection UI
            updateSelectionUI(message)
            
            // Setup click handlers for multi-select
            setupClickHandlers(message)
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View, private val isSent: Boolean) : BaseMessageViewHolder(itemView) {
        private val body: View = itemView.findViewById(R.id.body)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val timeText: TextView = itemView.findViewById(R.id.date)
        private val senderUsername: TextView? = itemView.findViewById(R.id.senderUsername)
        private val messageBG: android.widget.LinearLayout = itemView.findViewById(R.id.messageBG)
        private val deletedMessagePlaceholder: View = itemView.findViewById(R.id.deletedMessagePlaceholder)
        private val messageContentContainer: View = itemView.findViewById(R.id.messageContentContainer)
        
        init {
            // Set alignment based on sent/received
            val params = body.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (isSent) {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            } else {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            }
            body.layoutParams = params
        }

        fun bind(message: Message, messagePosition: MessagePosition) {
            // Apply dynamic width
            val maxWidth = getMaxBubbleWidth()
            val layoutParams = messageBG.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            messageBG.layoutParams = layoutParams
            
            // Apply corner radii
            messageBG.background = createBubbleDrawable(messagePosition, isSent)
            
            // Check if message is deleted for current user
            val isDeletedForCurrentUser = currentUserId?.let { userId ->
                message.isDeletedForUser(userId, userDeletedMessageIds)
            } ?: false
            
            // Handle deleted messages
            if (isDeletedForCurrentUser) {
                // Show deleted message placeholder
                messageText.text = currentUserId?.let { userId ->
                    message.getDeletedMessageText(userId, userDeletedMessageIds)
                } ?: "This message was deleted"
                
                // Hide sender name for deleted messages
                senderUsername?.visibility = View.GONE
                
                // Update selection UI even for deleted messages
                updateSelectionUI(message)
                setupClickHandlers(message)
                
                timeText.text = formatTime(message.createdAt)
                return
            }
            
            // Show sender name for received messages
            if (!isSent && message.senderName != null) {
                senderUsername?.visibility = View.VISIBLE
                senderUsername?.text = message.senderName
            } else {
                senderUsername?.visibility = View.GONE
            }
            
            messageText.text = message.getDisplayContent()
            timeText.text = formatTime(message.createdAt)
            
            // Update selection UI
            updateSelectionUI(message)
            
            // Setup click handlers for multi-select
            setupClickHandlers(message)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
    
    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
    
    /**
     * Updates upload progress for a message
     */
    fun updateUploadProgress(messageId: String, progress: Float, state: String, error: String? = null) {
        val position = currentList.indexOfFirst { it.id == messageId }
        if (position != -1) {
            notifyItemChanged(position, UploadProgressPayload(progress, state, error))
        }
    }
    
    /**
     * Payload for upload progress updates
     */
    data class UploadProgressPayload(
        val progress: Float,
        val state: String, // UPLOADING, COMPLETED, FAILED
        val error: String? = null
    )

    /**
     * ViewHolder for image attachments
     */
    inner class ImageAttachmentViewHolder(itemView: View, private val isSent: Boolean) : BaseMessageViewHolder(itemView) {
        private val body: View = itemView.findViewById(R.id.body)
        private val imageGridLayout: GridLayout = itemView.findViewById(R.id.imageGridLayout)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val shimmerContainer: View = itemView.findViewById(R.id.shimmer_container)
        private val timeText: TextView = itemView.findViewById(R.id.date)
        private val senderUsername: TextView? = itemView.findViewById(R.id.senderUsername)
        private val messageBG: android.widget.LinearLayout = itemView.findViewById(R.id.messageBG)
        private val deletedMessagePlaceholder: View = itemView.findViewById(R.id.deletedMessagePlaceholder)
        private val messageContentContainer: View = itemView.findViewById(R.id.messageContentContainer)
        
        // Upload progress views
        private val uploadProgressOverlay: View = itemView.findViewById(R.id.uploadProgressOverlay)
        private val uploadProgressBar: ProgressBar = itemView.findViewById(R.id.uploadProgressBar)
        private val uploadProgressText: TextView = itemView.findViewById(R.id.uploadProgressText)
        private val uploadEstimatedTime: TextView = itemView.findViewById(R.id.uploadEstimatedTime)
        private val uploadErrorLayout: View = itemView.findViewById(R.id.uploadErrorLayout)
        private val uploadErrorText: TextView = itemView.findViewById(R.id.uploadErrorText)
        private val retryButton: View = itemView.findViewById(R.id.retryButton)
        private val uploadSuccessIcon: View = itemView.findViewById(R.id.uploadSuccessIcon)
        
        init {
            // Set alignment based on sent/received
            val params = body.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (isSent) {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            } else {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            }
            body.layoutParams = params
        }

        fun bind(message: Message, messagePosition: MessagePosition, payload: UploadProgressPayload? = null) {
            // Handle payload updates for progress
            if (payload != null) {
                updateProgress(payload)
                return
            }
            
            bind(message, messagePosition)
        }
        
        fun bind(message: Message, messagePosition: MessagePosition) {
            // Apply dynamic width - set max width on the LinearLayout
            val maxWidth = getMaxBubbleWidth()
            val layoutParams = messageBG.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            messageBG.layoutParams = layoutParams
            
            // Apply corner radii
            messageBG.background = createBubbleDrawable(messagePosition, isSent)
            
            // Check if message is deleted for current user
            val isDeletedForCurrentUser = currentUserId?.let { userId ->
                message.isDeletedForUser(userId, userDeletedMessageIds)
            } ?: false

            // Handle deleted messages - hide media content
            if (isDeletedForCurrentUser) {
                // Hide image grid
                imageGridLayout.visibility = View.GONE
                imageGridLayout.removeAllViews()
                
                // Show deleted message text
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = currentUserId?.let { userId ->
                    message.getDeletedMessageText(userId, userDeletedMessageIds)
                } ?: "This message was deleted"
                
                // Hide sender name for deleted messages
                senderUsername?.visibility = View.GONE
                
                timeText.text = formatTime(message.createdAt)
                
                // Update selection UI even for deleted messages
                updateSelectionUI(message)
                setupClickHandlers(message)
                return
            } else {
                // Show image grid for non-deleted messages
                imageGridLayout.visibility = View.VISIBLE
            }

            // Show sender name for received messages
            if (!isSent && message.senderName != null) {
                senderUsername?.visibility = View.VISIBLE
                senderUsername?.text = message.senderName
            } else {
                senderUsername?.visibility = View.GONE
            }

            // Clear previous images
            imageGridLayout.removeAllViews()

            val attachments = message.attachments?.filter { it.type == "image" } ?: emptyList()
            
            // Add images to grid (2 columns)
            attachments.forEachIndexed { index, attachment ->
                val imgView = ImageView(context)
                imgView.layoutParams = ViewGroup.LayoutParams(
                    if (attachments.size == 1) 600 else 280,
                    if (attachments.size == 1) 600 else 280
                )
                imgView.scaleType = ImageView.ScaleType.CENTER_CROP
                imgView.setPadding(4, 4, 4, 4)
                
                // Load thumbnail with Glide - use applicationContext to avoid destroyed activity crash
                try {
                    Glide.with(context.applicationContext)
                        .load(attachment.thumbnailUrl ?: attachment.url)
                        .placeholder(R.drawable.ph_imgbluredsqure)
                        .error(R.drawable.ph_imgbluredsqure)
                        .thumbnail(Glide.with(context.applicationContext).load(attachment.thumbnailUrl ?: attachment.url).sizeMultiplier(0.1f)) // Load low-res first
                        .centerCrop()
                        .into(imgView)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load image with Glide", e)
                    imgView.setImageResource(R.drawable.ph_imgbluredsqure)
                }
                
                // Click to open gallery (or toggle selection in multi-select mode)
                imgView.setOnClickListener {
                    if (isMultiSelectMode) {
                        multiSelectManager?.toggleMessageSelection(message.id)
                    } else {
                        val imageUrls = attachments.map { it.url }
                        onImageClick(imageUrls, index)
                    }
                }
                
                imageGridLayout.addView(imgView)
            }

            // Show caption if present
            if (!message.content.isNullOrEmpty()) {
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = message.content
            } else {
                shimmerContainer.visibility = View.GONE
            }

            timeText.text = formatTime(message.createdAt)
            
            // Hide upload progress overlay by default
            uploadProgressOverlay.visibility = View.GONE
            
            // Update selection UI
            updateSelectionUI(message)
            
            // Setup click handlers for multi-select
            setupClickHandlers(message)
        }
        
        private fun updateProgress(payload: UploadProgressPayload) {
            when (payload.state) {
                "UPLOADING", "COMPRESSING" -> {
                    uploadProgressOverlay.visibility = View.VISIBLE
                    uploadProgressBar.visibility = View.VISIBLE
                    uploadProgressText.visibility = View.VISIBLE
                    uploadErrorLayout.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    uploadSuccessIcon.visibility = View.GONE
                    
                    val progressPercent = (payload.progress * 100).toInt()
                    uploadProgressBar.progress = progressPercent
                    uploadProgressText.text = if (payload.state == "COMPRESSING") {
                        "Compressing... $progressPercent%"
                    } else {
                        "Uploading... $progressPercent%"
                    }
                    
                    // Show estimated time for large uploads
                    if (progressPercent > 0 && progressPercent < 100) {
                        uploadEstimatedTime.visibility = View.VISIBLE
                        val estimatedSeconds = ((100 - progressPercent) * 0.5).toInt()
                        uploadEstimatedTime.text = "Estimated: ${estimatedSeconds}s"
                    } else {
                        uploadEstimatedTime.visibility = View.GONE
                    }
                }
                "COMPLETED" -> {
                    uploadProgressBar.visibility = View.GONE
                    uploadProgressText.visibility = View.GONE
                    uploadEstimatedTime.visibility = View.GONE
                    uploadErrorLayout.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    uploadSuccessIcon.visibility = View.VISIBLE
                    
                    // Hide overlay after 1 second
                    itemView.postDelayed({
                        uploadProgressOverlay.visibility = View.GONE
                    }, 1000)
                }
                "FAILED" -> {
                    uploadProgressBar.visibility = View.GONE
                    uploadProgressText.visibility = View.GONE
                    uploadEstimatedTime.visibility = View.GONE
                    uploadSuccessIcon.visibility = View.GONE
                    uploadErrorLayout.visibility = View.VISIBLE
                    retryButton.visibility = View.VISIBLE
                    
                    uploadErrorText.text = payload.error ?: "Upload failed"
                }
                else -> {
                    uploadProgressOverlay.visibility = View.GONE
                }
            }
        }
    }

    /**
     * ViewHolder for video attachments
     */
    inner class VideoAttachmentViewHolder(itemView: View, private val isSent: Boolean) : BaseMessageViewHolder(itemView) {
        private val body: View = itemView.findViewById(R.id.body)
        private val videoThumbnail: ImageView = itemView.findViewById(R.id.videoThumbnail)
        private val playButton: ImageView = itemView.findViewById(R.id.playButton)
        private val videoDuration: TextView? = itemView.findViewById(R.id.videoDuration)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val shimmerContainer: View = itemView.findViewById(R.id.shimmer_container)
        private val timeText: TextView = itemView.findViewById(R.id.date)
        private val senderUsername: TextView? = itemView.findViewById(R.id.senderUsername)
        private val messageBG: android.widget.LinearLayout = itemView.findViewById(R.id.messageBG)
        private val deletedMessagePlaceholder: View = itemView.findViewById(R.id.deletedMessagePlaceholder)
        private val messageContentContainer: View = itemView.findViewById(R.id.messageContentContainer)
        
        init {
            // Set alignment based on sent/received
            val params = body.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (isSent) {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            } else {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            }
            body.layoutParams = params
        }

        fun bind(message: Message, messagePosition: MessagePosition, payload: UploadProgressPayload? = null) {
            // Handle payload updates for progress
            if (payload != null) {
                // Video upload progress handling can be added here if needed
                return
            }
            
            bind(message, messagePosition)
        }
        
        fun bind(message: Message, messagePosition: MessagePosition) {
            // Apply dynamic width - set max width on the LinearLayout
            val maxWidth = getMaxBubbleWidth()
            val layoutParams = messageBG.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            messageBG.layoutParams = layoutParams
            
            // Apply corner radii
            messageBG.background = createBubbleDrawable(messagePosition, isSent)
            
            // Check if message is deleted for current user
            val isDeletedForCurrentUser = currentUserId?.let { userId ->
                message.isDeletedForUser(userId, userDeletedMessageIds)
            } ?: false

            // Handle deleted messages - hide video content
            if (isDeletedForCurrentUser) {
                // Hide video thumbnail and play button
                videoThumbnail.visibility = View.GONE
                playButton.visibility = View.GONE
                videoDuration?.visibility = View.GONE
                
                // Show deleted message text
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = currentUserId?.let { userId ->
                    message.getDeletedMessageText(userId, userDeletedMessageIds)
                } ?: "This message was deleted"
                
                // Hide sender name for deleted messages
                senderUsername?.visibility = View.GONE
                
                timeText.text = formatTime(message.createdAt)
                
                // Update selection UI even for deleted messages
                updateSelectionUI(message)
                setupClickHandlers(message)
                return
            } else {
                // Show video content for non-deleted messages
                videoThumbnail.visibility = View.VISIBLE
                playButton.visibility = View.VISIBLE
            }

            // Show sender name for received messages
            if (!isSent && message.senderName != null) {
                senderUsername?.visibility = View.VISIBLE
                senderUsername?.text = message.senderName
            } else {
                senderUsername?.visibility = View.GONE
            }

            val attachment = message.attachments?.firstOrNull { it.type == "video" }
            
            if (attachment != null) {
                // Load video thumbnail with Glide - use applicationContext to avoid destroyed activity crash
                try {
                    Glide.with(context.applicationContext)
                        .load(attachment.thumbnailUrl ?: attachment.url)
                        .placeholder(R.drawable.ph_imgbluredsqure)
                        .error(R.drawable.ph_imgbluredsqure)
                        .thumbnail(Glide.with(context.applicationContext).load(attachment.thumbnailUrl ?: attachment.url).sizeMultiplier(0.1f)) // Load low-res first
                        .centerCrop()
                        .into(videoThumbnail)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load video thumbnail with Glide", e)
                    videoThumbnail.setImageResource(R.drawable.ph_imgbluredsqure)
                }
                
                // Show duration overlay
                attachment.duration?.let { duration ->
                    videoDuration?.visibility = View.VISIBLE
                    videoDuration?.text = formatDuration(duration)
                } ?: run {
                    videoDuration?.visibility = View.GONE
                }
                
                // Click to play video (or toggle selection in multi-select mode)
                videoThumbnail.setOnClickListener {
                    if (isMultiSelectMode) {
                        multiSelectManager?.toggleMessageSelection(message.id)
                    } else {
                        onVideoClick(attachment.url)
                    }
                }
                playButton.setOnClickListener {
                    if (isMultiSelectMode) {
                        multiSelectManager?.toggleMessageSelection(message.id)
                    } else {
                        onVideoClick(attachment.url)
                    }
                }
            }

            // Show caption if present
            if (!message.content.isNullOrEmpty()) {
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = message.content
            } else {
                shimmerContainer.visibility = View.GONE
            }

            timeText.text = formatTime(message.createdAt)
            
            // Update selection UI
            updateSelectionUI(message)
            
            // Setup click handlers for multi-select
            setupClickHandlers(message)
        }
    }

    /**
     * ViewHolder for audio attachments
     */
    inner class AudioAttachmentViewHolder(itemView: View, private val isSent: Boolean) : BaseMessageViewHolder(itemView) {
        private val body: View = itemView.findViewById(R.id.body)
        private val audioFileName: TextView = itemView.findViewById(R.id.audioFileName)
        private val playPauseButton: ImageButton = itemView.findViewById(R.id.playPauseButton)
        private val seekBar: SeekBar = itemView.findViewById(R.id.seekBar)
        private val currentTime: TextView = itemView.findViewById(R.id.currentTime)
        private val totalDuration: TextView = itemView.findViewById(R.id.totalDuration)
        private val loadingIndicator: ProgressBar = itemView.findViewById(R.id.loadingIndicator)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val shimmerContainer: View = itemView.findViewById(R.id.shimmer_container)
        private val timeText: TextView = itemView.findViewById(R.id.date)
        private val senderUsername: TextView? = itemView.findViewById(R.id.senderUsername)
        private val messageBG: android.widget.LinearLayout = itemView.findViewById(R.id.messageBG)
        private val deletedMessagePlaceholder: View = itemView.findViewById(R.id.deletedMessagePlaceholder)
        private val messageContentContainer: View = itemView.findViewById(R.id.messageContentContainer)
        
        init {
            // Set alignment based on sent/received
            val params = body.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (isSent) {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            } else {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            }
            body.layoutParams = params
        }

        fun bind(message: Message, messagePosition: MessagePosition, payload: UploadProgressPayload? = null) {
            // Handle payload updates for progress
            if (payload != null) {
                // Audio upload progress handling can be added here if needed
                return
            }
            
            bind(message, messagePosition)
        }
        
        fun bind(message: Message, messagePosition: MessagePosition) {
            // Apply dynamic width - set max width on the LinearLayout
            val maxWidth = getMaxBubbleWidth()
            val layoutParams = messageBG.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            messageBG.layoutParams = layoutParams
            
            // Apply corner radii
            messageBG.background = createBubbleDrawable(messagePosition, isSent)
            
            // Check if message is deleted for current user
            val isDeletedForCurrentUser = currentUserId?.let { userId ->
                message.isDeletedForUser(userId, userDeletedMessageIds)
            } ?: false

            // Handle deleted messages - hide audio content
            if (isDeletedForCurrentUser) {
                // Hide audio controls
                audioFileName.visibility = View.GONE
                playPauseButton.visibility = View.GONE
                seekBar.visibility = View.GONE
                currentTime.visibility = View.GONE
                totalDuration.visibility = View.GONE
                loadingIndicator.visibility = View.GONE
                
                // Show deleted message text
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = currentUserId?.let { userId ->
                    message.getDeletedMessageText(userId, userDeletedMessageIds)
                } ?: "This message was deleted"
                
                // Hide sender name for deleted messages
                senderUsername?.visibility = View.GONE
                
                timeText.text = formatTime(message.createdAt)
                
                // Update selection UI even for deleted messages
                updateSelectionUI(message)
                setupClickHandlers(message)
                return
            } else {
                // Show audio controls for non-deleted messages
                audioFileName.visibility = View.VISIBLE
                playPauseButton.visibility = View.VISIBLE
                seekBar.visibility = View.VISIBLE
                currentTime.visibility = View.VISIBLE
                totalDuration.visibility = View.VISIBLE
            }

            // Show sender name for received messages
            if (!isSent && message.senderName != null) {
                senderUsername?.visibility = View.VISIBLE
                senderUsername?.text = message.senderName
            } else {
                senderUsername?.visibility = View.GONE
            }

            val attachment = message.attachments?.firstOrNull { it.type == "audio" }
            
            if (attachment != null) {
                // Set file name
                audioFileName.text = attachment.fileName ?: "Audio"
                
                // Set duration
                attachment.duration?.let { duration ->
                    totalDuration.text = formatDuration(duration)
                    currentTime.text = "0:00"
                    seekBar.max = (duration / 1000).toInt()
                    seekBar.progress = 0
                } ?: run {
                    totalDuration.text = "0:00"
                    currentTime.text = "0:00"
                }
                
                // Hide loading indicator (will be shown during playback)
                loadingIndicator.visibility = View.GONE
                
                // Click to play audio (or toggle selection in multi-select mode)
                playPauseButton.setOnClickListener {
                    if (isMultiSelectMode) {
                        multiSelectManager?.toggleMessageSelection(message.id)
                    } else {
                        onAudioClick(attachment.url)
                    }
                }
            }

            // Show caption if present
            if (!message.content.isNullOrEmpty()) {
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = message.content
            } else {
                shimmerContainer.visibility = View.GONE
            }

            timeText.text = formatTime(message.createdAt)
            
            // Update selection UI
            updateSelectionUI(message)
            
            // Setup click handlers for multi-select
            setupClickHandlers(message)
        }
    }

    /**
     * ViewHolder for document attachments
     */
    inner class DocumentAttachmentViewHolder(itemView: View, private val isSent: Boolean) : BaseMessageViewHolder(itemView) {
        private val body: View = itemView.findViewById(R.id.body)
        private val documentIcon: ImageView = itemView.findViewById(R.id.documentIcon)
        private val documentFileName: TextView = itemView.findViewById(R.id.documentFileName)
        private val documentFileInfo: TextView = itemView.findViewById(R.id.documentFileInfo)
        private val downloadButton: ImageButton = itemView.findViewById(R.id.downloadButton)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val shimmerContainer: View = itemView.findViewById(R.id.shimmer_container)
        private val timeText: TextView = itemView.findViewById(R.id.date)
        private val senderUsername: TextView? = itemView.findViewById(R.id.senderUsername)
        private val messageBG: android.widget.LinearLayout = itemView.findViewById(R.id.messageBG)
        private val deletedMessagePlaceholder: View = itemView.findViewById(R.id.deletedMessagePlaceholder)
        private val messageContentContainer: View = itemView.findViewById(R.id.messageContentContainer)
        
        init {
            // Set alignment based on sent/received
            val params = body.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (isSent) {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            } else {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            }
            body.layoutParams = params
        }

        fun bind(message: Message, messagePosition: MessagePosition, payload: UploadProgressPayload? = null) {
            // Handle payload updates for progress
            if (payload != null) {
                // Document upload progress handling can be added here if needed
                return
            }
            
            bind(message, messagePosition)
        }
        
        fun bind(message: Message, messagePosition: MessagePosition) {
            // Apply dynamic width - set max width on the LinearLayout
            val maxWidth = getMaxBubbleWidth()
            val layoutParams = messageBG.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            messageBG.layoutParams = layoutParams
            
            // Apply corner radii
            messageBG.background = createBubbleDrawable(messagePosition, isSent)
            
            // Check if message is deleted for current user
            val isDeletedForCurrentUser = currentUserId?.let { userId ->
                message.isDeletedForUser(userId, userDeletedMessageIds)
            } ?: false

            // Handle deleted messages - hide document content
            if (isDeletedForCurrentUser) {
                // Hide document controls
                documentIcon.visibility = View.GONE
                documentFileName.visibility = View.GONE
                documentFileInfo.visibility = View.GONE
                downloadButton.visibility = View.GONE
                
                // Show deleted message text
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = currentUserId?.let { userId ->
                    message.getDeletedMessageText(userId, userDeletedMessageIds)
                } ?: "This message was deleted"
                
                // Hide sender name for deleted messages
                senderUsername?.visibility = View.GONE
                
                timeText.text = formatTime(message.createdAt)
                
                // Update selection UI even for deleted messages
                updateSelectionUI(message)
                setupClickHandlers(message)
                return
            } else {
                // Show document controls for non-deleted messages
                documentIcon.visibility = View.VISIBLE
                documentFileName.visibility = View.VISIBLE
                documentFileInfo.visibility = View.VISIBLE
                downloadButton.visibility = View.VISIBLE
            }

            // Show sender name for received messages
            if (!isSent && message.senderName != null) {
                senderUsername?.visibility = View.VISIBLE
                senderUsername?.text = message.senderName
            } else {
                senderUsername?.visibility = View.GONE
            }

            val attachment = message.attachments?.firstOrNull { it.type == "document" }
            
            if (attachment != null) {
                // Set file name
                documentFileName.text = attachment.fileName ?: "Document"
                
                // Set file info (type and size)
                val fileType = attachment.mimeType?.substringAfterLast("/")?.uppercase() ?: "FILE"
                val fileSize = attachment.fileSize?.let { formatFileSize(it) } ?: ""
                documentFileInfo.text = if (fileSize.isNotEmpty()) {
                    "$fileType • $fileSize"
                } else {
                    fileType
                }
                
                // Click to open document (handled by setupClickHandlers for multi-select)
                downloadButton.setOnClickListener {
                    if (!isMultiSelectMode) {
                        onDocumentClick(attachment)
                    }
                }
                // Note: itemView click is handled by setupClickHandlers
            }

            // Show caption if present
            if (!message.content.isNullOrEmpty()) {
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = message.content
            } else {
                shimmerContainer.visibility = View.GONE
            }

            timeText.text = formatTime(message.createdAt)
            
            // Update selection UI
            updateSelectionUI(message)
            
            // Setup click handlers for multi-select
            setupClickHandlers(message)
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
