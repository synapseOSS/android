package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.chat.interfaces.ChatAdapterListener
import com.synapse.social.studioasinc.util.MessageAnimations
import com.synapse.social.studioasinc.util.setMessageState
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("UNCHECKED_CAST")
class ChatAdapter(
    private val data: ArrayList<HashMap<String, Any?>>,
    private val repliedMessagesCache: HashMap<String, HashMap<String, Any?>>,
    private val listener: ChatAdapterListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "ChatAdapter"
        private const val VIEW_TYPE_TEXT = 1
        private const val VIEW_TYPE_MEDIA_GRID = 2
        private const val VIEW_TYPE_TYPING = 3
        private const val VIEW_TYPE_VIDEO = 4
        private const val VIEW_TYPE_LINK_PREVIEW = 5
        private const val VIEW_TYPE_VOICE_MESSAGE = 6
        private const val VIEW_TYPE_ERROR = 7
        private const val VIEW_TYPE_LOADING_MORE = 99
        
        /** Maximum bubble width as a percentage of screen width (75%) */
        private const val MAX_BUBBLE_WIDTH_PERCENT = 0.75
    }
    
    /**
     * Calculate the maximum bubble width based on device screen width.
     * Returns 75% of the screen width to prevent bubbles from stretching
     * too wide on tablets and foldables.
     */
    private fun getMaxBubbleWidth(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return (displayMetrics.widthPixels * MAX_BUBBLE_WIDTH_PERCENT).toInt()
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

    private var context: Context? = null
    private var secondUserAvatarUrl = ""
    private var firstUserName = ""
    private var secondUserName = ""
    private var appSettings: SharedPreferences? = null
    private var isGroupChat = false
    private var userNamesMap = HashMap<String, String>()
    private var previousSenderId: String? = null
    
    // Multi-select mode state
    var isMultiSelectMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    
    // Multi-select callbacks
    var onEnterMultiSelectMode: ((String) -> Unit)? = null
    var onToggleMessageSelection: ((String) -> Unit)? = null
    var isMessageSelected: ((String) -> Boolean)? = null
    
    // Scroll state tracking for performance optimization
    private var isScrolling = false
    private var scrollStartTime = 0L
    
    // Supabase services
    private val authService = SupabaseAuthenticationService()
    private val databaseService = SupabaseDatabaseService()
    
    // Performance optimization: Track scroll state
    fun setScrolling(scrolling: Boolean) {
        if (scrolling && !isScrolling) {
            scrollStartTime = System.currentTimeMillis()
        }
        isScrolling = scrolling
    }
    
    /**
     * Minimal binding for fast scroll performance
     * Only binds essential elements during scroll
     */
    private fun bindMinimalViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val messageData = data[position]
        
        if (holder is BaseMessageViewHolder) {
            // Only bind essential message properties during scroll
            val currentUser = authService.getCurrentUser()
            val myUid = currentUser?.id ?: ""
            val msgUid = messageData["sender_id"]?.toString() 
                ?: messageData["uid"]?.toString() 
                ?: ""
            val isMyMessage = msgUid == myUid
            
            // Set basic layout alignment
            holder.bodyLayout?.let { body ->
                val layoutParams = body.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                layoutParams?.let { params ->
                    if (isMyMessage) {
                        params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                        params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    } else {
                        params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                        params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    }
                    body.layoutParams = params
                }
            }
            
            // Apply basic bubble background (always use SINGLE for performance)
            applyMessageBubbleBackground(holder, MessagePosition.SINGLE, isMyMessage)
            
            // Hide optional elements during scroll
            holder.senderUsername?.visibility = View.GONE
            holder.messageTime?.visibility = View.GONE
            holder.editedIndicator?.visibility = View.GONE
            holder.forwardedIndicator?.visibility = View.GONE
            holder.replyLayout?.visibility = View.GONE
        }
        
        // Bind minimal content based on type
        when (holder) {
            is TextViewHolder -> {
                val messageText = messageData["content"]?.toString() 
                    ?: messageData["message_text"]?.toString() 
                    ?: ""
                holder.messageText.text = messageText
            }
            is MediaViewHolder -> {
                // Skip image loading during scroll for performance
                holder.mediaGrid.removeAllViews()
                val attachments = messageData["attachments"] as? ArrayList<HashMap<String, Any?>>
                val caption = messageData["content"]?.toString() 
                    ?: messageData["message_text"]?.toString() 
                    ?: ""
                holder.mediaCaption?.text = caption
            }
            is VideoViewHolder -> {
                // Skip thumbnail loading during scroll
                val caption = messageData["content"]?.toString() 
                    ?: messageData["message_text"]?.toString() 
                    ?: ""
                holder.videoCaption?.text = caption
            }
        }
    }
    
    // Link preview cache to avoid refetching
    private val linkPreviewCache = HashMap<String, LinkPreviewUtil.LinkData>()

    // Setter methods for configuration
    fun setSecondUserAvatar(url: String) { secondUserAvatarUrl = url }
    fun setFirstUserName(name: String) { firstUserName = name }
    fun setSecondUserName(name: String) { secondUserName = name }
    fun setGroupChat(isGroup: Boolean) { isGroupChat = isGroup }
    fun setUserNamesMap(map: HashMap<String, String>) { userNamesMap = map }

    override fun getItemViewType(position: Int): Int {
        val item = data[position]
        
        if (item.containsKey("isLoadingMore")) return VIEW_TYPE_LOADING_MORE
        if (item.containsKey("typingMessageStatus")) return VIEW_TYPE_TYPING
        
        // Check for error/failed messages - support both field names
        val deliveryStatus = item["delivery_status"]?.toString() 
            ?: item["message_state"]?.toString() 
            ?: ""
        if (deliveryStatus == "failed" || deliveryStatus == "error") {
            Log.d(TAG, "Error message detected at position $position")
            return VIEW_TYPE_ERROR
        }
        
        val type = item["TYPE"]?.toString() ?: "MESSAGE"
        Log.d(TAG, "Message at position $position has type: $type")

        return when (type) {
            "VOICE_MESSAGE" -> VIEW_TYPE_VOICE_MESSAGE
            "ATTACHMENT_MESSAGE" -> {
                val attachments = item["attachments"] as? ArrayList<HashMap<String, Any?>>
                Log.d(TAG, "ATTACHMENT_MESSAGE detected with ${attachments?.size ?: 0} attachments")

                if (attachments?.size == 1 && 
                    attachments[0]["publicId"]?.toString()?.contains("|video") == true) {
                    Log.d(TAG, "Video message detected, returning VIEW_TYPE_VIDEO")
                    VIEW_TYPE_VIDEO
                } else {
                    Log.d(TAG, "Media message detected, returning VIEW_TYPE_MEDIA_GRID")
                    VIEW_TYPE_MEDIA_GRID
                }
            }
            else -> {
                val messageText = item["content"]?.toString() 
                    ?: item["message_text"]?.toString() 
                    ?: ""
                if (LinkPreviewUtil.extractUrl(messageText) != null) {
                    Log.d(TAG, "Link preview message detected, returning VIEW_TYPE_LINK_PREVIEW")
                    VIEW_TYPE_LINK_PREVIEW
                } else {
                    Log.d(TAG, "Text message detected, returning VIEW_TYPE_TEXT")
                    VIEW_TYPE_TEXT
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return try {
            val keyObj = data[position]["key"] ?: data[position]["KEY_KEY"] ?: position
            keyObj.toString().hashCode().toLong()
        } catch (e: Exception) {
            position.toLong()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        appSettings = context?.getSharedPreferences("appSettings", Context.MODE_PRIVATE)
        val inflater = LayoutInflater.from(context)
        
        return when (viewType) {
            VIEW_TYPE_MEDIA_GRID -> MediaViewHolder(inflater.inflate(R.layout.chat_bubble_media, parent, false))
            VIEW_TYPE_VIDEO -> VideoViewHolder(inflater.inflate(R.layout.chat_bubble_video, parent, false))
            VIEW_TYPE_TYPING -> TypingViewHolder(inflater.inflate(R.layout.chat_bubble_typing, parent, false))
            VIEW_TYPE_LINK_PREVIEW -> LinkPreviewViewHolder(inflater.inflate(R.layout.chat_bubble_link_preview, parent, false))
            VIEW_TYPE_VOICE_MESSAGE -> VoiceMessageViewHolder(inflater.inflate(R.layout.chat_bubble_voice, parent, false))
            VIEW_TYPE_ERROR -> ErrorViewHolder(inflater.inflate(R.layout.chat_bubble_error, parent, false))
            VIEW_TYPE_LOADING_MORE -> LoadingViewHolder(inflater.inflate(R.layout.chat_bubble_loading_more, parent, false))
            else -> TextViewHolder(inflater.inflate(R.layout.chat_bubble_text, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Performance optimization: Skip heavy operations during fast scroll
        if (isScrolling && position > 0 && position < data.size - 1) {
            bindMinimalViewHolder(holder, position)
            return
        }
        
        when (holder.itemViewType) {
            VIEW_TYPE_TEXT -> bindTextViewHolder(holder as TextViewHolder, position)
            VIEW_TYPE_MEDIA_GRID -> bindMediaViewHolder(holder as MediaViewHolder, position)
            VIEW_TYPE_VIDEO -> bindVideoViewHolder(holder as VideoViewHolder, position)
            VIEW_TYPE_TYPING -> bindTypingViewHolder(holder as TypingViewHolder, position)
            VIEW_TYPE_LINK_PREVIEW -> bindLinkPreviewViewHolder(holder as LinkPreviewViewHolder, position)
            VIEW_TYPE_VOICE_MESSAGE -> bindVoiceMessageViewHolder(holder as VoiceMessageViewHolder, position)
            VIEW_TYPE_ERROR -> bindErrorViewHolder(holder as ErrorViewHolder, position)
            VIEW_TYPE_LOADING_MORE -> bindLoadingViewHolder(holder as LoadingViewHolder, position)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.contains("message_state_update")) {
            // Handle partial update for message state only
            if (holder is BaseMessageViewHolder) {
                updateMessageStateIcon(holder, position)
            }
        } else {
            // Full bind
            onBindViewHolder(holder, position)
        }
    }

    /**
     * Update only the message state icon with fade animation
     */
    private fun updateMessageStateIcon(holder: BaseMessageViewHolder, position: Int) {
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        val msgUid = messageData["sender_id"]?.toString() 
            ?: messageData["uid"]?.toString() 
            ?: ""
        val isMyMessage = msgUid == myUid
        
        holder.messageStatus?.let { statusView ->
            if (isMyMessage) {
                val deliveryStatus = messageData["delivery_status"]?.toString() 
                    ?: messageData["message_state"]?.toString() 
                    ?: "sent"
                
                // Apply fade animation for state change
                statusView.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        // Update the icon using extension function
                        statusView.setMessageState(deliveryStatus)
                        statusView.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start()
                    }
                    .start()
            } else {
                statusView.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VoiceMessageViewHolder) {
            holder.mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
                holder.mediaPlayer = null
            }
            holder.handler?.removeCallbacksAndMessages(null)
        }
    }

    // Base ViewHolder class
    abstract class BaseMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderUsername: TextView? = try { itemView.findViewById(R.id.senderUsername) } catch (e: ClassCastException) { null }
        val forwardedIndicator: LinearLayout? = try { itemView.findViewById(R.id.forwardedIndicator) } catch (e: ClassCastException) { null }
        val editedIndicator: TextView? = try { itemView.findViewById(R.id.editedIndicator) } catch (e: ClassCastException) { null }
        val messageTime: TextView? = try { itemView.findViewById(R.id.date) } catch (e: ClassCastException) { null }
        val messageStatus: ImageView? = try { itemView.findViewById(R.id.message_state) } catch (e: ClassCastException) { null }
        val replyLayout: LinearLayout? = try { itemView.findViewById(R.id.mRepliedMessageLayout) } catch (e: ClassCastException) { null }
        val replyUsername: TextView? = try { itemView.findViewById(R.id.mRepliedMessageLayoutUsername) } catch (e: ClassCastException) { null }
        val replyText: TextView? = try { itemView.findViewById(R.id.mRepliedMessageLayoutMessage) } catch (e: ClassCastException) { null }
        val replyImage: ImageView? = try { itemView.findViewById(R.id.mRepliedMessageLayoutImage) } catch (e: ClassCastException) { null }
        val messageBubble: LinearLayout? = try { itemView.findViewById(R.id.messageBG) } catch (e: ClassCastException) { null }
        val messageLayout: LinearLayout? = try { itemView.findViewById(R.id.message_layout) } catch (e: ClassCastException) { null }
        val bodyLayout: LinearLayout? = try { itemView.findViewById(R.id.body) } catch (e: ClassCastException) { null }
        val deletedMessagePlaceholder: LinearLayout? = try { itemView.findViewById(R.id.deletedMessagePlaceholder) } catch (e: ClassCastException) { null }
        val messageContentContainer: LinearLayout? = try { itemView.findViewById(R.id.messageContentContainer) } catch (e: ClassCastException) { null }
    }

    // Text Message ViewHolder
    class TextViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
    }

    // Media Message ViewHolder
    class MediaViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
        val mediaGrid: androidx.gridlayout.widget.GridLayout = itemView.findViewById(R.id.mediaGridLayout)
        val mediaCaption: TextView? = itemView.findViewById(R.id.message_text)
    }

    // Video Message ViewHolder
    class VideoViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
        val videoThumbnail: ImageView = itemView.findViewById(R.id.videoThumbnail)
        val playButton: ImageView = itemView.findViewById(R.id.playButton)
        val videoDuration: TextView? = itemView.findViewById(R.id.date) // Use date field for duration
        val videoCaption: TextView? = itemView.findViewById(R.id.message_text)
    }

    // Typing Indicator ViewHolder
    class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typingAnimation: com.airbnb.lottie.LottieAnimationView = itemView.findViewById(R.id.lottie_typing)
        val typingIndicatorView: com.synapse.social.studioasinc.chat.TypingIndicatorView? = 
            itemView.findViewById(R.id.typing_animation)
    }

    // Link Preview ViewHolder
    class LinkPreviewViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val linkPreviewCard: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.linkPreviewContainer)
        val linkImage: ImageView = itemView.findViewById(R.id.linkPreviewImage)
        val linkTitle: TextView = itemView.findViewById(R.id.linkPreviewTitle)
        val linkDescription: TextView = itemView.findViewById(R.id.linkPreviewDescription)
        val linkUrl: TextView = itemView.findViewById(R.id.linkPreviewDomain)
    }

    // Voice Message ViewHolder
    class VoiceMessageViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
        val playPauseButton: ImageView = itemView.findViewById(R.id.play_pause_button)
        val waveform: SeekBar = itemView.findViewById(R.id.voice_seekbar)
        val duration: TextView = itemView.findViewById(R.id.voice_duration)
        var mediaPlayer: MediaPlayer? = null
        var handler: Handler? = null
    }

    // Error Message ViewHolder
    class ErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val errorMessageText: TextView = itemView.findViewById(R.id.error_message_text)
        val retryText: TextView = itemView.findViewById(R.id.retry_text)
        val errorIcon: ImageView = itemView.findViewById(R.id.error_icon)
        val messageTime: TextView? = itemView.findViewById(R.id.date)
        val messageLayout: LinearLayout? = itemView.findViewById(R.id.message_layout)
    }

    // Loading More ViewHolder
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val loadingProgress: ProgressBar = itemView.findViewById(R.id.loadingMoreProgressBar)
    }

    // Binding methods for each view type
    private fun bindCommonMessageProperties(holder: BaseMessageViewHolder, position: Int) {
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        
        // Apply entrance animation for new messages
        if (position == data.size - 1 && messageData["is_new"] == true) {
            val isMyMessage = (messageData["sender_id"]?.toString() ?: messageData["uid"]?.toString()) == myUid
            com.synapse.social.studioasinc.util.MessageAnimationHelper.animateMessageReceive(
                holder.itemView, 
                isMyMessage
            ) {
                messageData.remove("is_new")
            }
        }
        // Support both old (uid) and new (sender_id) field names
        val msgUid = messageData["sender_id"]?.toString() 
            ?: messageData["uid"]?.toString() 
            ?: ""
        val isMyMessage = msgUid == myUid
        
        // Calculate message position for grouping BEFORE any other styling
        val messagePosition = calculateMessagePosition(position)
        
        // Check if message is deleted
        val isDeleted = messageData["is_deleted"]?.toString()?.toBooleanStrictOrNull() ?: false
        val deleteForEveryone = messageData["delete_for_everyone"]?.toString()?.toBooleanStrictOrNull() ?: false
        
        // Handle deleted message display
        if (isDeleted || deleteForEveryone) {
            // Show deleted placeholder, hide content
            holder.deletedMessagePlaceholder?.visibility = View.VISIBLE
            holder.messageContentContainer?.visibility = View.GONE
            
            // Keep timestamp and sender info visible but hide message status
            holder.messageStatus?.visibility = View.GONE
            
            // Disable long-press for deleted messages
            holder.itemView.setOnLongClickListener(null)
            holder.itemView.isLongClickable = false
            
            // Still allow regular click for navigation
            holder.itemView.setOnClickListener {
                val messageId = messageData["id"]?.toString() 
                    ?: messageData["key"]?.toString() 
                    ?: ""
                listener.onMessageClick(messageId, position)
            }
            
            // Set message layout alignment using ConstraintLayout
            holder.bodyLayout?.let { body ->
                val layoutParams = body.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                layoutParams?.let { params ->
                    if (isMyMessage) {
                        // Align to right
                        params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                        params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    } else {
                        // Align to left
                        params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                        params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    }
                    body.layoutParams = params
                }
            }
            
            // Set message bubble background for deleted messages (always use SINGLE position)
            applyMessageBubbleBackground(holder, MessagePosition.SINGLE, isMyMessage)
            
            // Set message time (deleted messages always show timestamp)
            holder.messageTime?.let { timeView ->
                val timestamp = messageData["created_at"]?.toString()?.toLongOrNull()
                    ?: messageData["push_date"]?.toString()?.toLongOrNull() 
                    ?: System.currentTimeMillis()
                timeView.text = formatMessageTime(timestamp)
                timeView.visibility = View.VISIBLE
            }
            
            // Handle username display for group chats (deleted messages use SINGLE position logic)
            holder.senderUsername?.let { usernameView ->
                if (shouldShowUsername(position, MessagePosition.SINGLE, isGroupChat, isMyMessage) && userNamesMap.containsKey(msgUid)) {
                    usernameView.visibility = View.VISIBLE
                    usernameView.text = userNamesMap[msgUid]
                } else {
                    usernameView.visibility = View.GONE
                }
            }
            
            // Apply dynamic vertical spacing (deleted messages always use ungrouped spacing)
            holder.bodyLayout?.let { bodyLayout ->
                val layoutParams = bodyLayout.layoutParams as? ViewGroup.MarginLayoutParams
                layoutParams?.let { params ->
                    val context = holder.itemView.context
                    // Deleted messages always use ungrouped spacing since they break groups
                    val spacing = context.resources.getDimensionPixelSize(R.dimen.message_spacing_ungrouped)
                    params.topMargin = spacing
                    bodyLayout.layoutParams = params
                }
            }
            
            // Update previous sender ID
            previousSenderId = msgUid
            
            // Early return - don't process normal message content
            return
        }
        
        // Message is not deleted - show content, hide placeholder
        holder.deletedMessagePlaceholder?.visibility = View.GONE
        holder.messageContentContainer?.visibility = View.VISIBLE
        
        // Apply dynamic vertical spacing based on message grouping position
        holder.bodyLayout?.let { bodyLayout ->
            val layoutParams = bodyLayout.layoutParams as? ViewGroup.MarginLayoutParams
            layoutParams?.let { params ->
                val context = holder.itemView.context
                // Use grouped spacing (2dp) for messages in a group, ungrouped spacing (12dp) otherwise
                val spacing = when (messagePosition) {
                    MessagePosition.MIDDLE, MessagePosition.LAST -> {
                        // Messages in the middle or at the end of a group use tight spacing
                        context.resources.getDimensionPixelSize(R.dimen.message_spacing_grouped)
                    }
                    MessagePosition.SINGLE, MessagePosition.FIRST -> {
                        // Single messages or first in group use larger spacing
                        context.resources.getDimensionPixelSize(R.dimen.message_spacing_ungrouped)
                    }
                }
                params.topMargin = spacing
                bodyLayout.layoutParams = params
            }
        }
        
        // Update previous sender ID for next message
        previousSenderId = msgUid
        
        // Handle username display for group chats using grouping logic
        holder.senderUsername?.let { usernameView ->
            if (shouldShowUsername(position, messagePosition, isGroupChat, isMyMessage) && userNamesMap.containsKey(msgUid)) {
                usernameView.visibility = View.VISIBLE
                usernameView.text = userNamesMap[msgUid]
            } else {
                usernameView.visibility = View.GONE
            }
        }
        
        // Handle forwarded indicator display
        holder.forwardedIndicator?.let { forwardedView ->
            val forwardedFromMessageId = messageData["forwarded_from_message_id"]?.toString()
            if (!forwardedFromMessageId.isNullOrEmpty()) {
                forwardedView.visibility = View.VISIBLE
            } else {
                forwardedView.visibility = View.GONE
            }
        }
        
        // Set message time using grouping logic - support both old and new field names
        holder.messageTime?.let { timeView ->
            val timestamp = messageData["created_at"]?.toString()?.toLongOrNull()
                ?: messageData["push_date"]?.toString()?.toLongOrNull() 
                ?: System.currentTimeMillis()
            
            if (shouldShowTimestamp(position, messagePosition)) {
                timeView.text = formatMessageTime(timestamp)
                timeView.visibility = View.VISIBLE
            } else {
                timeView.visibility = View.GONE
            }
        }
        
        // Handle edited indicator display
        holder.editedIndicator?.let { editedView ->
            val isEdited = messageData["is_edited"]?.toString()?.toBooleanStrictOrNull() ?: false
            if (isEdited) {
                editedView.visibility = View.VISIBLE
                // Add click listener to show edit history dialog
                editedView.setOnClickListener {
                    val messageId = messageData["id"]?.toString() 
                        ?: messageData["key"]?.toString() 
                        ?: ""
                    listener.onEditHistoryClick(messageId)
                }
            } else {
                editedView.visibility = View.GONE
                editedView.setOnClickListener(null)
            }
        }
        
        // Set message status for sent messages using extension function - support both field names
        holder.messageStatus?.let { statusView ->
            if (isMyMessage) {
                val deliveryStatus = messageData["delivery_status"]?.toString() 
                    ?: messageData["message_state"]?.toString() 
                    ?: "sent"
                
                // Use the extension function to set the appropriate icon and styling
                statusView.setMessageState(deliveryStatus)
                statusView.visibility = View.VISIBLE
            } else {
                // Hide read receipt icons for incoming messages
                statusView.visibility = View.GONE
            }
        }
        
        // Handle reply layout with WhatsApp-style display
        holder.replyLayout?.let { replyLayout ->
            val repliedMessageId = messageData["replied_message_id"]?.toString() 
                ?: messageData["reply_to_id"]?.toString()
            
            if (!repliedMessageId.isNullOrEmpty()) {
                // Try to find the replied message in the current data list
                val repliedMessage = repliedMessagesCache[repliedMessageId] 
                    ?: data.find { it["id"]?.toString() == repliedMessageId }
                
                if (repliedMessage != null) {
                    // Set reply username - always show "You" for current user's messages
                    val replySenderId = repliedMessage["sender_id"]?.toString() 
                        ?: repliedMessage["uid"]?.toString()
                    val replyUsername = if (replySenderId == myUid) {
                        "You"
                    } else if (isGroupChat && userNamesMap.containsKey(replySenderId)) {
                        userNamesMap[replySenderId] ?: "User"
                    } else {
                        secondUserName.ifEmpty { "User" }
                    }
                    holder.replyUsername?.text = replyUsername
                    
                    // Set reply message text (maxLines=2 is set in XML)
                    val replyText = repliedMessage["content"]?.toString() 
                        ?: repliedMessage["message_text"]?.toString() 
                        ?: "Message"
                    holder.replyText?.text = replyText
                    
                    // Handle reply image preview if message has attachments
                    val attachments = repliedMessage["attachments"] as? ArrayList<HashMap<String, Any?>>
                    val firstAttachment = attachments?.firstOrNull()
                    val attachmentUrl = firstAttachment?.get("url")?.toString()
                    val attachmentType = firstAttachment?.get("type")?.toString()
                    
                    if (!attachmentUrl.isNullOrEmpty() && attachmentType == "image") {
                        holder.replyImage?.visibility = View.VISIBLE
                        context?.let { ctx ->
                            Glide.with(ctx)
                                .load(attachmentUrl)
                                .transform(RoundedCorners(8))
                                .placeholder(R.drawable.ph_imgbluredsqure)
                                .error(R.drawable.ph_imgbluredsqure)
                                .into(holder.replyImage!!)
                        }
                    } else {
                        holder.replyImage?.visibility = View.GONE
                    }
                    
                    // Set click listener to scroll to replied message
                    replyLayout.setOnClickListener {
                        val position = data.indexOfFirst { it["id"]?.toString() == repliedMessageId }
                        if (position != -1) {
                            listener.onReplyClick(repliedMessageId, replyText, replyUsername)
                        }
                    }
                    
                    replyLayout.visibility = View.VISIBLE
                } else {
                    // Replied message not found - show placeholder
                    holder.replyUsername?.text = "Unknown"
                    holder.replyText?.text = "Message not available"
                    holder.replyImage?.visibility = View.GONE
                    replyLayout.setOnClickListener(null)
                    replyLayout.visibility = View.VISIBLE
                }
            } else {
                replyLayout.visibility = View.GONE
            }
        }
        
        // Set message layout alignment using ConstraintLayout
        holder.bodyLayout?.let { body ->
            val layoutParams = body.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams?.let { params ->
                if (isMyMessage) {
                    // Align to right
                    params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                } else {
                    // Align to left
                    params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                }
                body.layoutParams = params
            }
        }
        
        // Set message layout alignment - message_layout is now a LinearLayout
        holder.messageLayout?.let { layout ->
            val layoutParams = layout.layoutParams as? LinearLayout.LayoutParams
            if (layoutParams != null) {
                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                layoutParams.gravity = if (isMyMessage) Gravity.END else Gravity.START
                layout.layoutParams = layoutParams
            }
        }
        
        // Apply responsive bubble width constraint (75% of screen width)
        // Prevents bubbles from stretching too wide on tablets/foldables
        context?.let { ctx ->
            val maxWidth = getMaxBubbleWidth(ctx)
            holder.messageBubble?.let { bubble ->
                val currentParams = bubble.layoutParams
                if (currentParams is LinearLayout.LayoutParams) {
                    currentParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                // Set max width constraint via post to ensure layout is ready
                bubble.post {
                    if (bubble.width > maxWidth) {
                        val newParams = bubble.layoutParams
                        newParams?.width = maxWidth
                        bubble.layoutParams = newParams
                    }
                }
            }
        }
        
        // Set message bubble background using grouping logic
        applyMessageBubbleBackground(holder, messagePosition, isMyMessage)
        
        // Set click listeners - support both id field names
        holder.itemView.setOnClickListener {
            val messageId = messageData["id"]?.toString() 
                ?: messageData["key"]?.toString() 
                ?: ""
            
            if (isMultiSelectMode) {
                // Toggle selection in multi-select mode
                onToggleMessageSelection?.invoke(messageId)
            } else {
                // Normal click behavior
                listener.onMessageClick(messageId, position)
            }
        }
        
        holder.itemView.setOnLongClickListener {
            val messageId = messageData["id"]?.toString() 
                ?: messageData["key"]?.toString() 
                ?: ""
            
            // Trigger haptic feedback on long-press
            holder.itemView.performHapticFeedback(
                android.view.HapticFeedbackConstants.LONG_PRESS
            )
            
            // Enter multi-select mode if not already in it
            if (!isMultiSelectMode) {
                onEnterMultiSelectMode?.invoke(messageId)
                true
            } else {
                // Call listener for message actions if already in multi-select mode
                listener.onMessageLongClick(messageId, position)
                true
            }
        }
    }

    /**
     * Check if a message type supports grouping
     * Excludes typing, error, and loading indicators from grouping
     */
    private fun isGroupableMessageType(viewType: Int): Boolean {
        return when (viewType) {
            VIEW_TYPE_TEXT,
            VIEW_TYPE_MEDIA_GRID,
            VIEW_TYPE_VIDEO,
            VIEW_TYPE_VOICE_MESSAGE,
            VIEW_TYPE_LINK_PREVIEW -> true
            VIEW_TYPE_TYPING,
            VIEW_TYPE_ERROR,
            VIEW_TYPE_LOADING_MORE -> false
            else -> false
        }
    }

    /**
     * Check if current message should group with previous message
     * Considers sender ID, message type, and deleted status
     */
    private fun shouldGroupWithPrevious(currentPosition: Int): Boolean {
        // Validate bounds
        if (currentPosition <= 0 || currentPosition >= data.size) {
            return false
        }
        
        val previousPosition = currentPosition - 1
        val currentMessage = data[currentPosition]
        val previousMessage = data[previousPosition]
        
        // Check if current message is groupable
        val currentViewType = getItemViewType(currentPosition)
        if (!isGroupableMessageType(currentViewType)) {
            return false
        }
        
        // Check if previous message is groupable
        val previousViewType = getItemViewType(previousPosition)
        if (!isGroupableMessageType(previousViewType)) {
            return false
        }
        
        // Check if current message is deleted
        val currentDeleted = currentMessage["is_deleted"]?.toString()?.toBooleanStrictOrNull() ?: false
        val currentDeleteForEveryone = currentMessage["delete_for_everyone"]?.toString()?.toBooleanStrictOrNull() ?: false
        if (currentDeleted || currentDeleteForEveryone) {
            return false
        }
        
        // Check if previous message is deleted
        val previousDeleted = previousMessage["is_deleted"]?.toString()?.toBooleanStrictOrNull() ?: false
        val previousDeleteForEveryone = previousMessage["delete_for_everyone"]?.toString()?.toBooleanStrictOrNull() ?: false
        if (previousDeleted || previousDeleteForEveryone) {
            return false
        }
        
        // Check if same sender
        val currentSenderId = currentMessage["sender_id"]?.toString() 
            ?: currentMessage["uid"]?.toString() 
            ?: ""
        val previousSenderId = previousMessage["sender_id"]?.toString() 
            ?: previousMessage["uid"]?.toString() 
            ?: ""
        
        return currentSenderId == previousSenderId && currentSenderId.isNotEmpty()
    }

    /**
     * Check if current message should group with next message
     * Considers sender ID, message type, and deleted status
     */
    private fun shouldGroupWithNext(currentPosition: Int): Boolean {
        // Validate bounds
        if (currentPosition < 0 || currentPosition >= data.size - 1) {
            return false
        }
        
        val nextPosition = currentPosition + 1
        val currentMessage = data[currentPosition]
        val nextMessage = data[nextPosition]
        
        // Check if current message is groupable
        val currentViewType = getItemViewType(currentPosition)
        if (!isGroupableMessageType(currentViewType)) {
            return false
        }
        
        // Check if next message is groupable
        val nextViewType = getItemViewType(nextPosition)
        if (!isGroupableMessageType(nextViewType)) {
            return false
        }
        
        // Check if current message is deleted
        val currentDeleted = currentMessage["is_deleted"]?.toString()?.toBooleanStrictOrNull() ?: false
        val currentDeleteForEveryone = currentMessage["delete_for_everyone"]?.toString()?.toBooleanStrictOrNull() ?: false
        if (currentDeleted || currentDeleteForEveryone) {
            return false
        }
        
        // Check if next message is deleted
        val nextDeleted = nextMessage["is_deleted"]?.toString()?.toBooleanStrictOrNull() ?: false
        val nextDeleteForEveryone = nextMessage["delete_for_everyone"]?.toString()?.toBooleanStrictOrNull() ?: false
        if (nextDeleted || nextDeleteForEveryone) {
            return false
        }
        
        // Check if same sender
        val currentSenderId = currentMessage["sender_id"]?.toString() 
            ?: currentMessage["uid"]?.toString() 
            ?: ""
        val nextSenderId = nextMessage["sender_id"]?.toString() 
            ?: nextMessage["uid"]?.toString() 
            ?: ""
        
        return currentSenderId == nextSenderId && currentSenderId.isNotEmpty()
    }

    /**
     * Calculate the position of a message within its group
     * Returns SINGLE, FIRST, MIDDLE, or LAST based on adjacent messages
     */
    private fun calculateMessagePosition(position: Int): MessagePosition {
        // Validate position
        if (position < 0 || position >= data.size) {
            return MessagePosition.SINGLE
        }
        
        val canGroupWithPrevious = shouldGroupWithPrevious(position)
        val canGroupWithNext = shouldGroupWithNext(position)
        
        return when {
            !canGroupWithPrevious && !canGroupWithNext -> MessagePosition.SINGLE
            !canGroupWithPrevious && canGroupWithNext -> MessagePosition.FIRST
            canGroupWithPrevious && canGroupWithNext -> MessagePosition.MIDDLE
            canGroupWithPrevious && !canGroupWithNext -> MessagePosition.LAST
            else -> MessagePosition.SINGLE // Fallback
        }
    }

    /**
     * Apply message bubble background based on message position and direction
     * Maps MessagePosition to appropriate drawable resources
     */
    private fun applyMessageBubbleBackground(
        holder: BaseMessageViewHolder,
        messagePosition: MessagePosition,
        isMyMessage: Boolean
    ) {
        holder.messageBubble?.let { bubble ->
            val drawableRes = when (messagePosition) {
                MessagePosition.SINGLE -> {
                    if (isMyMessage) {
                        R.drawable.shape_outgoing_message_single
                    } else {
                        R.drawable.shape_incoming_message_single
                    }
                }
                MessagePosition.FIRST -> {
                    if (isMyMessage) {
                        R.drawable.shape_outgoing_message_first
                    } else {
                        R.drawable.shape_incoming_message_first
                    }
                }
                MessagePosition.MIDDLE -> {
                    if (isMyMessage) {
                        R.drawable.shape_outgoing_message_middle
                    } else {
                        R.drawable.shape_incoming_message_middle
                    }
                }
                MessagePosition.LAST -> {
                    if (isMyMessage) {
                        R.drawable.shape_outgoing_message_last
                    } else {
                        R.drawable.shape_incoming_message_last
                    }
                }
            }
            bubble.setBackgroundResource(drawableRes)
        }
    }

    /**
     * Calculate time difference in milliseconds between two messages
     * Handles null timestamp values with fallback to current time
     * 
     * @param position1 Position of first message
     * @param position2 Position of second message
     * @return Time difference in milliseconds, or Long.MAX_VALUE if either message is invalid
     */
    private fun getTimeDifference(position1: Int, position2: Int): Long {
        // Validate positions are within bounds
        if (position1 < 0 || position1 >= data.size || position2 < 0 || position2 >= data.size) {
            return Long.MAX_VALUE
        }
        
        val message1 = data[position1]
        val message2 = data[position2]
        
        // Get timestamps with fallback to current time
        val timestamp1 = message1["created_at"]?.toString()?.toLongOrNull()
            ?: message1["push_date"]?.toString()?.toLongOrNull()
            ?: System.currentTimeMillis()
        
        val timestamp2 = message2["created_at"]?.toString()?.toLongOrNull()
            ?: message2["push_date"]?.toString()?.toLongOrNull()
            ?: System.currentTimeMillis()
        
        return kotlin.math.abs(timestamp2 - timestamp1)
    }

    /**
     * Determine if timestamp should be shown for a message
     * Implements 60-second threshold logic for grouped messages
     * 
     * @param position Position of the message in the data list
     * @param messagePosition Position of message within its group (SINGLE, FIRST, MIDDLE, LAST)
     * @return true if timestamp should be displayed, false otherwise
     */
    private fun shouldShowTimestamp(position: Int, messagePosition: MessagePosition): Boolean {
        // Validate position is within bounds
        if (position < 0 || position >= data.size) {
            return true // Show timestamp by default for invalid positions
        }
        
        return when (messagePosition) {
            // SINGLE and LAST messages always show timestamp
            MessagePosition.SINGLE, MessagePosition.LAST -> true
            
            // FIRST and MIDDLE messages show timestamp only if time difference with next message > 60 seconds
            MessagePosition.FIRST, MessagePosition.MIDDLE -> {
                val nextPosition = position + 1
                if (nextPosition >= data.size) {
                    // No next message, show timestamp
                    true
                } else {
                    // Check time difference with next message
                    val timeDiff = getTimeDifference(position, nextPosition)
                    timeDiff > 60000 // 60 seconds = 60000 milliseconds
                }
            }
        }
    }

    /**
     * Determine if username should be shown for a message in group chats
     * Username is shown only for SINGLE or FIRST messages in group chats
     * 
     * @param position Position of the message in the data list
     * @param messagePosition Position of message within its group (SINGLE, FIRST, MIDDLE, LAST)
     * @param isGroupChat Whether the conversation is a group chat
     * @param isMyMessage Whether the message is from the current user
     * @return true if username should be displayed, false otherwise
     */
    private fun shouldShowUsername(
        position: Int,
        messagePosition: MessagePosition,
        isGroupChat: Boolean,
        isMyMessage: Boolean
    ): Boolean {
        // Never show username for current user's messages
        if (isMyMessage) {
            return false
        }
        
        // Never show username in 1-on-1 chats
        if (!isGroupChat) {
            return false
        }
        
        // In group chats, show username only for SINGLE or FIRST message positions
        return when (messagePosition) {
            MessagePosition.SINGLE, MessagePosition.FIRST -> true
            MessagePosition.MIDDLE, MessagePosition.LAST -> false
        }
    }

    private fun bindTextViewHolder(holder: TextViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        val msgUid = messageData["sender_id"]?.toString() 
            ?: messageData["uid"]?.toString() 
            ?: ""
        val isMyMessage = msgUid == myUid
        
        // Check if AI summary is being shown
        val showingSummary = messageData["showing_ai_summary"]?.toString()?.toBooleanStrictOrNull() ?: false
        val aiSummary = messageData["ai_summary"]?.toString()
        
        // Support both content (Supabase) and message_text (legacy) field names
        val originalText = messageData["content"]?.toString() 
            ?: messageData["message_text"]?.toString() 
            ?: ""
        
        // Display summary or original text
        val displayText = if (showingSummary && !aiSummary.isNullOrEmpty()) {
            aiSummary
        } else {
            originalText
        }
        holder.messageText.text = displayText
        
        // Apply text color based on message type
        val context = holder.itemView.context
        holder.messageText.setTextColor(
            if (isMyMessage) {
                context.getColor(R.color.md_theme_onPrimaryContainer)
            } else {
                context.getColor(R.color.md_theme_onSecondaryContainer)
            }
        )
    }

    private fun bindMediaViewHolder(holder: MediaViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        val msgUid = messageData["sender_id"]?.toString() 
            ?: messageData["uid"]?.toString() 
            ?: ""
        val isMyMessage = msgUid == myUid
        val attachments = messageData["attachments"] as? ArrayList<HashMap<String, Any?>>
        
        holder.mediaGrid.removeAllViews()
        
        attachments?.forEach { attachment ->
            val imageView = ImageView(context)
            val imageUrl = attachment["url"]?.toString()
            
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(context!!)
                    .load(imageUrl)
                    .transform(RoundedCorners(16))
                    .into(imageView)
                
                imageView.setOnClickListener {
                    listener.onAttachmentClick(imageUrl, attachment["type"]?.toString() ?: "image")
                }
            }
            
            holder.mediaGrid.addView(imageView)
        }
        
        // Support both content and message_text for caption
        val caption = messageData["content"]?.toString() 
            ?: messageData["message_text"]?.toString() 
            ?: ""
        holder.mediaCaption?.text = caption
        
        // Apply text color to caption based on message type
        holder.mediaCaption?.let { captionView ->
            val context = holder.itemView.context
            captionView.setTextColor(
                if (isMyMessage) {
                    context.getColor(R.color.md_theme_onPrimaryContainer)
                } else {
                    context.getColor(R.color.md_theme_onSecondaryContainer)
                }
            )
        }
    }

    private fun bindVideoViewHolder(holder: VideoViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        val msgUid = messageData["sender_id"]?.toString() 
            ?: messageData["uid"]?.toString() 
            ?: ""
        val isMyMessage = msgUid == myUid
        val attachments = messageData["attachments"] as? ArrayList<HashMap<String, Any?>>
        val videoAttachment = attachments?.firstOrNull()
        
        val thumbnailUrl = videoAttachment?.get("thumbnailUrl")?.toString()
        val videoUrl = videoAttachment?.get("url")?.toString()
        
        if (!thumbnailUrl.isNullOrEmpty()) {
            Glide.with(context!!)
                .load(thumbnailUrl)
                .transform(RoundedCorners(16))
                .into(holder.videoThumbnail)
        }
        
        holder.playButton.setOnClickListener {
            if (!videoUrl.isNullOrEmpty()) {
                listener.onAttachmentClick(videoUrl, "video")
            }
        }
        
        // Support both content and message_text for caption
        val caption = messageData["content"]?.toString() 
            ?: messageData["message_text"]?.toString() 
            ?: ""
        holder.videoCaption?.text = caption
        
        // Apply text color to caption based on message type
        holder.videoCaption?.let { captionView ->
            val context = holder.itemView.context
            captionView.setTextColor(
                if (isMyMessage) {
                    context.getColor(R.color.md_theme_onPrimaryContainer)
                } else {
                    context.getColor(R.color.md_theme_onSecondaryContainer)
                }
            )
        }
    }

    private fun bindTypingViewHolder(holder: TypingViewHolder, position: Int) {
        val messageData = data[position]
        
        // Handle legacy Lottie animation
        holder.typingAnimation.playAnimation()
        
        // Handle new TypingIndicatorView if available
        holder.typingIndicatorView?.let { typingView ->
            // Get typing users from message data
            val typingUsers = messageData["typingUsers"] as? List<String> ?: emptyList()
            val displayNames = messageData["displayNames"] as? Map<String, String> ?: emptyMap()
            
            if (typingUsers.isNotEmpty()) {
                typingView.updateTypingUsers(typingUsers, displayNames)
            } else {
                // Fallback to generic typing message
                typingView.setCustomTypingMessage("Someone is typing...")
            }
        }
    }

    private fun bindLinkPreviewViewHolder(holder: LinkPreviewViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        val msgUid = messageData["sender_id"]?.toString() 
            ?: messageData["uid"]?.toString() 
            ?: ""
        val isMyMessage = msgUid == myUid
        
        // Support both content and message_text field names
        val messageText = messageData["content"]?.toString() 
            ?: messageData["message_text"]?.toString() 
            ?: ""
        
        holder.messageText.text = messageText
        
        // Apply text color based on message type
        val context = holder.itemView.context
        holder.messageText.setTextColor(
            if (isMyMessage) {
                context.getColor(R.color.md_theme_onPrimaryContainer)
            } else {
                context.getColor(R.color.md_theme_onSecondaryContainer)
            }
        )
        
        val url = LinkPreviewUtil.extractUrl(messageText)
        if (url != null) {
            holder.linkPreviewCard.visibility = View.VISIBLE
            
            // Set click listener to open URL
            holder.linkPreviewCard.setOnClickListener {
                listener.onAttachmentClick(url, "link")
            }
            
            // Check cache first
            if (linkPreviewCache.containsKey(url)) {
                val cachedData = linkPreviewCache[url]!!
                displayLinkPreview(holder, cachedData)
            } else {
                // Show loading state with basic info
                holder.linkTitle.text = "Loading..."
                holder.linkDescription.text = url
                holder.linkUrl.text = LinkPreviewUtil.extractDomain(url) ?: url
                holder.linkImage.visibility = View.GONE
                
                // Fetch metadata asynchronously
                CoroutineScope(Dispatchers.Main).launch {
                    val result = LinkPreviewUtil.fetchPreview(url)
                    result.onSuccess { linkData ->
                        // Cache the result
                        linkPreviewCache[url] = linkData
                        
                        // Only update if this holder is still showing the same URL
                        if (position < data.size) {
                            val currentMessageText = data[position]["content"]?.toString() 
                                ?: data[position]["message_text"]?.toString() 
                                ?: ""
                            val currentUrl = LinkPreviewUtil.extractUrl(currentMessageText)
                            if (currentUrl == url) {
                                displayLinkPreview(holder, linkData)
                            }
                        }
                    }.onFailure {
                        // On error, show basic preview
                        holder.linkTitle.text = LinkPreviewUtil.extractDomain(url) ?: "Link"
                        holder.linkDescription.text = url
                        holder.linkUrl.text = LinkPreviewUtil.extractDomain(url) ?: url
                        holder.linkImage.visibility = View.GONE
                    }
                }
            }
        } else {
            holder.linkPreviewCard.visibility = View.GONE
        }
    }
    
    private fun displayLinkPreview(holder: LinkPreviewViewHolder, linkData: LinkPreviewUtil.LinkData) {
        holder.linkTitle.text = linkData.title ?: linkData.domain ?: "Link"
        holder.linkDescription.text = linkData.description ?: linkData.url
        holder.linkUrl.text = linkData.domain ?: linkData.url
        
        // Load and display image if available
        if (!linkData.imageUrl.isNullOrEmpty()) {
            holder.linkImage.visibility = View.VISIBLE
            context?.let { ctx ->
                Glide.with(ctx)
                    .load(linkData.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ph_imgbluredsqure)
                    .error(R.drawable.ph_imgbluredsqure)
                    .into(holder.linkImage)
            }
        } else {
            holder.linkImage.visibility = View.GONE
        }
    }

    private fun bindVoiceMessageViewHolder(holder: VoiceMessageViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        val attachments = messageData["attachments"] as? ArrayList<HashMap<String, Any?>>
        val audioAttachment = attachments?.firstOrNull()
        val audioUrl = audioAttachment?.get("url")?.toString()
        
        holder.playPauseButton.setOnClickListener {
            if (!audioUrl.isNullOrEmpty()) {
                toggleVoicePlayback(holder, audioUrl)
            }
        }
        
        val duration = audioAttachment?.get("duration")?.toString()?.toLongOrNull() ?: 0L
        holder.duration.text = formatDuration(duration)
    }

    private fun bindErrorViewHolder(holder: ErrorViewHolder, position: Int) {
        val messageData = data[position]
        
        // Display user-friendly error message
        holder.errorMessageText.text = context?.getString(R.string.failed_to_send) ?: "Failed to send"
        holder.retryText.text = context?.getString(R.string.tap_to_retry) ?: "Tap to retry"
        
        // Log full error details for debugging (never display in UI)
        val errorDetails = messageData["error"]?.toString()
        val exception = messageData["exception"] as? Exception
        val messageId = messageData["id"]?.toString() 
            ?: messageData["key"]?.toString() 
            ?: "unknown"
        
        Log.e(TAG, "Message send failed for message ID: $messageId. Error: $errorDetails", exception)
        
        // Set message time
        holder.messageTime?.let { timeView ->
            val timestamp = messageData["created_at"]?.toString()?.toLongOrNull()
                ?: messageData["push_date"]?.toString()?.toLongOrNull() 
                ?: System.currentTimeMillis()
            timeView.text = formatMessageTime(timestamp)
        }
        
        // Set message layout alignment (error messages are always from current user)
        holder.messageLayout?.let { layout ->
            val layoutParams = layout.layoutParams as? LinearLayout.LayoutParams
            if (layoutParams != null) {
                layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                layoutParams.gravity = Gravity.END
                layout.layoutParams = layoutParams
            }
        }
        
        // Implement retry click listener
        holder.itemView.setOnClickListener {
            listener.onMessageRetry(messageId, position)
        }
        
        // Also allow long click for message options
        holder.itemView.setOnLongClickListener {
            // Enter multi-select mode if not already in it
            if (!isMultiSelectMode) {
                onEnterMultiSelectMode?.invoke(messageId)
                true
            } else {
                // Call listener for message actions if already in multi-select mode
                listener.onMessageLongClick(messageId, position)
                true
            }
        }
    }

    private fun bindLoadingViewHolder(holder: LoadingViewHolder, position: Int) {
        // Loading is handled by progress bar only
    }

    private fun toggleVoicePlayback(holder: VoiceMessageViewHolder, audioUrl: String) {
        try {
            if (holder.mediaPlayer == null) {
                holder.mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    prepareAsync()
                    setOnPreparedListener { player ->
                        player.start()
                        holder.playPauseButton.setImageResource(R.drawable.ic_close)
                        startProgressUpdate(holder)
                    }
                    setOnCompletionListener {
                        holder.playPauseButton.setImageResource(R.drawable.ic_play_circle_filled)
                        holder.waveform.progress = 0
                    }
                }
            } else {
                if (holder.mediaPlayer!!.isPlaying) {
                    holder.mediaPlayer!!.pause()
                    holder.playPauseButton.setImageResource(R.drawable.ic_play_circle_filled)
                } else {
                    holder.mediaPlayer!!.start()
                    holder.playPauseButton.setImageResource(R.drawable.ic_close)
                    startProgressUpdate(holder)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing voice message", e)
        }
    }

    private fun startProgressUpdate(holder: VoiceMessageViewHolder) {
        holder.handler = Handler(Looper.getMainLooper())
        val updateProgress = object : Runnable {
            override fun run() {
                holder.mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val progress = (player.currentPosition * 100) / player.duration
                        holder.waveform.progress = progress
                        holder.handler?.postDelayed(this, 100)
                    }
                }
            }
        }
        holder.handler?.post(updateProgress)
    }

    private fun formatMessageTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        
        val now = Calendar.getInstance()
        
        return if (isSameDay(calendar, now)) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else if (isYesterday(calendar, now)) {
            "Yesterday ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        } else {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(messageTime: Calendar, now: Calendar): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.timeInMillis = now.timeInMillis
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        
        return isSameDay(messageTime, yesterday)
    }

    /**
     * Update a message with fade animation (for edited messages)
     */
    fun updateMessageWithAnimation(position: Int, newMessageData: HashMap<String, Any?>) {
        if (position >= 0 && position < data.size) {
            data[position] = newMessageData
            notifyItemChanged(position)
        }
    }

    /**
     * Remove a message with slide-out animation (for deleted messages)
     */
    fun removeMessageWithAnimation(position: Int, recyclerView: RecyclerView) {
        if (position >= 0 && position < data.size) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
            viewHolder?.itemView?.let { view ->
                val currentUser = authService.getCurrentUser()
                val myUid = currentUser?.id ?: ""
                val messageData = data[position]
                val msgUid = messageData["sender_id"]?.toString() 
                    ?: messageData["uid"]?.toString() 
                    ?: ""
                val isMyMessage = msgUid == myUid
                
                MessageAnimations.applyDeletedMessageAnimation(view, isMyMessage) {
                    data.removeAt(position)
                    notifyItemRemoved(position)
                }
            } ?: run {
                // If view is not visible, remove without animation
                data.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    /**
     * Scroll to a message and highlight it
     */
    fun scrollToMessageWithHighlight(recyclerView: RecyclerView, position: Int) {
        MessageAnimations.scrollToMessageWithHighlight(recyclerView, position)
    }

    /**
     * Update message state for real-time read receipts
     * This method efficiently updates only the message state without full item refresh
     */
    fun updateMessageState(messageId: String, newState: String) {
        val position = data.indexOfFirst { messageData ->
            val id = messageData["id"]?.toString() ?: messageData["key"]?.toString() ?: ""
            id == messageId
        }
        
        if (position != -1) {
            val messageData = data[position]
            val oldState = messageData["delivery_status"]?.toString() 
                ?: messageData["message_state"]?.toString() 
                ?: "sent"
            
            // Only update if state actually changed
            if (oldState != newState) {
                // Update the data
                messageData["message_state"] = newState
                messageData["delivery_status"] = newState
                
                // Set timestamps based on state
                when (newState) {
                    "delivered" -> {
                        if (messageData["delivered_at"] == null) {
                            messageData["delivered_at"] = System.currentTimeMillis()
                        }
                    }
                    "read" -> {
                        if (messageData["read_at"] == null) {
                            messageData["read_at"] = System.currentTimeMillis()
                        }
                        if (messageData["delivered_at"] == null) {
                            messageData["delivered_at"] = System.currentTimeMillis()
                        }
                    }
                }
                
                // Notify only the specific item changed with payload for partial update
                notifyItemChanged(position, "message_state_update")
            }
        }
    }

    /**
     * Update multiple message states in batch for efficiency
     */
    fun updateMessageStates(messageStates: Map<String, String>) {
        val updatedPositions = mutableListOf<Int>()
        
        messageStates.forEach { (messageId, newState) ->
            val position = data.indexOfFirst { messageData ->
                val id = messageData["id"]?.toString() ?: messageData["key"]?.toString() ?: ""
                id == messageId
            }
            
            if (position != -1) {
                val messageData = data[position]
                val oldState = messageData["delivery_status"]?.toString() 
                    ?: messageData["message_state"]?.toString() 
                    ?: "sent"
                
                if (oldState != newState) {
                    messageData["message_state"] = newState
                    messageData["delivery_status"] = newState
                    
                    // Set timestamps based on state
                    when (newState) {
                        "delivered" -> {
                            if (messageData["delivered_at"] == null) {
                                messageData["delivered_at"] = System.currentTimeMillis()
                            }
                        }
                        "read" -> {
                            if (messageData["read_at"] == null) {
                                messageData["read_at"] = System.currentTimeMillis()
                            }
                            if (messageData["delivered_at"] == null) {
                                messageData["delivered_at"] = System.currentTimeMillis()
                            }
                        }
                    }
                    
                    updatedPositions.add(position)
                }
            }
        }
        
        // Batch notify all changed positions
        updatedPositions.forEach { position ->
            notifyItemChanged(position, "message_state_update")
        }
    }

    /**
     * Add loading indicator at the top of the chat for loading older messages
     */
    fun addLoadingIndicator() {
        // Check if loading indicator already exists
        if (data.isNotEmpty() && data[0].containsKey("isLoadingMore")) {
            return
        }
        
        // Add loading indicator at position 0
        val loadingItem = HashMap<String, Any?>()
        loadingItem["isLoadingMore"] = true
        data.add(0, loadingItem)
        notifyItemInserted(0)
    }

    /**
     * Remove loading indicator from the top of the chat
     */
    fun removeLoadingIndicator() {
        // Find and remove loading indicator
        val loadingPosition = data.indexOfFirst { it.containsKey("isLoadingMore") }
        if (loadingPosition != -1) {
            data.removeAt(loadingPosition)
            notifyItemRemoved(loadingPosition)
        }
    }

    /**
     * Check if loading indicator is currently shown
     */
    fun isLoadingMore(): Boolean {
        return data.isNotEmpty() && data[0].containsKey("isLoadingMore")
    }

    /**
     * Prepend older messages to the chat while preserving scroll position
     * @param olderMessages List of older messages to prepend
     * @param onScrollPositionCalculated Callback to restore scroll position after prepending
     */
    fun prependMessages(
        olderMessages: List<HashMap<String, Any?>>,
        onScrollPositionCalculated: ((itemsAdded: Int) -> Unit)? = null
    ) {
        if (olderMessages.isEmpty()) {
            return
        }
        
        // Remove loading indicator if present
        removeLoadingIndicator()
        
        // Calculate the number of items to add
        val itemsToAdd = olderMessages.size
        
        // Add older messages at the beginning
        data.addAll(0, olderMessages)
        notifyItemRangeInserted(0, itemsToAdd)
        
        // Notify callback with number of items added for scroll position restoration
        onScrollPositionCalculated?.invoke(itemsToAdd)
    }
    
    // Typing indicator management methods
    
    /**
     * Show typing indicator with user information
     */
    fun showTypingIndicator(typingUsers: List<String>, displayNames: Map<String, String> = emptyMap()) {
        // Remove existing typing indicator
        removeTypingIndicator()
        
        // Add new typing indicator
        val typingData = hashMapOf<String, Any?>(
            "typingMessageStatus" to true,
            "typingUsers" to typingUsers,
            "displayNames" to displayNames,
            "id" to "typing_indicator",
            "timestamp" to System.currentTimeMillis()
        )
        
        data.add(typingData)
        notifyItemInserted(data.size - 1)
    }
    
    /**
     * Update typing indicator with new user list
     */
    fun updateTypingIndicator(typingUsers: List<String>, displayNames: Map<String, String> = emptyMap()) {
        val typingIndex = data.indexOfFirst { it.containsKey("typingMessageStatus") }
        if (typingIndex != -1) {
            data[typingIndex]["typingUsers"] = typingUsers
            data[typingIndex]["displayNames"] = displayNames
            notifyItemChanged(typingIndex)
        } else if (typingUsers.isNotEmpty()) {
            showTypingIndicator(typingUsers, displayNames)
        }
    }
    
    /**
     * Remove typing indicator
     */
    fun removeTypingIndicator() {
        val typingIndex = data.indexOfFirst { it.containsKey("typingMessageStatus") }
        if (typingIndex != -1) {
            data.removeAt(typingIndex)
            notifyItemRemoved(typingIndex)
        }
    }
    
    /**
     * Check if typing indicator is currently shown
     */
    fun hasTypingIndicator(): Boolean {
        return data.any { it.containsKey("typingMessageStatus") }
    }
    
}
