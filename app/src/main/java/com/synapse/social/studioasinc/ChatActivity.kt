package com.synapse.social.studioasinc

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
// import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.serialization.json.JsonObject
import com.synapse.social.studioasinc.util.ChatHelper
import com.synapse.social.studioasinc.chat.presentation.MessageActionsViewModel
import com.synapse.social.studioasinc.chat.MessageActionsBottomSheet
import com.synapse.social.studioasinc.chat.DeleteConfirmationDialog
import com.synapse.social.studioasinc.chat.EditMessageDialog
import com.synapse.social.studioasinc.chat.EditHistoryDialog
import com.synapse.social.studioasinc.chat.ForwardMessageDialog
import com.synapse.social.studioasinc.chat.SwipeToReplyCallback
import com.synapse.social.studioasinc.chat.ImageGalleryActivity
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.synapse.social.studioasinc.presentation.viewmodel.ChatViewModel
import com.synapse.social.studioasinc.chat.service.RealtimeState
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatActivity : BaseActivity(), DefaultLifecycleObserver {

    companion object {
        private const val TAG = "ChatActivity"
    }

    // Supabase services
    private val chatService = com.synapse.social.studioasinc.backend.SupabaseChatService()
    private val databaseService = com.synapse.social.studioasinc.backend.SupabaseDatabaseService()
    
    private var synapseLoadingDialog: ProgressDialog? = null
    private var chatId: String? = null
    private var otherUserId: String? = null
    private var isGroup: Boolean = false
    private var replyMessageId: String? = null
    
    private val messagesList = ArrayList<HashMap<String, Any?>>()
    private var otherUserData: Map<String, Any?>? = null
    private var currentUserId: String? = null
    private var chatAdapter: ChatAdapter? = null
    
    // Cache for user-deleted message IDs
    private val userDeletedMessageIds = mutableSetOf<String>()
    
    // ViewModels
    private lateinit var viewModel: MessageActionsViewModel
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var messageDeletionViewModel: com.synapse.social.studioasinc.presentation.viewmodel.MessageDeletionViewModel
    
    // Multi-select components
    private var multiSelectManager: com.synapse.social.studioasinc.chat.MultiSelectManager? = null
    private var messageDeletionCoordinator: com.synapse.social.studioasinc.chat.MessageDeletionCoordinator? = null
    
    // Realtime channel
    private var realtimeChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var realtimeJob: kotlinx.coroutines.Job? = null
    
    // Pagination state
    private var isLoadingMoreMessages = false
    private var hasMoreMessages = true

    // UI Components
    private var recyclerView: RecyclerView? = null
    private var messageInput: EditText? = null
    private var sendButton: MaterialButton? = null
    private var backButton: ImageView? = null
    private var chatNameText: TextView? = null
    private var chatAvatarImage: ImageView? = null
    
    // Message input container
    private var messageInputContainer: LinearLayout? = null
    
    // Reply preview UI components
    private var replyPreviewContainer: androidx.constraintlayout.widget.ConstraintLayout? = null
    private var replyLayout: androidx.constraintlayout.widget.ConstraintLayout? = null
    private var replyUsername: TextView? = null
    private var replyMessage: TextView? = null
    private var replyMediaPreview: ImageView? = null
    private var replyCancelButton: ImageView? = null
    
    // Typing indicator UI components
    private var typingIndicatorView: View? = null
    private var typingText: TextView? = null
    private var typingAnimation: com.synapse.social.studioasinc.widget.TypingAnimationView? = null
    private var typingAvatar: ImageView? = null
    
    // Connection status UI components
    private var connectionStatusBanner: LinearLayout? = null
    private var connectionProgress: ProgressBar? = null
    private var connectionIcon: ImageView? = null
    private var connectionText: TextView? = null
    private var connectionRetryButton: com.google.android.material.button.MaterialButton? = null
    
    // App lifecycle state tracking
    private var isAppInBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onCreate(savedInstanceState)
        Log.d(TAG, "Lifecycle: onCreate")
        setContentView(R.layout.activity_chat)
        
        // Get intent data
        chatId = intent.getStringExtra("chatId")
        otherUserId = intent.getStringExtra("uid")
        isGroup = intent.getBooleanExtra("isGroup", false)
        currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
        
        // Log received intent extras for debugging
        Log.d(TAG, "onCreate - Received intent extras:")
        Log.d(TAG, "  chatId (from 'chatId' extra): $chatId")
        Log.d(TAG, "  otherUserId (from 'uid' extra): $otherUserId")
        Log.d(TAG, "  isGroup: $isGroup")
        Log.d(TAG, "  currentUserId (from auth): $currentUserId")
        
        if (currentUserId == null) {
            Log.e(TAG, "No current user found, finishing activity")
            finish()
            return
        }

        try {
            initialize()
            initializeLogic()
            loadChatData()
            setupRealtimeSubscription()
            
            // Register lifecycle observer for app backgrounding
            lifecycle.addObserver(this)
        } catch (e: Exception) {
            android.util.Log.e("ChatActivity", "Failed to initialize ChatActivity", e)
            Toast.makeText(this, "Failed to initialize chat", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initialize() {
        // Initialize UI components from layout
        try {
            recyclerView = findViewById(R.id.ChatMessagesListRecycler)
            messageInput = findViewById(R.id.etMessageInput)
            sendButton = findViewById(R.id.btnSendMessage)
            backButton = findViewById(R.id.back)
            chatNameText = findViewById(R.id.topProfileLayoutUsername)
            chatAvatarImage = findViewById(R.id.topProfileLayoutProfileImage)
            
            // Initialize message input container
            messageInputContainer = findViewById(R.id.layoutMessageInputContainer)
            
            // Initialize reply preview components
            replyPreviewContainer = findViewById(R.id.layoutReplyPreview)
            replyLayout = findViewById(R.id.layoutReplyPreview)
            replyUsername = findViewById(R.id.tvReplyUsername)
            replyMessage = findViewById(R.id.tvReplyMessage)
            replyMediaPreview = findViewById(R.id.ivReplyMediaPreview)
            replyCancelButton = findViewById(R.id.btnCancelReply)
            
            // Setup RecyclerView with proper configuration
            recyclerView?.apply {
                val linearLayoutManager = LinearLayoutManager(this@ChatActivity).apply {
                    stackFromEnd = true
                }
                layoutManager = linearLayoutManager
                setHasFixedSize(true)
                
                // Setup swipe-to-reply gesture
                setupSwipeToReply(this)
                
                // Setup scroll listener for pagination
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        
                        // Check if user scrolled to the top
                        val firstVisiblePosition = linearLayoutManager.findFirstVisibleItemPosition()
                        if (firstVisiblePosition == 0 && !isLoadingMoreMessages && hasMoreMessages) {
                            loadMoreMessages()
                        }
                    }
                })
            }
            
            // Initialize ChatAdapter with full listener implementation
            val chatAdapter = ChatAdapter(
                data = messagesList,
                repliedMessagesCache = HashMap(),
                listener = object : com.synapse.social.studioasinc.chat.interfaces.ChatAdapterListener {
                    override fun onMessageClick(messageId: String, position: Int) {
                        // Handle message click if needed
                    }
                    
                    override fun onMessageLongClick(messageId: String, position: Int): Boolean {
                        showMessageActionsBottomSheet(messageId, position)
                        return true
                    }
                    
                    override fun onReplyClick(messageId: String, messageText: String, senderName: String) {
                        prepareReply(messageId, messageText, senderName)
                    }
                    
                    override fun onAttachmentClick(attachmentUrl: String, attachmentType: String) {
                        when (attachmentType) {
                            "link" -> openUrl(attachmentUrl)
                            "image" -> openMediaViewer(attachmentUrl, "image")
                            "video" -> openMediaViewer(attachmentUrl, "video")
                            "audio" -> openAudioPlayer(attachmentUrl)
                            "document" -> openDocument(attachmentUrl)
                            else -> openUrl(attachmentUrl)
                        }
                    }
                    
                    override fun onUserProfileClick(userId: String) {
                        openUserProfile(userId)
                    }
                    
                    override fun onMessageRetry(messageId: String, position: Int) {
                        retryFailedMessage(messageId, position)
                    }
                    
                    override fun onReplyAction(messageId: String, messageText: String, senderName: String) {
                        prepareReply(messageId, messageText, senderName)
                    }
                    
                    override fun onForwardAction(messageId: String, messageData: Map<String, Any?>) {
                        showForwardDialog(messageId, messageData)
                    }
                    
                    override fun onEditAction(messageId: String, currentText: String) {
                        showEditDialog(messageId, currentText)
                    }
                    
                    override fun onDeleteAction(messageId: String, deleteForEveryone: Boolean) {
                        showDeleteConfirmation(messageId, deleteForEveryone)
                    }
                    
                    override fun onAISummaryAction(messageId: String, messageText: String) {
                        showAISummary(messageId, messageText)
                    }
                    
                    override fun onEditHistoryClick(messageId: String) {
                        showEditHistory(messageId)
                    }
                }
            )
            recyclerView?.adapter = chatAdapter
            this.chatAdapter = chatAdapter
            
            // Initialize ViewModels FIRST
            viewModel = MessageActionsViewModel(this)
            chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
            messageDeletionViewModel = ViewModelProvider(this)[com.synapse.social.studioasinc.presentation.viewmodel.MessageDeletionViewModel::class.java]
            
            // Initialize managers in ChatViewModel
            chatViewModel.initializeManagers(this)
            
            // Set ChatAdapter reference in ChatViewModel for real-time updates
            chatViewModel.setChatAdapter(chatAdapter)
            
            // Initialize multi-select components
            multiSelectManager = com.synapse.social.studioasinc.chat.MultiSelectManager(this, chatAdapter)
            messageDeletionCoordinator = com.synapse.social.studioasinc.chat.MessageDeletionCoordinator(
                activity = this,
                viewModel = messageDeletionViewModel,
                currentUserId = currentUserId ?: ""
            )
            
            // Wire up adapter callbacks for multi-select
            chatAdapter.onEnterMultiSelectMode = { messageId ->
                multiSelectManager?.enterMultiSelectMode(messageId)
                // Disable animations when entering multi-select mode
                disableRecyclerViewAnimations()
            }
            chatAdapter.onToggleMessageSelection = { messageId ->
                multiSelectManager?.toggleMessageSelection(messageId)
            }
            chatAdapter.isMessageSelected = { messageId ->
                multiSelectManager?.isMessageSelected(messageId) ?: false
            }
            
            // Setup queued messages callback
            multiSelectManager?.onQueuedMessagesReady = { queuedMessages ->
                // Add queued messages to the list
                queuedMessages.forEach { message ->
                    messagesList.add(message)
                }
                chatAdapter.notifyDataSetChanged()
                // Scroll to bottom to show new messages
                if (messagesList.isNotEmpty()) {
                    recyclerView?.scrollToPosition(messagesList.size - 1)
                }
                // Re-enable animations after processing queued messages
                enableRecyclerViewAnimations()
            }
            
            // Setup action toolbar click handlers
            setupActionToolbarHandlers()
            
            // Observe ViewModel state changes
            observeMessageDeletionState()
            
            // Setup real-time message state updates for read receipts
            setupMessageStateUpdates()
            
            // Initialize typing indicator
            initializeTypingIndicator()
            
            // Initialize connection status banner
            initializeConnectionStatusBanner()
            
        } catch (e: Exception) {
            android.util.Log.e("ChatActivity", "UI initialization error: ${e.message}", e)
            Toast.makeText(this, "Failed to initialize chat interface", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeTypingIndicator() {
        // Find the parent container where we can add the typing indicator
        val bottomContainer = findViewById<LinearLayout>(R.id.layoutChatInputRoot)?.parent as? LinearLayout
        
        if (bottomContainer != null) {
            // Create typing indicator view programmatically
            val inflater = layoutInflater
            typingIndicatorView = inflater.inflate(R.layout.typing_indicator_item, bottomContainer, false)
            
            // Initialize typing indicator components
            typingText = typingIndicatorView?.findViewById(R.id.typing_text)
            typingAnimation = typingIndicatorView?.findViewById(R.id.typing_animation)
            typingAvatar = typingIndicatorView?.findViewById(R.id.typing_avatar)
            
            // Initially hide the typing indicator
            typingIndicatorView?.visibility = View.GONE
            
            // Add typing indicator above the message input (before the last child which is message input)
            val messageInputIndex = bottomContainer.childCount - 1
            bottomContainer.addView(typingIndicatorView, messageInputIndex)
            
            // Set up typing indicator observer
            setupTypingIndicatorObserver()
        }
    }

    private fun initializeConnectionStatusBanner() {
        // Initialize connection status banner components
        connectionStatusBanner = findViewById(R.id.connection_status_banner)
        connectionProgress = findViewById(R.id.connection_progress)
        connectionIcon = findViewById(R.id.connection_icon)
        connectionText = findViewById(R.id.connection_text)
        connectionRetryButton = findViewById(R.id.connection_retry_button)
        
        // Initially hide the banner
        connectionStatusBanner?.visibility = View.GONE
        
        // Setup retry button click listener
        connectionRetryButton?.setOnClickListener {
            retryConnection()
        }
        
        // Setup connection state observer
        setupConnectionStateObserver()
    }
    
    /**
     * Setup action toolbar click handlers for multi-select mode
     * Implements navigation icon click to exit multi-select mode
     * and delete menu item click to show deletion dialog
     * 
     * Requirements: 4.3, 4.4, 4.5
     */
    private fun setupActionToolbarHandlers() {
        // Wire up delete action callback to MessageDeletionCoordinator
        multiSelectManager?.onDeleteActionClicked = {
            val selectedMessages = multiSelectManager?.getSelectedMessages() ?: emptyList()
            if (selectedMessages.isNotEmpty()) {
                messageDeletionCoordinator?.initiateDelete(selectedMessages)
            }
        }
    }

    /**
     * Observe MessageDeletionViewModel state changes
     * Handles loading indicator, success state, and error state
     * 
     * Requirements: 6.5, 5.6
     */
    private fun observeMessageDeletionState() {
        // Observe deletion state flow
        lifecycleScope.launch {
            messageDeletionViewModel.deletionState.collect { state ->
                runOnUiThread {
                    when (state) {
                        is com.synapse.social.studioasinc.presentation.viewmodel.DeletionState.Idle -> {
                            // No action needed
                        }
                        is com.synapse.social.studioasinc.presentation.viewmodel.DeletionState.Deleting -> {
                            // Show loading indicator
                            loadingDialog(true)
                        }
                        is com.synapse.social.studioasinc.presentation.viewmodel.DeletionState.Success -> {
                            // Hide loading indicator
                            loadingDialog(false)
                            
                            // Provide haptic feedback for successful deletion
                            provideHapticFeedback()
                            
                            // Exit multi-select mode (this will re-enable animations)
                            multiSelectManager?.exitMultiSelectMode()
                            
                            // Refresh messages to show deletion
                            loadMessages()
                            
                            // Show success message using string resources
                            val messageCount = state.deletedCount
                            val successMessage = if (messageCount == 1) {
                                getString(R.string.success_deletion_single)
                            } else {
                                getString(R.string.success_deletion_multiple, messageCount)
                            }
                            Toast.makeText(this@ChatActivity, successMessage, Toast.LENGTH_SHORT).show()
                            
                            // Reset state
                            messageDeletionViewModel.resetState()
                            
                            // Re-enable animations after deletion completes
                            enableRecyclerViewAnimations()
                        }
                        is com.synapse.social.studioasinc.presentation.viewmodel.DeletionState.Error -> {
                            // Hide loading indicator
                            loadingDialog(false)
                            
                            // Provide haptic feedback for error
                            provideHapticFeedback()
                            
                            // Show error toast
                            Toast.makeText(this@ChatActivity, state.message, Toast.LENGTH_LONG).show()
                            
                            // Reset state
                            messageDeletionViewModel.resetState()
                        }
                    }
                }
            }
        }
        
        // Observe error state flow
        lifecycleScope.launch {
            messageDeletionViewModel.errorState.collect { errorMessage ->
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * Provide haptic feedback to the user
     * Used for deletion success/error and selection changes
     * 
     * Requirements: 5.6
     */
    private fun provideHapticFeedback() {
        try {
            window.decorView.performHapticFeedback(
                android.view.HapticFeedbackConstants.LONG_PRESS,
                android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (e: Exception) {
            // Haptic feedback not available on this device
            android.util.Log.d(TAG, "Haptic feedback not available: ${e.message}")
        }
    }
    
    /**
     * Disable RecyclerView animations during multi-select mode
     * Prevents disruptions during message selection
     * 
     * Requirements: 8.2
     */
    private fun disableRecyclerViewAnimations() {
        recyclerView?.itemAnimator = null
    }
    
    /**
     * Re-enable RecyclerView animations after exiting multi-select mode
     * 
     * Requirements: 8.2
     */
    private fun enableRecyclerViewAnimations() {
        recyclerView?.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
    }
    
    /**
     * Handle incoming message during multi-select mode
     * Queues the message for display after exiting multi-select mode
     * 
     * Requirements: 8.1, 8.3
     * 
     * @param message The incoming message to handle
     */
    private fun handleIncomingMessage(message: HashMap<String, Any?>) {
        if (multiSelectManager?.shouldQueueMessages() == true) {
            // Queue message for later display
            multiSelectManager?.queueMessage(message)
        } else {
            // Add message immediately
            messagesList.add(message)
            chatAdapter?.notifyItemInserted(messagesList.size - 1)
            // Scroll to bottom to show new message
            recyclerView?.scrollToPosition(messagesList.size - 1)
        }
    }

    private fun initializeLogic() {
        // Setup basic functionality
        sendButton?.apply {
            isEnabled = false
            setOnClickListener { sendMessage() }
        }
        
        backButton?.setOnClickListener {
            // Clean up subscriptions when leaving chat
            if (::chatViewModel.isInitialized) {
                chatViewModel.onChatClosed()
            }
            onBackPressedDispatcher.onBackPressed()
        }
        
        // Setup typing indicator
        setupTypingIndicator()
        
        // Start polling for typing indicators from other users
        startTypingIndicatorPolling()
        
        // Setup reply preview observers
        setupReplyPreview()
    }
    
    private fun setupTypingIndicatorObserver() {
        // Check if chatViewModel is initialized
        if (!::chatViewModel.isInitialized) {
            android.util.Log.e("ChatActivity", "setupTypingIndicatorObserver called before chatViewModel initialization")
            return
        }
        
        // Observe typing users from ChatViewModel
        lifecycleScope.launch {
            chatViewModel.typingUsers.collect { typingUsers ->
                runOnUiThread {
                    updateTypingIndicator(typingUsers)
                }
            }
        }
    }

    private fun setupConnectionStateObserver() {
        // Check if chatViewModel is initialized
        if (!::chatViewModel.isInitialized) {
            android.util.Log.e("ChatActivity", "setupConnectionStateObserver called before chatViewModel initialization")
            return
        }
        
        // Observe connection state from realtime service
        lifecycleScope.launch {
            chatViewModel.getRealtimeService()?.connectionState?.collect { state ->
                runOnUiThread {
                    updateConnectionStatusBanner(state)
                }
            }
        }
    }

    private fun loadChatData() {
        lifecycleScope.launch {
            try {
                loadingDialog(true)
                
                // Log received parameters for debugging
                Log.d(TAG, "loadChatData - chatId: $chatId, otherUserId: $otherUserId, currentUserId: $currentUserId")
                
                // Create or get chat if needed
                if (chatId == null && otherUserId != null && currentUserId != null) {
                    Log.d(TAG, "Creating or getting direct chat for currentUser: $currentUserId, otherUser: $otherUserId")
                    // Create or get direct chat
                    val result = chatService.getOrCreateDirectChat(currentUserId!!, otherUserId!!)
                    result.fold(
                        onSuccess = { createdChatId ->
                            Log.d(TAG, "Successfully created/retrieved chat with ID: $createdChatId")
                            chatId = createdChatId
                            loadMessages()
                            loadUserData()
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to create chat", error)
                            showError("Failed to create chat: ${error.message}")
                            loadingDialog(false)
                        }
                    )
                } else if (chatId != null) {
                    Log.d(TAG, "Loading existing chat with ID: $chatId")
                    loadMessages()
                    loadUserData()
                } else {
                    // Log which parameters are missing
                    val missingParams = mutableListOf<String>()
                    if (chatId == null) missingParams.add("chatId")
                    if (otherUserId == null) missingParams.add("otherUserId")
                    if (currentUserId == null) missingParams.add("currentUserId")
                    
                    Log.e(TAG, "Invalid chat configuration - Missing parameters: ${missingParams.joinToString(", ")}")
                    Log.e(TAG, "Intent extras received - chatId: $chatId, otherUserId: $otherUserId")
                    showError("Invalid chat configuration - Missing: ${missingParams.joinToString(", ")}")
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadChatData", e)
                showError("Error: ${e.message}")
                loadingDialog(false)
            }
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            try {
                if (chatId == null) return@launch
                
                // Load user-deleted messages first
                loadUserDeletedMessages()
                
                val result = chatService.getMessages(chatId!!, limit = 50)
                result.fold(
                    onSuccess = { messages ->
                        messagesList.clear()
                        // Messages are already sorted by created_at ascending (oldest first) from the service
                        messages.forEach { message ->
                            val messageMap = HashMap<String, Any?>()
                            messageMap["id"] = message["id"]
                            messageMap["chat_id"] = message["chat_id"]
                            messageMap["sender_id"] = message["sender_id"]
                            messageMap["uid"] = message["sender_id"] // For compatibility
                            messageMap["content"] = message["content"]
                            messageMap["message_text"] = message["content"] // For compatibility
                            messageMap["message_type"] = message["message_type"]
                            messageMap["created_at"] = message["created_at"]
                            messageMap["push_date"] = message["created_at"] // For compatibility
                            messageMap["is_deleted"] = message["is_deleted"]
                            messageMap["is_edited"] = message["is_edited"]
                            messageMap["delete_for_everyone"] = message["delete_for_everyone"]
                            messagesList.add(messageMap)
                        }
                        
                        // Check if there might be more messages
                        hasMoreMessages = messages.size >= 50
                        
                        chatAdapter?.notifyDataSetChanged()
                        if (messagesList.isNotEmpty()) {
                            recyclerView?.scrollToPosition(messagesList.size - 1)
                        }
                        
                        // Mark messages as read
                        if (currentUserId != null) {
                            chatService.markMessagesAsRead(chatId!!, currentUserId!!)
                        }
                        
                        loadingDialog(false)
                    },
                    onFailure = { error ->
                        showError("Failed to load messages: ${error.message}")
                        loadingDialog(false)
                    }
                )
            } catch (e: Exception) {
                showError("Error loading messages: ${e.message}")
                loadingDialog(false)
            }
        }
    }
    
    private fun loadMoreMessages() {
        if (isLoadingMoreMessages || !hasMoreMessages || messagesList.isEmpty()) return
        
        lifecycleScope.launch {
            try {
                isLoadingMoreMessages = true
                
                // Get the timestamp of the oldest message currently loaded
                val oldestMessage = messagesList.firstOrNull() ?: return@launch
                val oldestTimestamp = oldestMessage["created_at"]?.toString()?.toLongOrNull() ?: return@launch
                
                Log.d(TAG, "Loading more messages before timestamp: $oldestTimestamp")
                
                val result = chatService.getMessages(
                    chatId = chatId!!,
                    limit = 50,
                    beforeTimestamp = oldestTimestamp
                )
                
                result.fold(
                    onSuccess = { messages ->
                        if (messages.isEmpty()) {
                            hasMoreMessages = false
                            Log.d(TAG, "No more messages to load")
                        } else {
                            // Remember the current first item position
                            val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager
                            val firstVisiblePosition = layoutManager?.findFirstVisibleItemPosition() ?: 0
                            
                            // Add older messages to the beginning of the list
                            val newMessages = messages.map { message ->
                                HashMap<String, Any?>().apply {
                                    put("id", message["id"])
                                    put("chat_id", message["chat_id"])
                                    put("sender_id", message["sender_id"])
                                    put("uid", message["sender_id"])
                                    put("content", message["content"])
                                    put("message_text", message["content"])
                                    put("message_type", message["message_type"])
                                    put("created_at", message["created_at"])
                                    put("push_date", message["created_at"])
                                    put("is_deleted", message["is_deleted"])
                                    put("is_edited", message["is_edited"])
                                    put("delete_for_everyone", message["delete_for_everyone"])
                                }
                            }
                            
                            messagesList.addAll(0, newMessages)
                            
                            // Check if there might be more messages
                            hasMoreMessages = messages.size >= 50
                            
                            // Notify adapter and maintain scroll position
                            chatAdapter?.notifyItemRangeInserted(0, newMessages.size)
                            
                            // Restore scroll position (add the number of new items to the old position)
                            layoutManager?.scrollToPositionWithOffset(
                                firstVisiblePosition + newMessages.size,
                                0
                            )
                            
                            Log.d(TAG, "Loaded ${newMessages.size} more messages")
                        }
                        
                        isLoadingMoreMessages = false
                    },
                    onFailure = { error ->
                        showError("Failed to load more messages: ${error.message}")
                        isLoadingMoreMessages = false
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more messages: ${e.message}", e)
                showError("Error loading more messages: ${e.message}")
                isLoadingMoreMessages = false
            }
        }
    }
    
    /**
     * Load user-deleted messages from database
     * Stores deleted message IDs in memory for quick lookup during rendering
     * 
     * Requirements: 1.3, 1.4
     */
    private suspend fun loadUserDeletedMessages() {
        if (currentUserId == null) return
        
        try {
            val repository = com.synapse.social.studioasinc.data.repository.MessageDeletionRepository()
            val result = repository.getUserDeletedMessageIds(currentUserId!!, chatId)
            
            result.fold(
                onSuccess = { deletedIds ->
                    userDeletedMessageIds.clear()
                    userDeletedMessageIds.addAll(deletedIds)
                    Log.d(TAG, "Loaded ${deletedIds.size} user-deleted messages")
                    
                    // Update adapter with deleted message IDs
                    runOnUiThread {
                        (chatAdapter as? com.synapse.social.studioasinc.adapters.ChatAdapter)?.userDeletedMessageIds = deletedIds
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load user-deleted messages: ${error.message}")
                    // Don't show error to user, just log it
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user-deleted messages: ${e.message}")
        }
    }
    
    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                if (otherUserId == null) return@launch
                
                val result = databaseService.selectWhere("users", "*", "uid", otherUserId!!)
                result.fold(
                    onSuccess = { users ->
                        otherUserData = users.firstOrNull()
                        updateChatHeader()
                    },
                    onFailure = { error ->
                        android.util.Log.e("ChatActivity", "Failed to load user data: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error loading user data: ${e.message}")
            }
        }
    }
    
    private fun updateChatHeader() {
        val userData = otherUserData ?: return
        
        chatNameText?.text = userData["username"]?.toString() ?: "User"
        
        val avatarUrl = userData["avatar"]?.toString()
            ?.takeIf { it.isNotEmpty() && it != "null" }
        
        if (avatarUrl != null) {
            chatAvatarImage?.let { imageView ->
                Glide.with(this)
                    .load(Uri.parse(avatarUrl))
                    .circleCrop()
                    .placeholder(R.drawable.ic_account_circle_48px)
                    .error(R.drawable.ic_account_circle_48px)
                    .into(imageView)
            }
        }
    }
    
    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadingDialog(show: Boolean) {
        runOnUiThread {
            if (show) {
                if (synapseLoadingDialog == null) {
                    synapseLoadingDialog = ProgressDialog(this).apply {
                        setMessage("Loading...")
                        setCancelable(false)
                    }
                }
                synapseLoadingDialog?.show()
            } else {
                synapseLoadingDialog?.dismiss()
            }
        }
    }

    private fun sendMessage() {
        val messageText = messageInput?.text?.toString()?.trim()
        if (messageText.isNullOrEmpty()) return
        
        if (chatId == null || currentUserId == null) {
            showError("Chat not initialized")
            return
        }
        
        // Generate temporary message ID using timestamp
        val tempId = "temp_${System.currentTimeMillis()}"
        
        // Create optimistic message map with temp ID
        val optimisticMessage = HashMap<String, Any?>()
        optimisticMessage["id"] = tempId
        optimisticMessage["chat_id"] = chatId
        optimisticMessage["sender_id"] = currentUserId
        optimisticMessage["uid"] = currentUserId
        optimisticMessage["content"] = messageText
        optimisticMessage["message_text"] = messageText
        optimisticMessage["message_type"] = "text"
        optimisticMessage["created_at"] = System.currentTimeMillis()
        optimisticMessage["push_date"] = System.currentTimeMillis()
        optimisticMessage["is_deleted"] = false
        optimisticMessage["is_edited"] = false
        optimisticMessage["delivery_status"] = "sending"
        optimisticMessage["is_optimistic"] = true
        optimisticMessage["temp_id"] = tempId
        
        // Include reply reference if present
        val currentReplyId = replyMessageId
        if (currentReplyId != null) {
            optimisticMessage["replied_message_id"] = currentReplyId
        }
        
        // Add optimistic message to messagesList immediately
        messagesList.add(optimisticMessage)
        
        // Clear input field immediately after adding to list
        messageInput?.text?.clear()
        
        // Clear reply preview after message sent
        cancelReply()
        
        // Notify adapter of new message insertion
        chatAdapter?.notifyItemInserted(messagesList.size - 1)
        
        // Scroll to bottom to show new message
        recyclerView?.scrollToPosition(messagesList.size - 1)
        
        // Disable send button to prevent double-sending
        sendButton?.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val result = chatService.sendMessage(
                    chatId = chatId!!,
                    senderId = currentUserId!!,
                    content = messageText,
                    messageType = "text",
                    replyToId = currentReplyId
                )
                
                result.fold(
                    onSuccess = { messageId ->
                        // Find message by temp ID in messagesList
                        val position = messagesList.indexOfFirst { 
                            it["temp_id"]?.toString() == tempId 
                        }
                        
                        if (position != -1) {
                            // Update message with real ID from server
                            val message = messagesList[position]
                            message["id"] = messageId
                            message["delivery_status"] = "sent"
                            message["is_optimistic"] = false
                            message.remove("temp_id")
                            
                            // Notify adapter of item change
                            runOnUiThread {
                                chatAdapter?.notifyItemChanged(position)
                            }
                        }
                    },
                    onFailure = { error ->
                        // Find message by temp ID in messagesList
                        val position = messagesList.indexOfFirst { 
                            it["temp_id"]?.toString() == tempId 
                        }
                        
                        if (position != -1) {
                            // Update message delivery status to "failed"
                            val message = messagesList[position]
                            message["delivery_status"] = "failed"
                            message["is_optimistic"] = false
                            
                            // Notify adapter of item change
                            runOnUiThread {
                                chatAdapter?.notifyItemChanged(position)
                            }
                        }
                        
                        showError("Failed to send message: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                // Find message by temp ID in messagesList
                val position = messagesList.indexOfFirst { 
                    it["temp_id"]?.toString() == tempId 
                }
                
                if (position != -1) {
                    // Update message delivery status to "failed"
                    val message = messagesList[position]
                    message["delivery_status"] = "failed"
                    message["is_optimistic"] = false
                    
                    // Notify adapter of item change
                    runOnUiThread {
                        chatAdapter?.notifyItemChanged(position)
                    }
                }
                
                showError("Error sending message: ${e.message}")
            } finally {
                // Re-enable send button
                runOnUiThread {
                    sendButton?.isEnabled = true
                }
            }
        }
    }
    
    /**
     * Retry sending a failed message
     * 
     * @param messageId The ID of the failed message (temp ID)
     * @param position The position of the message in the list
     */
    private fun retryFailedMessage(messageId: String, position: Int) {
        val message = messagesList.getOrNull(position) ?: return
        
        // Verify this is a failed message
        if (message["delivery_status"] != "failed") {
            return
        }
        
        val messageText = message["content"]?.toString() ?: return
        val replyToId = message["replied_message_id"]?.toString()
        
        // Update status to sending
        message["delivery_status"] = "sending"
        chatAdapter?.notifyItemChanged(position)
        
        lifecycleScope.launch {
            try {
                val result = chatService.sendMessage(
                    chatId = chatId!!,
                    senderId = currentUserId!!,
                    content = messageText,
                    messageType = "text",
                    replyToId = replyToId
                )
                
                result.fold(
                    onSuccess = { newMessageId ->
                        // Update message with real ID from server
                        message["id"] = newMessageId
                        message["delivery_status"] = "sent"
                        message["is_optimistic"] = false
                        message.remove("temp_id")
                        
                        // Notify adapter of item change
                        runOnUiThread {
                            chatAdapter?.notifyItemChanged(position)
                        }
                    },
                    onFailure = { error ->
                        // Update message delivery status back to "failed"
                        message["delivery_status"] = "failed"
                        
                        // Notify adapter of item change
                        runOnUiThread {
                            chatAdapter?.notifyItemChanged(position)
                        }
                        
                        showError("Failed to send message: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                // Update message delivery status back to "failed"
                message["delivery_status"] = "failed"
                
                // Notify adapter of item change
                runOnUiThread {
                    chatAdapter?.notifyItemChanged(position)
                }
                
                showError("Error sending message: ${e.message}")
            }
        }
    }
    
    /**
     * Setup typing indicator
     */
    private fun setupTypingIndicator() {
        var typingJob: kotlinx.coroutines.Job? = null
        
        messageInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel previous typing job
                typingJob?.cancel()
                
                // Send typing status through ChatViewModel (only if app is not in background)
                if (!isAppInBackground && ::chatViewModel.isInitialized) {
                    chatViewModel.onUserTyping(s?.toString() ?: "")
                }
            }
            override fun afterTextChanged(s: Editable?) {
                sendButton?.isEnabled = !s.isNullOrEmpty() && s.isNotBlank()
                
                // Stop typing when input is cleared (handled by ChatViewModel)
            }
        })
    }
    
    /**
     * Start polling for typing indicators
     */
    private fun startTypingIndicatorPolling() {
        // This method is now replaced by setupTypingIndicatorObserver()
        // Keep for backward compatibility but it's no longer used
    }
    
    /**
     * Update typing indicator UI based on typing users list
     * 
     * @param typingUsers List of user IDs who are currently typing
     */
    private fun updateTypingIndicator(typingUsers: List<String>) {
        if (typingUsers.isEmpty()) {
            hideTypingIndicator()
        } else {
            showTypingIndicator(typingUsers)
        }
    }
    
    // Auto-hide job for typing indicator
    private var typingIndicatorAutoHideJob: kotlinx.coroutines.Job? = null
    
    /**
     * Show typing indicator with appropriate text and animation
     * 
     * @param typingUsers List of user IDs who are currently typing
     */
    private fun showTypingIndicator(typingUsers: List<String>) {
        typingIndicatorView?.let { view ->
            // Generate typing text based on number of users
            val typingMessage = generateTypingMessage(typingUsers)
            typingText?.text = typingMessage
            
            // Load avatar for single user typing
            if (typingUsers.size == 1 && otherUserData != null) {
                val avatarUrl = otherUserData?.get("avatar")?.toString()
                    ?.takeIf { it.isNotEmpty() && it != "null" }
                
                if (avatarUrl != null) {
                    typingAvatar?.let { imageView ->
                        Glide.with(this)
                            .load(Uri.parse(avatarUrl))
                            .circleCrop()
                            .placeholder(R.drawable.ic_account_circle_48px)
                            .error(R.drawable.ic_account_circle_48px)
                            .into(imageView)
                    }
                }
            }
            
            // Show with fade-in animation if currently hidden
            if (view.visibility != View.VISIBLE) {
                view.visibility = View.VISIBLE
                view.alpha = 0f
                view.animate()
                    .alpha(1f)
                    .setDuration(200) // 200ms fade-in as required
                    .withStartAction {
                        // Start typing animation when fade-in begins
                        typingAnimation?.startAnimation()
                    }
                    .start()
            } else {
                // If already visible, just update the text and ensure animation is running
                typingAnimation?.startAnimation()
            }
            
            // Cancel previous auto-hide job and start new one
            typingIndicatorAutoHideJob?.cancel()
            typingIndicatorAutoHideJob = lifecycleScope.launch {
                kotlinx.coroutines.delay(5000) // Auto-hide after 5 seconds
                runOnUiThread {
                    hideTypingIndicator()
                }
            }
        }
    }
    
    /**
     * Hide typing indicator with fade-out animation
     */
    private fun hideTypingIndicator() {
        typingIndicatorView?.let { view ->
            if (view.visibility == View.VISIBLE) {
                view.animate()
                    .alpha(0f)
                    .setDuration(200) // 200ms fade-out as required
                    .withStartAction {
                        // Stop typing animation when fade-out begins
                        typingAnimation?.stopAnimation()
                    }
                    .withEndAction {
                        view.visibility = View.GONE
                    }
                    .start()
            }
        }
    }
    
    /**
     * Generate appropriate typing message based on number of users
     * 
     * @param typingUsers List of user IDs who are currently typing
     * @return Formatted typing message string
     */
    private fun generateTypingMessage(typingUsers: List<String>): String {
        return when (typingUsers.size) {
            0 -> ""
            1 -> {
                val username = otherUserData?.get("username")?.toString() ?: "User"
                "$username is typing..."
            }
            2 -> {
                // For now, we'll use generic text since we only have one other user's data
                // In a group chat, this would need to fetch multiple user names
                "2 people are typing..."
            }
            else -> {
                "${typingUsers.size} people are typing..."
            }
        }
    }

    /**
     * Update connection status banner based on realtime connection state.
     * Shows "Connecting..." when establishing connection, "Connection lost" when disconnected,
     * and hides banner when connected.
     * 
     * Requirements: 6.2
     * 
     * @param state The current realtime connection state
     */
    private fun updateConnectionStatusBanner(state: com.synapse.social.studioasinc.chat.service.RealtimeState) {
        when (state) {
            is com.synapse.social.studioasinc.chat.service.RealtimeState.Connected -> {
                hideConnectionStatusBanner()
            }
            is com.synapse.social.studioasinc.chat.service.RealtimeState.Connecting -> {
                showConnectionStatusBanner(
                    message = "Connecting...",
                    showProgress = true,
                    showRetry = false,
                    backgroundColor = R.color.md_theme_secondaryContainer,
                    textColor = R.color.md_theme_onSecondaryContainer
                )
            }
            is com.synapse.social.studioasinc.chat.service.RealtimeState.Disconnected -> {
                showConnectionStatusBanner(
                    message = "Connection lost",
                    showProgress = false,
                    showRetry = true,
                    backgroundColor = R.color.md_theme_errorContainer,
                    textColor = R.color.md_theme_onErrorContainer
                )
            }
            is com.synapse.social.studioasinc.chat.service.RealtimeState.Error -> {
                val errorMessage = when {
                    state.message.contains("timeout", ignoreCase = true) -> "Connection timeout"
                    state.message.contains("network", ignoreCase = true) -> "Network error"
                    state.message.contains("polling", ignoreCase = true) -> "Using backup connection"
                    else -> "Connection error"
                }
                
                val isPollingFallback = state.message.contains("polling", ignoreCase = true)
                showConnectionStatusBanner(
                    message = errorMessage,
                    showProgress = false,
                    showRetry = !isPollingFallback,
                    backgroundColor = if (isPollingFallback) R.color.md_theme_tertiaryContainer else R.color.md_theme_errorContainer,
                    textColor = if (isPollingFallback) R.color.md_theme_onTertiaryContainer else R.color.md_theme_onErrorContainer
                )
            }
        }
    }

    /**
     * Show connection status banner with specified configuration.
     * 
     * @param message The message to display
     * @param showProgress Whether to show progress indicator
     * @param showRetry Whether to show retry button
     * @param backgroundColor Background color resource ID
     * @param textColor Text color resource ID
     */
    private fun showConnectionStatusBanner(
        message: String,
        showProgress: Boolean,
        showRetry: Boolean,
        backgroundColor: Int,
        textColor: Int
    ) {
        connectionStatusBanner?.let { banner ->
            // Update banner appearance
            banner.setBackgroundColor(getColor(backgroundColor))
            
            // Update text and color
            connectionText?.text = message
            connectionText?.setTextColor(getColor(textColor))
            
            // Show/hide progress indicator
            connectionProgress?.visibility = if (showProgress) View.VISIBLE else View.GONE
            
            // Show/hide retry button
            connectionRetryButton?.visibility = if (showRetry) View.VISIBLE else View.GONE
            connectionRetryButton?.setTextColor(getColor(textColor))
            
            // Update icon based on state
            if (showProgress) {
                connectionIcon?.visibility = View.GONE
            } else {
                connectionIcon?.visibility = View.VISIBLE
                connectionIcon?.setImageResource(
                    if (showRetry) R.drawable.ic_wifi_off_24 else R.drawable.ic_wifi_24
                )
                connectionIcon?.setColorFilter(getColor(textColor))
            }
            
            // Show banner with animation if currently hidden
            if (banner.visibility != View.VISIBLE) {
                banner.visibility = View.VISIBLE
                banner.alpha = 0f
                banner.translationY = -banner.height.toFloat()
                banner.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start()
            }
        }
    }

    /**
     * Hide connection status banner with animation.
     */
    private fun hideConnectionStatusBanner() {
        connectionStatusBanner?.let { banner ->
            if (banner.visibility == View.VISIBLE) {
                banner.animate()
                    .alpha(0f)
                    .translationY(-banner.height.toFloat())
                    .setDuration(300)
                    .withEndAction {
                        banner.visibility = View.GONE
                    }
                    .start()
            }
        }
    }

    /**
     * Retry connection when user taps retry button.
     * Attempts to reconnect the realtime service for the current chat.
     */
    private fun retryConnection() {
        val chatId = this.chatId ?: return
        
        lifecycleScope.launch {
            try {
                // Show connecting state
                showConnectionStatusBanner(
                    message = "Reconnecting...",
                    showProgress = true,
                    showRetry = false,
                    backgroundColor = R.color.md_theme_secondaryContainer,
                    textColor = R.color.md_theme_onSecondaryContainer
                )
                
                // Attempt to reconnect
                if (::chatViewModel.isInitialized) {
                    chatViewModel.getRealtimeService()?.reconnect(chatId)
                }
                
                Toast.makeText(this@ChatActivity, "Reconnecting...", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Failed to retry connection", e)
                Toast.makeText(this@ChatActivity, "Reconnection failed", Toast.LENGTH_SHORT).show()
                
                // Show error state again
                showConnectionStatusBanner(
                    message = "Connection failed",
                    showProgress = false,
                    showRetry = true,
                    backgroundColor = R.color.md_theme_errorContainer,
                    textColor = R.color.md_theme_onErrorContainer
                )
            }
        }
    }

    /**
     * Setup swipe-to-reply gesture on RecyclerView
     */
    private fun setupSwipeToReply(recyclerView: RecyclerView) {
        val swipeToReplyCallback = SwipeToReplyCallback(this) { position ->
            // Get message data at position
            val messageData = messagesList.getOrNull(position) ?: return@SwipeToReplyCallback
            
            // Extract message details
            val messageId = messageData["id"]?.toString() 
                ?: messageData["key"]?.toString() 
                ?: return@SwipeToReplyCallback
            val messageText = messageData["content"]?.toString() 
                ?: messageData["message_text"]?.toString() 
                ?: ""
            
            // Get sender name
            val senderId = messageData["sender_id"]?.toString() 
                ?: messageData["uid"]?.toString()
            val senderName = if (senderId == currentUserId) {
                "You"
            } else {
                otherUserData?.get("username")?.toString() ?: "User"
            }
            
            // Prepare reply
            prepareReply(messageId, messageText, senderName)
        }
        
        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(swipeToReplyCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    // Animation state tracking to prevent conflicts (Requirement 7.3)
    private var replyAnimationJob: kotlinx.coroutines.Job? = null
    
    // Debounce timestamp for close button clicks (Requirement 7.3)
    private var lastCancelClickTime = 0L
    private val CANCEL_CLICK_DEBOUNCE_MS = 300L
    
    /**
     * Setup reply preview observers and listeners
     * Wires close button to cancelReply() method
     * Adds haptic feedback on click
     * Ensures proper touch feedback
     * Implements debouncing for rapid clicks
     * 
     * Requirements: 2.2, 2.4, 7.3
     */
    private fun setupReplyPreview() {
        // Setup close button click listener with debouncing
        replyCancelButton?.setOnClickListener {
            // Debounce rapid clicks (Requirement 7.3)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCancelClickTime < CANCEL_CLICK_DEBOUNCE_MS) {
                return@setOnClickListener
            }
            lastCancelClickTime = currentTime
            
            // Add haptic feedback on click
            try {
                it.performHapticFeedback(
                    android.view.HapticFeedbackConstants.CONTEXT_CLICK,
                    android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            } catch (e: Exception) {
                // Haptic feedback not available on this device
                android.util.Log.d(TAG, "Haptic feedback not available: ${e.message}")
            }
            
            // Cancel reply
            cancelReply()
        }
    }
    

    
    /**
     * Prepare reply to a message (called from message actions)
     * Sets reply username and message text
     * Handles media preview visibility and loading with error handling
     * Shows reply preview with animation
     * Maintains keyboard focus on text input
     * Prevents animation conflicts
     * 
     * Requirements: 3.1, 3.2, 3.3, 5.1, 5.4, 7.2, 7.3
     * 
     * @param messageId The ID of the message to reply to
     * @param messageText The text content of the message
     * @param senderName The name of the message sender
     */
    fun prepareReply(messageId: String, messageText: String, senderName: String) {
        // Cancel any ongoing animation to prevent conflicts (Requirement 7.3)
        replyAnimationJob?.cancel()
        replyPreviewContainer?.animate()?.cancel()
        
        // Store reply message ID
        replyMessageId = messageId
        
        // Set reply username and message text
        replyUsername?.text = senderName
        replyMessage?.text = messageText
        
        // Find the message data to check for media
        val messageData = messagesList.find { it["id"]?.toString() == messageId }
        
        // Handle media preview visibility and loading with error handling (Requirement 7.2)
        val attachmentUrl = messageData?.get("attachment_url")?.toString()
        val messageType = messageData?.get("message_type")?.toString()
        
        if (!attachmentUrl.isNullOrEmpty() && attachmentUrl != "null" && messageType == "image") {
            replyMediaPreview?.visibility = View.VISIBLE
            replyMediaPreview?.let { imageView ->
                Glide.with(this)
                    .load(Uri.parse(attachmentUrl))
                    .centerCrop()
                    .placeholder(R.drawable.ph_imgbluredsqure) // Loading placeholder (Requirement 7.2)
                    .error(R.drawable.ph_imgbluredsqure) // Error placeholder (Requirement 7.2)
                    .into(imageView)
            }
        } else {
            replyMediaPreview?.visibility = View.GONE
        }
        
        // Set content description for accessibility
        replyPreviewContainer?.contentDescription = getString(R.string.replying_to, senderName)
        
        // Show reply preview with animation (Requirement 5.1, 7.3)
        replyAnimationJob = lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                replyPreviewContainer?.apply {
                    alpha = 0f
                    translationY = -20f
                    visibility = View.VISIBLE
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(200)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                }
            }
        }
        
        // Maintain keyboard focus on text input
        messageInput?.requestFocus()
        
        // Show keyboard
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.showSoftInput(messageInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    /**
     * Cancel reply action
     * Clears reply state (replyMessageId = null)
     * Hides reply preview with animation
     * Clears reply text fields
     * Hides media preview
     * Prevents animation conflicts
     * 
     * Requirements: 2.2, 5.2, 7.3
     */
    private fun cancelReply() {
        // Cancel any ongoing animation to prevent conflicts (Requirement 7.3)
        replyAnimationJob?.cancel()
        replyPreviewContainer?.animate()?.cancel()
        
        // Clear reply state
        replyMessageId = null
        
        // Hide reply preview with animation (Requirement 5.2, 7.3)
        replyAnimationJob = lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                replyPreviewContainer?.apply {
                    animate()
                        .alpha(0f)
                        .translationY(-20f)
                        .setDuration(150)
                        .setInterpolator(android.view.animation.AccelerateInterpolator())
                        .withEndAction {
                            visibility = View.GONE
                            translationY = 0f
                            
                            // Clear reply text fields
                            replyUsername?.text = ""
                            replyMessage?.text = ""
                            
                            // Hide media preview
                            replyMediaPreview?.visibility = View.GONE
                            
                            // Clear content description for accessibility
                            contentDescription = null
                        }
                        .start()
                }
            }
        }
    }
    
    /**
     * Scroll to a specific message and highlight it
     * 
     * @param messageId The ID of the message to scroll to
     */
    private fun scrollToMessage(messageId: String) {
        // Find the position of the message in the list
        val position = messagesList.indexOfFirst { 
            it["id"]?.toString() == messageId 
        }
        
        if (position != -1) {
            // Scroll to the message with smooth animation
            recyclerView?.smoothScrollToPosition(position)
            
            // Highlight the message briefly after scrolling
            recyclerView?.postDelayed({
                highlightMessage(position)
            }, 300) // Wait for scroll animation to complete
        } else {
            // Message not found in current list
            // In a real implementation, we would load more messages
            Toast.makeText(
                this,
                "Original message not found. It may have been deleted or not loaded yet.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Highlight a message with a flash animation
     * 
     * @param position The position of the message in the list
     */
    private fun highlightMessage(position: Int) {
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
        val messageView = viewHolder?.itemView?.findViewById<LinearLayout>(R.id.messageBG)
        
        messageView?.let { view ->
            // Store original background
            val originalBackground = view.background
            
            // Create highlight animation
            val highlightColor = getColor(R.color.md_theme_primaryContainer)
            view.setBackgroundColor(highlightColor)
            
            // Fade back to original background
            view.animate()
                .alpha(0.5f)
                .setDuration(200)
                .withEndAction {
                    view.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .withEndAction {
                            view.background = originalBackground
                        }
                        .start()
                }
                .start()
        }
    }

    /**
     * Set up Supabase Realtime channel for messages
     * Subscribes to UPDATE and INSERT events on messages table filtered by chat_id
     * Handles reconnection scenarios gracefully
     * 
     * NOTE: The Supabase Realtime API for version 2.6.0 requires proper configuration.
     * This implementation provides the structure for Realtime sync. The actual Realtime
     * subscription needs to be configured based on the Supabase Realtime documentation
     * for the specific version being used.
     * 
     * For now, message updates will be handled through:
     * 1. Optimistic UI updates when sending messages
     * 2. Periodic refresh when returning to the chat
     * 3. Manual refresh by pulling down
     * 
     * Requirements: 7.1, 7.2, 7.4
     */
    private fun setupRealtimeSubscription() {
        val currentChatId = chatId
        if (currentChatId == null) {
            android.util.Log.w("ChatActivity", "Cannot setup realtime: chatId is null")
            return
        }
        
        // Cancel existing job if any
        realtimeJob?.cancel()
        
        // Remove existing channel if any
        val oldChannel = realtimeChannel
        if (oldChannel != null) {
             lifecycleScope.launch(Dispatchers.IO) {
                 try {
                     SupabaseClient.client.realtime.removeChannel(oldChannel)
                 } catch (e: Exception) {
                     android.util.Log.e("ChatActivity", "Error removing old channel", e)
                 }
             }
        }

        realtimeJob = lifecycleScope.launch {
            try {
                android.util.Log.d("ChatActivity", "Setting up Realtime subscription for chat: $currentChatId")

                val channel = SupabaseClient.client.realtime.channel("messages_$currentChatId")
                realtimeChannel = channel

                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                    filter = "chat_id=eq.$currentChatId"
                }

                channel.subscribe()

                changes.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> handleMessageInsert(action.record)
                        is PostgresAction.Update -> handleMessageUpdate(action.record)
                        is PostgresAction.Delete -> {
                             // Soft deletes come as Update with is_deleted=true
                             // Hard deletes handling if needed
                             val id = action.oldRecord["id"]?.toString()?.removeSurrounding("\"")
                             if (id != null) {
                                 android.util.Log.d("ChatActivity", "Received hard delete for message: $id")
                             }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error in Realtime subscription", e)
                // Attempt reconnection after delay handled by handleRealtimeReconnection which calls this method
                 handleRealtimeReconnection()
            }
        }
    }
    
    /**
     * Handle Realtime reconnection scenarios gracefully
     * Attempts to reconnect after a delay when connection is lost
     * 
     * Requirements: 7.4
     */
    private fun handleRealtimeReconnection() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("ChatActivity", "Attempting to reconnect Realtime subscription...")
                
                // Wait before attempting reconnection
                delay(3000) // 3 second delay
                
                // Only reconnect if activity is still active and chat is visible
                if (!isAppInBackground && chatId != null) {
                    setupRealtimeSubscription()
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Failed to reconnect Realtime subscription", e)
            }
        }
    }
    
    /**
     * Calculate which message positions are affected by a new message insertion
     * and need their grouping recalculated.
     * 
     * When a new message is added:
     * - The previous message may change from LAST to MIDDLE or from SINGLE to FIRST
     * - The new message needs its grouping calculated
     * 
     * @param newMessagePosition The position of the newly added message
     * @return List of positions that need grouping recalculation
     */
    private fun calculateAffectedPositions(newMessagePosition: Int): List<Int> {
        val affectedPositions = mutableListOf<Int>()
        
        // The new message itself is affected
        affectedPositions.add(newMessagePosition)
        
        // The previous message (if exists) is affected because it may change grouping position
        if (newMessagePosition > 0) {
            affectedPositions.add(newMessagePosition - 1)
        }
        
        return affectedPositions
    }
    
    /**
     * Handle real-time message updates (edits and deletions)
     * Filters for updates where is_deleted or delete_for_everyone changed
     * 
     * Requirements: 7.1, 7.2
     */
    private fun handleMessageUpdate(record: JsonObject) {
        lifecycleScope.launch {
            try {
                val messageId = record["id"]?.toString()?.removeSurrounding("\"") ?: return@launch
                val isEdited = record["is_edited"]?.toString()?.removeSurrounding("\"")?.toBooleanStrictOrNull() ?: false
                val isDeleted = record["is_deleted"]?.toString()?.removeSurrounding("\"")?.toBooleanStrictOrNull() ?: false
                val deleteForEveryone = record["delete_for_everyone"]?.toString()?.removeSurrounding("\"")?.toBooleanStrictOrNull() ?: false
                
                android.util.Log.d("ChatActivity", "Received message update - ID: $messageId, isDeleted: $isDeleted, deleteForEveryone: $deleteForEveryone, isEdited: $isEdited")
                
                // Find the message in the list
                val position = messagesList.indexOfFirst { it["id"]?.toString() == messageId }
                
                if (position != -1) {
                    // Check if this is a deletion event (either is_deleted or delete_for_everyone is true)
                    if (isDeleted && deleteForEveryone) {
                        // Handle deletion for everyone
                        handleRealtimeMessageDeletion(messageId, position, isDeleted, deleteForEveryone)
                    } else if (isEdited) {
                        // Handle edit
                        handleRealtimeMessageEdit(messageId, position, record)
                    }
                } else {
                    android.util.Log.w("ChatActivity", "Message not found in list: $messageId")
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error handling message update", e)
            }
        }
    }
    
    /**
     * Handle real-time message inserts (new messages including forwarded)
     */
    private fun handleMessageInsert(record: JsonObject) {
        lifecycleScope.launch {
            try {
                val chatIdFromRecord = record["chat_id"]?.toString()?.removeSurrounding("\"")
                
                // Only process if message is for current chat
                if (chatIdFromRecord != chatId) {
                    return@launch
                }
                
                val senderId = record["sender_id"]?.toString()?.removeSurrounding("\"")
                
                // Don't add if it's from current user (already added optimistically)
                if (senderId == currentUserId) {
                    return@launch
                }
                
                handleRealtimeForwardedMessage(record)
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error handling message insert", e)
            }
        }
    }
    
    /**
     * Handle real-time message edits
     * Updates message in RecyclerView adapter data and refreshes the view
     */
    private fun handleRealtimeMessageEdit(messageId: String, position: Int, record: JsonObject) {
        runOnUiThread {
            try {
                // Update message data
                val message = messagesList[position]
                val newContent = record["content"]?.toString()?.removeSurrounding("\"") ?: ""
                val editedAt = record["edited_at"]?.toString()?.removeSurrounding("\"")?.toLongOrNull() ?: System.currentTimeMillis()
                
                message["content"] = newContent
                message["message_text"] = newContent
                message["is_edited"] = true
                message["edited_at"] = editedAt
                
                // Refresh the specific message view with animation
                chatAdapter?.notifyItemChanged(position)
                
                // Show brief animation to indicate update
                recyclerView?.postDelayed({
                    val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                    viewHolder?.itemView?.let { view ->
                        view.alpha = 0.5f
                        view.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    }
                }, 100)
                
                android.util.Log.d("ChatActivity", "Message edited in real-time: $messageId")
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error updating edited message in UI", e)
            }
        }
    }
    
    /**
     * Handle real-time message deletions
     * Replaces message content with deleted placeholder
     * Ensures smooth UI updates without disrupting scroll position
     * Handles edge case where deleted message is currently selected
     * 
     * Requirements: 1.2, 2.2, 7.2, 7.3
     */
    private fun handleRealtimeMessageDeletion(messageId: String, position: Int, isDeleted: Boolean, deleteForEveryone: Boolean) {
        runOnUiThread {
            try {
                // Save current scroll position to restore later
                val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager
                val scrollPosition = layoutManager?.findFirstVisibleItemPosition() ?: 0
                val scrollOffset = layoutManager?.findViewByPosition(scrollPosition)?.top ?: 0
                
                // Check if message is currently selected in multi-select mode
                val isMessageSelected = multiSelectManager?.isMessageSelected(messageId) ?: false
                if (isMessageSelected) {
                    // Deselect the message since it's being deleted
                    multiSelectManager?.toggleMessageSelection(messageId)
                    android.util.Log.d("ChatActivity", "Deselected deleted message: $messageId")
                }
                
                // Update message data
                val message = messagesList[position]
                message["is_deleted"] = isDeleted
                message["delete_for_everyone"] = deleteForEveryone
                
                // Refresh the specific message view with animation
                chatAdapter?.notifyItemChanged(position)
                
                // Restore scroll position to prevent disruption
                layoutManager?.scrollToPositionWithOffset(scrollPosition, scrollOffset)
                
                // Show brief animation to indicate deletion
                recyclerView?.postDelayed({
                    val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                    viewHolder?.itemView?.let { view ->
                        view.alpha = 1f
                        view.animate()
                            .alpha(0.5f)
                            .setDuration(200)
                            .withEndAction {
                                view.animate()
                                    .alpha(1f)
                                    .setDuration(200)
                                    .start()
                            }
                            .start()
                    }
                }, 100)
                
                android.util.Log.d("ChatActivity", "Message deleted in real-time: $messageId (isDeleted=$isDeleted, deleteForEveryone=$deleteForEveryone)")
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error updating deleted message in UI", e)
            }
        }
    }
    
    /**
     * Handle real-time forwarded messages
     * Adds new message to RecyclerView and scrolls if user is at bottom
     * Implements deduplication to prevent duplicate messages from appearing
     */
    private fun handleRealtimeForwardedMessage(record: JsonObject) {
        runOnUiThread {
            try {
                val newMessageId = record["id"]?.toString()?.removeSurrounding("\"")
                
                // Check if incoming message ID already exists in messagesList (deduplication)
                val existingIndex = messagesList.indexOfFirst { 
                    it["id"]?.toString() == newMessageId 
                }
                
                // Create message map from record
                val newMessage = HashMap<String, Any?>()
                newMessage["id"] = newMessageId
                newMessage["chat_id"] = record["chat_id"]?.toString()?.removeSurrounding("\"")
                newMessage["sender_id"] = record["sender_id"]?.toString()?.removeSurrounding("\"")
                newMessage["uid"] = record["sender_id"]?.toString()?.removeSurrounding("\"")
                newMessage["content"] = record["content"]?.toString()?.removeSurrounding("\"")
                newMessage["message_text"] = record["content"]?.toString()?.removeSurrounding("\"")
                newMessage["message_type"] = record["message_type"]?.toString()?.removeSurrounding("\"")
                newMessage["created_at"] = record["created_at"]?.toString()?.removeSurrounding("\"")?.toLongOrNull() ?: System.currentTimeMillis()
                newMessage["push_date"] = record["created_at"]?.toString()?.removeSurrounding("\"")?.toLongOrNull() ?: System.currentTimeMillis()
                newMessage["is_deleted"] = false
                newMessage["is_edited"] = false
                newMessage["delivery_status"] = "delivered"
                
                // Check if message is forwarded
                val forwardedFromMessageId = record["forwarded_from_message_id"]?.toString()?.removeSurrounding("\"")
                if (!forwardedFromMessageId.isNullOrEmpty() && forwardedFromMessageId != "null") {
                    newMessage["forwarded_from_message_id"] = forwardedFromMessageId
                    newMessage["forwarded_from_chat_id"] = record["forwarded_from_chat_id"]?.toString()?.removeSurrounding("\"")
                }
                
                // Check if message is a reply
                val replyToId = record["reply_to_id"]?.toString()?.removeSurrounding("\"")
                if (!replyToId.isNullOrEmpty() && replyToId != "null") {
                    newMessage["replied_message_id"] = replyToId
                }
                
                if (existingIndex != -1) {
                    // Message already exists, update existing message instead of adding new one
                    messagesList[existingIndex] = newMessage
                    chatAdapter?.notifyItemChanged(existingIndex)
                    android.util.Log.d("ChatActivity", "Updated existing message in real-time: $newMessageId")
                } else {
                    // New message, add it to list
                    messagesList.add(newMessage)
                    
                    // Calculate affected range for grouping updates (will be implemented in subtask 5.2)
                    val affectedPositions = calculateAffectedPositions(messagesList.size - 1)
                    
                    chatAdapter?.notifyItemInserted(messagesList.size - 1)
                    
                    // Notify adapter of grouping changes for affected messages
                    affectedPositions.forEach { position ->
                        if (position != messagesList.size - 1) {
                            chatAdapter?.notifyItemChanged(position)
                        }
                    }
                    
                    // Check if user is at bottom of list
                    val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager
                    val lastVisiblePosition = layoutManager?.findLastCompletelyVisibleItemPosition() ?: -1
                    val isAtBottom = lastVisiblePosition >= messagesList.size - 2 // Account for newly added message
                    
                    if (isAtBottom) {
                        // Scroll to new message if user is at bottom
                        recyclerView?.smoothScrollToPosition(messagesList.size - 1)
                    } else {
                        // Show notification if user is scrolled up
                        Toast.makeText(
                            this,
                            "New message received",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                    android.util.Log.d("ChatActivity", "New message received in real-time: $newMessageId")
                }
                
                // Mark message as read if user is viewing
                if (currentUserId != null) {
                    lifecycleScope.launch {
                        chatService.markMessagesAsRead(chatId!!, currentUserId!!)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error adding new message to UI", e)
            }
        }
    }

    /**
     * Show message actions bottom sheet on long press
     */
    private fun showMessageActionsBottomSheet(messageId: String, position: Int) {
        val messageData = messagesList.getOrNull(position) ?: return
        
        MessageActionsBottomSheet.show(
            fragmentManager = supportFragmentManager,
            messageData = messageData,
            currentUserId = currentUserId ?: return,
            listener = object : MessageActionsBottomSheet.MessageActionListener {
                override fun onReplyAction(messageId: String, messageText: String, senderName: String) {
                    prepareReply(messageId, messageText, senderName)
                }
                
                override fun onForwardAction(messageId: String, messageData: Map<String, Any?>) {
                    showForwardDialog(messageId, messageData)
                }
                
                override fun onEditAction(messageId: String, currentText: String) {
                    showEditDialog(messageId, currentText)
                }
                
                override fun onDeleteAction(messageId: String) {
                    showDeleteConfirmation(messageId, false)
                }
                
                override fun onAISummaryAction(messageId: String, messageText: String) {
                    showAISummary(messageId, messageText)
                }
            }
        )
    }
    
    /**
     * Open URL in browser
     */
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Open media viewer for images/videos
     */
    private fun openMediaViewer(url: String, type: String) {
        when (type) {
            "image" -> {
                // Launch ImageGalleryActivity for image viewing
                val intent = ImageGalleryActivity.createIntent(
                    context = this,
                    imageUrls = listOf(url),
                    initialPosition = 0
                )
                startActivity(intent)
            }
            "video" -> {
                // Launch video player (inline playback handled by VideoPlayerView in adapter)
                // For full-screen, we can use the system video player
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(url), "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening video", e)
                    Toast.makeText(this, "Unable to open video", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(this, "Unsupported media type", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Open audio player
     * Audio playback is handled inline by AudioPlayerView in the adapter.
     * This method is called when user taps on the audio attachment.
     */
    private fun openAudioPlayer(url: String) {
        // Audio playback is handled inline by the AudioPlayerView in chat_bubble_audio.xml
        // No additional action needed as the play button in the bubble handles playback
        Log.d(TAG, "Audio playback handled inline: $url")
    }
    
    /**
     * Open document with system viewer
     */
    private fun openDocument(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), getMimeTypeFromUrl(url))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // No app found, try opening as generic file
                val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(genericIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening document", e)
            Toast.makeText(this, "Unable to open document", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Get MIME type from URL based on file extension
     */
    private fun getMimeTypeFromUrl(url: String): String {
        val extension = url.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            else -> "*/*"
        }
    }
    
    /**
     * Open user profile
     */
    private fun openUserProfile(userId: String) {
        val intent = Intent(this, ProfileComposeActivity::class.java)
        intent.putExtra("uid", userId)
        startActivity(intent)
    }
    

    
    /**
     * Show forward message dialog
     */
    private fun showForwardDialog(messageId: String, messageData: Map<String, Any?>) {
        val dialog = ForwardMessageDialog.newInstance(messageId, messageData)
        dialog.show(supportFragmentManager, "ForwardMessageDialog")
    }
    
    /**
     * Show edit message dialog
     */
    private fun showEditDialog(messageId: String, currentText: String) {
        val dialog = EditMessageDialog.newInstance(
            messageId = messageId,
            currentText = currentText,
            listener = object : EditMessageDialog.EditMessageListener {
                override fun onMessageEdited(messageId: String, newText: String) {
                    editMessage(messageId, newText)
                }
            }
        )
        dialog.show(supportFragmentManager, "EditMessageDialog")
    }
    
    /**
     * Edit a message
     */
    private fun editMessage(messageId: String, newText: String) {
        lifecycleScope.launch {
            try {
                val result = chatService.editMessage(messageId, newText)
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@ChatActivity, "Message edited", Toast.LENGTH_SHORT).show()
                        // Update will come through realtime
                    },
                    onFailure = { error ->
                        showError("Failed to edit message: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showError("Error editing message: ${e.message}")
            }
        }
    }
    
    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteConfirmation(messageId: String, deleteForEveryone: Boolean) {
        // Determine if this is the user's own message
        val messageData = messagesList.find { it["id"]?.toString() == messageId }
        val senderId = messageData?.get("sender_id")?.toString() 
            ?: messageData?.get("uid")?.toString()
        val isOwnMessage = senderId == currentUserId
        
        // Show delete confirmation dialog with single message
        val dialog = DeleteConfirmationDialog.newInstance(listOf(messageId), isOwnMessage)
        dialog.show(supportFragmentManager, "DeleteConfirmationDialog")
    }
    
    /**
     * Delete a message
     */
    fun deleteMessage(messageId: String, deleteForEveryone: Boolean) {
        lifecycleScope.launch {
            try {
                val result = chatService.deleteMessage(messageId, deleteForEveryone)
                result.fold(
                    onSuccess = {
                        val message = if (deleteForEveryone) "Message deleted for everyone" else "Message deleted for you"
                        Toast.makeText(this@ChatActivity, message, Toast.LENGTH_SHORT).show()
                        
                        // If deleting for me only, remove from local list immediately
                        if (!deleteForEveryone) {
                            val position = messagesList.indexOfFirst { it["id"]?.toString() == messageId }
                            if (position != -1) {
                                messagesList.removeAt(position)
                                chatAdapter?.notifyItemRemoved(position)
                            }
                        }
                        // If deleting for everyone, update will come through realtime
                    },
                    onFailure = { error ->
                        showError("Failed to delete message: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showError("Error deleting message: ${e.message}")
            }
        }
    }
    
    /**
     * Show AI summary inline in the message bubble or toggle back to original
     */
    private fun showAISummary(messageId: String, messageText: String) {
        // Find the message in the list
        val position = messagesList.indexOfFirst { 
            it["id"]?.toString() == messageId 
        }
        
        if (position == -1) {
            Toast.makeText(this, "Message not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val messageData = messagesList[position]
        
        // Check if currently showing summary (user wants to see original)
        val showingSummary = messageData["showing_ai_summary"]?.toString()?.toBooleanStrictOrNull() ?: false
        if (showingSummary) {
            // Toggle back to original text
            messageData["showing_ai_summary"] = false
            chatAdapter?.notifyItemChanged(position)
            return
        }
        
        // Check if summary already exists
        val existingSummary = messageData["ai_summary"]?.toString()
        if (!existingSummary.isNullOrEmpty()) {
            // Toggle to show summary
            messageData["showing_ai_summary"] = true
            chatAdapter?.notifyItemChanged(position)
            return
        }
        
        // Show loading state
        Toast.makeText(this, "Generating AI summary...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                viewModel.generateAISummary(messageId, messageText).collect { state ->
                    when {
                        state.isGenerating -> {
                            // Loading state already shown
                        }
                        state.summary != null -> {
                            // Store summary in message data
                            messageData["ai_summary"] = state.summary
                            messageData["showing_ai_summary"] = true
                            
                            // Update the message bubble
                            runOnUiThread {
                                chatAdapter?.notifyItemChanged(position)
                                Toast.makeText(
                                    this@ChatActivity,
                                    "AI summary generated",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        state.error != null -> {
                            runOnUiThread {
                                Toast.makeText(
                                    this@ChatActivity,
                                    "Failed to generate summary: ${state.error}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@ChatActivity,
                        "Error generating summary: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    /**
     * Show edit history dialog
     */
    private fun showEditHistory(messageId: String) {
        val dialog = EditHistoryDialog.newInstance(messageId)
        dialog.show(supportFragmentManager, "EditHistoryDialog")
    }

    /**
     * Setup real-time message state updates for read receipts
     * This method handles updating message states when read receipts are received
     * 
     * Requirements: 4.3
     */
    private fun setupMessageStateUpdates() {
        // This method would integrate with the ReadReceiptManager when it's fully implemented
        // For now, we provide a placeholder that demonstrates the integration pattern
        
        // Example of how message states would be updated:
        // readReceiptManager?.subscribeToReadReceipts(chatId) { readReceiptEvent ->
        //     // Update message states in the adapter
        //     val messageStates = readReceiptEvent.messageIds.associateWith { "read" }
        //     chatAdapter?.updateMessageStates(messageStates)
        // }
        
        android.util.Log.d("ChatActivity", "Message state updates setup completed")
    }

    /**
     * Update message state for a specific message
     * Called when read receipts are received from the backend
     * 
     * Requirements: 4.3
     * 
     * @param messageId The message ID to update
     * @param newState The new message state (sent, delivered, read, failed)
     */
    fun updateMessageState(messageId: String, newState: String) {
        runOnUiThread {
            chatAdapter?.updateMessageState(messageId, newState)
        }
    }

    /**
     * Update multiple message states efficiently
     * Used for batch read receipt updates
     * 
     * Requirements: 4.3
     * 
     * @param messageStates Map of message ID to new state
     */
    fun updateMessageStates(messageStates: Map<String, String>) {
        runOnUiThread {
            chatAdapter?.updateMessageStates(messageStates)
        }
    }

    /**
     * Handle activity resume
     * Ensures Realtime subscription is active when chat screen is visible
     * 
     * Requirements: 7.4
     */
    override fun onResume() {
        super<BaseActivity>.onResume()
        Log.d(TAG, "Lifecycle: onResume")
        
        // App is returning to foreground
        isAppInBackground = false
        
        // Subscribe to typing events and read receipts when chat screen opens
        // Ensure chatViewModel is initialized before accessing it
        val currentChatId = chatId
        if (currentChatId != null && ::chatViewModel.isInitialized) {
            chatViewModel.onChatOpened(currentChatId)
            
            // Mark visible messages as read when chat opens (only if not backgrounded)
            // Ensure recyclerView is initialized before marking messages as read
            if (!isAppInBackground && recyclerView != null) {
                markVisibleMessagesAsRead()
            }
        }
        
        // Resume operations when app returns to foreground
        if (::chatViewModel.isInitialized) {
            chatViewModel.setChatVisibility(true)
        }
    }
    
    /**
     * Handle activity pause
     * Manages Realtime subscription lifecycle when chat screen is not visible
     * 
     * Requirements: 7.4
     */
    override fun onPause() {
        super<BaseActivity>.onPause()
        Log.d(TAG, "Lifecycle: onPause")

        // App is going to background
        isAppInBackground = true

        // Defer read receipt updates when app is backgrounded
        if (::chatViewModel.isInitialized) {
            chatViewModel.setChatVisibility(false)
        }

        // Stop typing indicator when leaving chat
        val currentChatId = chatId
        val currentUser = currentUserId
        if (currentChatId != null && currentUser != null) {
            lifecycleScope.launch {
                chatService.updateTypingStatus(currentChatId, currentUser, false)
            }
        }
    }
    
    // DefaultLifecycleObserver methods for app backgrounding
    override fun onStart(owner: LifecycleOwner) {
        // App is coming to foreground
        isAppInBackground = false
        
        // Resume operations when app returns to foreground
        // Only proceed if initialization has completed
        val currentChatId = chatId
        if (currentChatId != null && ::chatViewModel.isInitialized) {
            chatViewModel.setChatVisibility(true)
            // Re-subscribe to events if needed
            chatViewModel.onChatOpened(currentChatId)
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        // App is going to background
        isAppInBackground = true
        
        // Defer read receipt updates when app is backgrounded
        if (::chatViewModel.isInitialized) {
            chatViewModel.setChatVisibility(false)
        }
        
        // Stop sending typing events when app is backgrounded
        val currentChatId = chatId
        val currentUser = currentUserId
        if (currentChatId != null && currentUser != null) {
            lifecycleScope.launch {
                chatService.updateTypingStatus(currentChatId, currentUser, false)
            }
        }
    }

    /**
     * Mark all visible messages as read when chat opens.
     * This implements requirement 4.1 - mark messages as read within 1 second when chat opens.
     * Only marks messages as read if app is not in background (requirement 4.5).
     */
    private fun markVisibleMessagesAsRead() {
        // Don't mark messages as read if app is in background
        if (isAppInBackground) {
            return
        }
        
        val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            return
        }
        
        // Get message IDs for visible messages that aren't sent by current user
        val visibleMessageIds = mutableListOf<String>()
        for (position in firstVisible..lastVisible) {
            val messageData = messagesList.getOrNull(position) ?: continue
            val messageId = messageData["id"]?.toString() ?: continue
            val senderId = messageData["sender_id"]?.toString() 
                ?: messageData["uid"]?.toString()
            
            // Only mark messages from other users as read
            if (senderId != currentUserId) {
                visibleMessageIds.add(messageId)
            }
        }
        
        if (visibleMessageIds.isNotEmpty() && ::chatViewModel.isInitialized) {
            chatViewModel.markVisibleMessagesAsRead(visibleMessageIds)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
    
    /**
     * Clean up resources when activity is destroyed
     * Unsubscribes from Realtime channel to conserve resources
     * 
     * Requirements: 7.4
     */
    override fun onDestroy() {
        super<BaseActivity>.onDestroy()
        Log.d(TAG, "Lifecycle: onDestroy")

        // Remove lifecycle observer to prevent memory leaks.
        lifecycle.removeObserver(this)


        // Clear all Glide image loading requests associated with this activity.
        // This is a critical step to free up memory from images and prevent memory leaks.
        if (isFinishing) {
            Log.d(TAG, "Clearing Glide resources")
            // Glide automatically clears requests when the activity is destroyed,
            // so we don't need to manually clear these views.
            // Attempting to use Glide.with(this) here causes a crash because the activity is destroyed.
        }
        
        // Stop any running animations and cancel related coroutine jobs to prevent leaks.
        typingAnimation?.stopAnimation()
        typingIndicatorAutoHideJob?.cancel()

        // Dismiss any showing dialogs and nullify them to prevent window leaks,
        // which can happen if a dialog is showing when the activity is destroyed.
        synapseLoadingDialog?.dismiss()
        synapseLoadingDialog = null

        // Nullify the adapter and any other listeners to break reference cycles
        // between the RecyclerView and the adapter, which can cause memory leaks.
        recyclerView?.adapter = null
        chatAdapter = null

        // Clean up ChatViewModel resources, which includes unsubscribing from the Realtime channel.
        // This is the most critical cleanup step to prevent memory leaks and stop background network activity.
        if (::chatViewModel.isInitialized) {
            Log.d(TAG, "Cleaning up ChatViewModel and unsubscribing from Realtime channel.")
            chatViewModel.onChatClosed()
        }

        // Clean up local Realtime subscription
        realtimeJob?.cancel()
        val channelToRemove = realtimeChannel
        if (channelToRemove != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                     SupabaseClient.client.realtime.removeChannel(channelToRemove)
                } catch (e: Exception) {
                     Log.e(TAG, "Failed to remove channel in onDestroy", e)
                }
            }
        }
        realtimeChannel = null
    }
    
}
