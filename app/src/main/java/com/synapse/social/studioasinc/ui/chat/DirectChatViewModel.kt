package com.synapse.social.studioasinc.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService

import com.synapse.social.studioasinc.data.remote.services.SupabaseStorageService
import com.synapse.social.studioasinc.chat.service.SupabaseRealtimeService
import com.synapse.social.studioasinc.data.local.database.AppDatabase
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.domain.model.Message
import com.synapse.social.studioasinc.domain.model.Chat
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.core.util.LinkDetectionService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.json.JsonObject
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import com.synapse.social.studioasinc.ui.components.mentions.MentionHelper
import com.synapse.social.studioasinc.data.remote.services.ai.Gemini
import com.synapse.social.studioasinc.data.repository.AiRepository
import com.synapse.social.studioasinc.domain.model.AiSummary
import com.synapse.social.studioasinc.domain.model.ChatThemePreset
import com.synapse.social.studioasinc.domain.model.ChatWallpaper

/**
 * ViewModel for DirectChatScreen
 * Adapter for existing ChatRepository to Compose UI State
 * Requirements: 2.4
 */
@HiltViewModel
class DirectChatViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    // Dependencies
    private val chatDao = AppDatabase.getDatabase(application).chatDao()
    private val chatRepository = ChatRepository(chatDao, SupabaseClient.client)
    private val searchRepository = com.synapse.social.studioasinc.data.repository.SearchRepositoryImpl()
    private val authService = SupabaseAuthenticationService(application)
    private val aiRepository = AiRepository()
    private val settingsRepository = com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl.getInstance(application)
    private var gemini: Gemini? = null
    
    // Enhanced typing and presence managers
    private val typingIndicatorManager = com.synapse.social.studioasinc.chat.TypingIndicatorManager.getInstance()
    private val activeStatusManager = com.synapse.social.studioasinc.chat.ActiveStatusManager.getInstance()
    
    // UI State
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Screen visibility state for read receipts
    private val _isScreenActive = MutableStateFlow(false)
    val isScreenActive: StateFlow<Boolean> = _isScreenActive.asStateFlow()

    // Available chats for forwarding
    private val _availableChats = MutableStateFlow<List<ChatForwardUiModel>>(emptyList())
    val availableChats: StateFlow<List<ChatForwardUiModel>> = _availableChats.asStateFlow()

    private var loadChatsJob: Job? = null
    
    // Messages list (Source of Truth: Realtime/DB)
    private val _dbMessages = MutableStateFlow<List<MessageUiModel>>(emptyList())

    // Typing Debounce Job
    private var stopTypingJob: Job? = null
    
    // Storage Service
    private val storageService = com.synapse.social.studioasinc.data.remote.services.SupabaseStorageService()

    // Optimistic Messages (Temporary local state)
    private val _optimisticMessages = MutableStateFlow<List<MessageUiModel>>(emptyList())

    // Combined Messages Flow
    // Combined Messages Flow
    val messages: StateFlow<List<MessageUiModel>> = combine(_dbMessages, _optimisticMessages) { db, optimistic ->
        // Merge DB messages with optimistic ones.
        // We prioritize DB messages. Optimistic messages are only shown if they haven't been "seen" in the DB yet.

        val merged = ArrayList<MessageUiModel>(db.size + optimistic.size)
        merged.addAll(db)

        val dbIds = db.map { it.id }.toSet()

        optimistic.forEach { optMsg ->
             // 1. Check if the optimistic message ID (tempId) is already in DB (unlikely, but good for safety)
             if (dbIds.contains(optMsg.id)) return@forEach

             // 2. Check for "content sync": mismatched IDs but same content, sender, and recent time
             // This happens when Realtime returns the NEW message with a server-generated ID, but our local one has temp_id.
             val isSynced = db.any { dbMsg ->
                 dbMsg.isFromCurrentUser &&
                 dbMsg.content == optMsg.content &&
                 kotlin.math.abs(dbMsg.timestamp - optMsg.timestamp) < 5000 // 5s window
             }

             if (!isSynced) {
                 merged.add(optMsg)
             }
        }
        // Ensure sorted by timestamp descending (newest first) ?? No, UI expects newest at bottom usually, but LazyColumn is reversed. 
        // The original code passed `items(messages.reversed())` to a reversed LazyColumn.
        // That means `messages` should be sorted Oldest -> Newest (ascending timestamp).
        val sorted = merged.sortedBy { it.timestamp }
        calculateMessagePositions(sorted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun calculateMessagePositions(messages: List<MessageUiModel>): List<MessageUiModel> {
        if (messages.isEmpty()) return emptyList()

        val groupedMessages = ArrayList<MessageUiModel>(messages.size)
        val threshold = 60 * 1000 // 60 seconds to group messages

        for (i in messages.indices) {
            val current = messages[i]
            val prev = messages.getOrNull(i - 1)
            val next = messages.getOrNull(i + 1)

            val isSameAsPrev = prev != null && prev.senderId == current.senderId &&
                    (current.timestamp - prev.timestamp < threshold)
            
            val isSameAsNext = next != null && next.senderId == current.senderId &&
                    (next.timestamp - current.timestamp < threshold)

            val position = when {
                !isSameAsPrev && !isSameAsNext -> MessagePosition.Single
                !isSameAsPrev && isSameAsNext -> MessagePosition.First
                isSameAsPrev && isSameAsNext -> MessagePosition.Middle
                isSameAsPrev && !isSameAsNext -> MessagePosition.Last
                else -> MessagePosition.Single
            }

            groupedMessages.add(current.copy(position = position))
        }

        return groupedMessages
    }
    
    private var currentChatId: String? = null
    private var currentUserId: String? = null
    private var realtimeJob: Job? = null
    
    // Realtime Service - moved before init block
    private val realtimeService = SupabaseRealtimeService()
    
    init {
        loadCurrentUser()
        observeConnectionState()
        observeSettings()
        initializeGemini()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.chatSettings.collect { settings ->
                _uiState.update {
                    it.copy(
                        themePreset = settings.themePreset,
                        wallpaper = settings.wallpaper
                    )
                }
            }
        }
    }

    private fun initializeGemini() {
        gemini = Gemini.Builder(getApplication())
            .model("gemini-1.5-flash")
            .responseType("text")
            .build()
    }
    
    private fun observeConnectionState() {
        viewModelScope.launch {
            realtimeService.connectionState.collect { state ->
                val connectionState = when (state) {
                    is com.synapse.social.studioasinc.chat.service.RealtimeState.Connected -> RealtimeConnectionState.Connected
                    is com.synapse.social.studioasinc.chat.service.RealtimeState.Connecting -> RealtimeConnectionState.Connecting
                    is com.synapse.social.studioasinc.chat.service.RealtimeState.Disconnected -> RealtimeConnectionState.Disconnected
                    is com.synapse.social.studioasinc.chat.service.RealtimeState.Error -> RealtimeConnectionState.Disconnected
                }
                _uiState.update { it.copy(connectionState = connectionState) }

                // Auto-retry if disconnected and we have a chat loaded
                if (connectionState == RealtimeConnectionState.Disconnected && currentChatId != null) {
                    delay(5000)
                    // Check again before retrying to avoid loops if state changed
                    if (_uiState.value.connectionState == RealtimeConnectionState.Disconnected) {
                        retryConnection()
                    }
                }
            }
        }
    }

    /**
     * Retry realtime connection manually
     */
    fun retryConnection() {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            try {
                android.util.Log.d("DirectChatViewModel", "Retrying connection for chat: $chatId")
                
                // 1. Force connection state to connecting
                _uiState.update { it.copy(connectionState = RealtimeConnectionState.Connecting) }
                
                // 2. Run diagnostics
                com.synapse.social.studioasinc.core.util.ConnectionDiagnostics.runDiagnostics(getApplication())
                
                // 3. Cancel existing connection completely
                realtimeJob?.cancel()
                realtimeService.cleanup()
                
                // 4. Wait for cleanup
                delay(1000)
                
                // 5. Reset connection state in service
                realtimeService.resetConnectionState()
                
                // 6. Restart realtime observation
                observeRealtimeMessages(chatId)
                
                android.util.Log.d("DirectChatViewModel", "Connection retry initiated")
            } catch (e: Exception) {
                android.util.Log.e("DirectChatViewModel", "Failed to retry connection", e)
                _uiState.update { it.copy(connectionState = RealtimeConnectionState.Disconnected) }
            }
        }
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            currentUserId = authService.getCurrentUserId()
        }
    }

    fun loadChat(chatId: String) {
        // Only skip full reload if already observing this chat with active realtime
        if (currentChatId == chatId && realtimeJob?.isActive == true) return
        
        // Clean up previous chat's realtime connection
        if (currentChatId != null && currentChatId != chatId) {
            realtimeJob?.cancel()
            viewModelScope.launch {
                currentChatId?.let { oldChatId ->
                    realtimeService.unsubscribeFromChat(oldChatId)
                }
            }
        }
        
        currentChatId = chatId
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Initial fetch (restores from DB / Network)
                val result = chatRepository.getMessagesPage(chatId, null, 50)
                
                result.onSuccess { domainMessages ->
                    val uiMessages = domainMessages.map { it.toUiModel(currentUserId) }
                    _dbMessages.value = uiMessages
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    
                    // Mark messages as read only if screen is active
                    if (currentUserId != null && _isScreenActive.value) {
                        launch { chatRepository.markMessagesAsRead(chatId, currentUserId!!) }
                    }


                    
                    // Start realtime observation
                    observeRealtimeMessages(chatId)
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
                
                // Load other user info
                 chatRepository.getChatParticipants(chatId).onSuccess { userIds ->
                     val otherId = userIds.firstOrNull { it != currentUserId }
                     if (otherId != null) {
                         // Fetch actual user profile from UserProfileManager
                         val userProfile = com.synapse.social.studioasinc.UserProfileManager.getUserProfile(otherId)
                         _uiState.update {
                             it.copy(otherUser = ChatUserInfo(
                                 id = otherId,
                                 username = userProfile?.username ?: "User",
                                 displayName = userProfile?.displayName,
                                 avatarUrl = userProfile?.avatar
                             ))
                         }
                         
                         // Initialize presence tracking after user info is loaded
                         initializePresenceTracking()
                     }
                 }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Start observing everything: Messages and Typing
     */
    private fun observeRealtimeMessages(chatId: String) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            try {
                // 1. Get channel (not subscribed yet)
                val channel = realtimeService.getOrCreateChannelForMessages(chatId)
                
                // 2. Set up postgres changes flow BEFORE subscribing
                val changesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                    filter("chat_id", FilterOperator.EQ, chatId)
                }
                
                // 3. Set up broadcast flow BEFORE subscribing
                val typingFlow = channel.broadcastFlow<JsonObject>(event = "typing")
                
                // 4. Now subscribe to the channel
                channel.subscribe()

                // Notify service that connection is established (since we manually subscribed)
                realtimeService.updateConnectionState(com.synapse.social.studioasinc.chat.service.RealtimeState.Connected)

                // 5. Observe Messages (Insert/Update/Delete)
                launch {
                    try {
                        changesFlow.collect { action ->
                            when (action) {
                                is PostgresAction.Insert -> {
                                    val message = action.decodeRecord<Message>()
                                    // Double check chat_id just in case
                                    if (message.chatId == chatId) handleMessageInsert(message)
                                }
                                is PostgresAction.Update -> {
                                    val message = action.decodeRecord<Message>()
                                    if (message.chatId == chatId) handleMessageUpdate(message)
                                }
                                is PostgresAction.Delete -> {
                                    val oldRecord = action.oldRecord
                                    val recChatId = oldRecord["chat_id"]?.toString()?.replace("\"", "")
                                    if (recChatId == chatId || (recChatId == null)) { 
                                         val id = oldRecord["id"]?.toString()?.replace("\"", "")
                                         if (id != null) handleMessageDelete(id)
                                    }
                                }
                                else -> {}
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DirectChatViewModel", "Realtime message error", e)
                        _uiState.update { it.copy(connectionState = RealtimeConnectionState.Disconnected) }
                        
                        // Auto-retry after a delay if this is a connection issue
                        if (e.message?.contains("connection", ignoreCase = true) == true ||
                            e.message?.contains("network", ignoreCase = true) == true) {
                            delay(3000) // Wait 3 seconds before retry
                            if (currentChatId == chatId) { // Only retry if still on same chat
                                android.util.Log.d("DirectChatViewModel", "Auto-retrying connection after error")
                                observeRealtimeMessages(chatId)
                            }
                        }
                    }
                }
                
                // 6. Observe Typing Events
                launch {
                    try {
                        typingFlow.collect { event ->
                            val payload = event
                            val userId = payload["user_id"]?.toString()?.replace("\"", "") ?: return@collect
                            val isTyping = payload["is_typing"]?.toString()?.toBoolean() ?: false
                            
                            // Ignore self
                            if (userId == currentUserId) return@collect
                            
                            _uiState.update { state ->
                                val currentTyping = state.typingUsers.toMutableSet()
                                if (isTyping) {
                                    // Add user
                                    currentTyping.add(state.otherUser?.username ?: "User")
                                } else {
                                    currentTyping.remove(state.otherUser?.username ?: "User")
                                }
                                state.copy(typingUsers = currentTyping.toList())
                            }
                            
                            // Auto-remove typing status after 3 seconds (debounce safety)
                            if (isTyping) {
                                launch {
                                    kotlinx.coroutines.delay(3000)
                                    _uiState.update { state ->
                                        val currentTyping = state.typingUsers.toMutableSet()
                                        currentTyping.remove(state.otherUser?.username ?: "User")
                                        state.copy(typingUsers = currentTyping.toList())
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = "Realtime typing error: ${e.message}") }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DirectChatViewModel", "Failed to set up realtime connection", e)
                _uiState.update { it.copy(connectionState = RealtimeConnectionState.Disconnected) }
                
                // Auto-retry for connection-related errors
                if (e.message?.contains("connection", ignoreCase = true) == true ||
                    e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true) {
                    
                    delay(5000) // Wait 5 seconds before retry
                    if (currentChatId == chatId) { // Only retry if still on same chat
                        android.util.Log.d("DirectChatViewModel", "Auto-retrying realtime setup after error")
                        observeRealtimeMessages(chatId)
                    }
                } else {
                    _uiState.update { it.copy(error = "Connection failed: ${e.message}") }
                }
            }
        }
    }
    
    /**
     * Handle incoming new message
     */
    private fun handleMessageInsert(message: Message) {
        val uiMessage = message.toUiModel(currentUserId).copy(isAnimating = true)
        _dbMessages.update { current ->
            if (current.any { it.id == uiMessage.id }) current else current + uiMessage
        }

        // Mark as read if from other user and screen is active
        if (!uiMessage.isFromCurrentUser && currentUserId != null && _isScreenActive.value) {
            viewModelScope.launch {
                chatRepository.markMessagesAsRead(message.chatId, currentUserId!!)
            }
        }
        


        // Generate smart replies if the message is from the other user
        if (!uiMessage.isFromCurrentUser) {
            generateSmartReplies()
        }
    }

    /**
     * Handle incoming message update
     */
    private fun handleMessageUpdate(message: Message) {
        val uiMessage = message.toUiModel(currentUserId)
        _dbMessages.update { current ->
            current.map { if (it.id == uiMessage.id) uiMessage else it }
        }
    }

    /**
     * Handle incoming message deletion
     */
    private fun handleMessageDelete(messageId: String) {
        _dbMessages.update { current ->
            current.filter { it.id != messageId }
        }
    }

    // Effects Channel
    private val _effects = kotlinx.coroutines.channels.Channel<ChatEffect>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // AI State
    private val _smartReplies = MutableStateFlow<List<String>>(emptyList())
    val smartReplies: StateFlow<List<String>> = _smartReplies.asStateFlow()

    fun generateSmartReplies() {
        val lastMessages = _dbMessages.value.takeLast(5).map {
             "${if (it.isFromCurrentUser) "Me" else "Other"}: ${it.content}"
        }.joinToString("\n")

        if (lastMessages.isBlank()) return

        val prompt = "Based on the following conversation, suggest 3 short, relevant replies for 'Me'. Return only the replies separated by '|'.\n\nConversation:\n$lastMessages"

        gemini?.sendPrompt(prompt, object : Gemini.GeminiCallback {
            override fun onSuccess(response: String) {
                val replies = response.split("|").map { it.trim() }.filter { it.isNotBlank() }.take(3)
                _smartReplies.value = replies
            }
            override fun onError(error: String) {
                // Log error
            }
            override fun onThinking() {}
        })
    }

    fun summarizeChat() {
         val messagesContent = _dbMessages.value.joinToString("\n") {
             "${if (it.isFromCurrentUser) "Me" else "Other"}: ${it.content}"
        }

        if (messagesContent.isBlank()) return

        val prompt = "Summarize the following conversation in a concise paragraph:\n\n$messagesContent"

        viewModelScope.launch {
            _effects.send(ChatEffect.ShowSnackbar("Generating summary..."))
            gemini?.sendPrompt(prompt, object : Gemini.GeminiCallback {
                override fun onSuccess(response: String) {
                    viewModelScope.launch {
                        _effects.send(ChatEffect.ShowSnackbar("Summary: $response")) // Or show in a dialog

                        // Save summary
                        val currentMsg = _dbMessages.value.lastOrNull()
                        if (currentMsg != null && currentUserId != null) {
                            try {
                                val summary = AiSummary(
                                    id = java.util.UUID.randomUUID().toString(),
                                    messageId = currentMsg.id,
                                    summaryText = response,
                                    generatedAt = System.currentTimeMillis(),
                                    generatedBy = currentUserId!!,
                                    characterCount = response.length
                                )
                                aiRepository.saveSummary(summary)
                            } catch (e: Exception) {
                                // Log error
                            }
                        }
                    }
                }
                override fun onError(error: String) {
                    viewModelScope.launch { _effects.send(ChatEffect.ShowSnackbar("Failed to summarize")) }
                }
                override fun onThinking() {}
            })
        }
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> {
                sendMessage(intent.content)
                _smartReplies.value = emptyList() // Clear suggestions after sending
            }
            is ChatIntent.UpdateInputText -> {
                _uiState.update { it.copy(inputText = intent.text) }

                // Typing Indicator Logic with Debounce
                if (intent.text.text.isNotEmpty()) {
                    setTypingStatus(true)
                    // Reset debounce timer
                    stopTypingJob?.cancel()
                    stopTypingJob = viewModelScope.launch {
                        delay(3000) // 3 seconds of inactivity
                        setTypingStatus(false)
                    }
                } else {
                    setTypingStatus(false)
                    stopTypingJob?.cancel()
                }

                // Check for mention
                val mentionQuery = MentionHelper.getMentionQuery(intent.text)
                if (mentionQuery != null) {
                    viewModelScope.launch {
                        searchRepository.searchUsers(mentionQuery.query).onSuccess { users ->
                             _uiState.update { it.copy(mentionSuggestions = users) }
                        }.onFailure {
                             _uiState.update { it.copy(mentionSuggestions = emptyList()) }
                        }
                    }
                } else {
                     _uiState.update { it.copy(mentionSuggestions = emptyList()) }
                }
            }
            is ChatIntent.InsertMention -> {
                val currentInput = _uiState.value.inputText
                val mentionQuery = MentionHelper.getMentionQuery(currentInput)
                if (mentionQuery != null) {
                    val range = mentionQuery.range
                    val newText = currentInput.text.replaceRange(range.start, range.end, "@${intent.user.username} ")
                    val newCursor = range.start + intent.user.username.length + 2 // @ + name + space
                    _uiState.update { 
                        it.copy(
                            inputText = TextFieldValue(newText, selection = TextRange(newCursor)),
                            mentionSuggestions = emptyList()
                        ) 
                    }
                }
            }
            is ChatIntent.SetReplyTo -> {
                _uiState.update { it.copy(replyTo = intent.message) }
            }
            is ChatIntent.ClearReply -> {
                _uiState.update { it.copy(replyTo = null) }
            }
            is ChatIntent.DeleteMessage -> deleteMessage(intent.messageId)
            is ChatIntent.EditMessage -> editMessage(intent.messageId, intent.newContent)
            is ChatIntent.CopyToClipboard -> {
                viewModelScope.launch {
                    _effects.send(ChatEffect.CopyToClipboard(intent.content))
                    _effects.send(ChatEffect.ShowSnackbar("Copied to clipboard"))
                }
            }
            is ChatIntent.ForwardMessage -> {
                forwardMessage(intent.messageId, intent.toChatIds)
            }
            // Multi-select intents
            is ChatIntent.ToggleMessageSelection -> {
                val currentIds = _uiState.value.selectedMessageIds
                val newIds = if (currentIds.contains(intent.messageId)) {
                    currentIds - intent.messageId
                } else {
                    currentIds + intent.messageId
                }
                _uiState.update { 
                    it.copy(
                        selectedMessageIds = newIds,
                        isMultiSelectMode = newIds.isNotEmpty()
                    )
                }
            }
            is ChatIntent.EnterMultiSelectMode -> {
                _uiState.update { it.copy(isMultiSelectMode = true) }
            }
            is ChatIntent.ExitMultiSelectMode -> {
                _uiState.update { it.copy(isMultiSelectMode = false, selectedMessageIds = emptySet()) }
            }
            is ChatIntent.DeleteSelectedMessages -> {
                viewModelScope.launch {
                    _uiState.value.selectedMessageIds.forEach { deleteMessage(it) }
                    _uiState.update { it.copy(isMultiSelectMode = false, selectedMessageIds = emptySet()) }
                }
            }
            is ChatIntent.CopySelectedMessages -> {
                val selectedMsgs = messages.value.filter { _uiState.value.selectedMessageIds.contains(it.id) }
                val content = selectedMsgs
                    .sortedBy { it.timestamp }
                    .joinToString("\n") { it.content }
                viewModelScope.launch { 
                    _effects.send(ChatEffect.CopyToClipboard(content))
                    _effects.send(ChatEffect.ShowSnackbar("Copied ${selectedMsgs.size} message(s)"))
                    _uiState.update { it.copy(isMultiSelectMode = false, selectedMessageIds = emptySet()) }
                }
            }
            is ChatIntent.ForwardSelectedMessages -> {
                // Load chats for forwarding, UI will handle showing the sheet
                loadUserChats()
            }
            // Media picker intents
            is ChatIntent.ShowMediaPicker -> {
                _uiState.update { it.copy(showMediaPicker = true) }
            }
            is ChatIntent.HideMediaPicker -> {
                _uiState.update { it.copy(showMediaPicker = false) }
            }
            is ChatIntent.AddPendingAttachment -> {
                val pending = PendingAttachment(
                    id = "pending_${System.currentTimeMillis()}",
                    uri = intent.uri,
                    type = intent.type
                )
                _uiState.update { 
                    it.copy(pendingAttachments = it.pendingAttachments + pending)
                }
            }
            is ChatIntent.RemovePendingAttachment -> {
                _uiState.update {
                    it.copy(pendingAttachments = it.pendingAttachments.filter { p -> p.id != intent.id })
                }
            }
            is ChatIntent.ClearPendingAttachments -> {
                _uiState.update { it.copy(pendingAttachments = emptyList()) }
            }
            is ChatIntent.SendWithAttachments -> {
                sendMediaMessage()
            }
            else -> { /* TODO: Implement other intents */ }
        }
    }

    /**
     * Send media message with pending attachments
     */
    private fun sendMediaMessage() {
        val chatId = currentChatId ?: return
        val senderId = currentUserId ?: return
        val pending = _uiState.value.pendingAttachments
        
        if (pending.isEmpty()) return
        
        val caption = _uiState.value.inputText.text.takeIf { it.isNotBlank() }
        val tempId = "temp_${System.currentTimeMillis()}"
        
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingMedia = true) }
            
            // Upload each attachment
            val uploadedUrls = mutableListOf<String>()
            pending.forEach { attachment ->
                try {
                    val bytes = getApplication<Application>().contentResolver
                        .openInputStream(attachment.uri)?.use { it.readBytes() }
                        ?: return@forEach
                    
                    val fileName = "upload_${System.currentTimeMillis()}"
                    val path = storageService.generateStoragePath(chatId, fileName)
                    
                    storageService.uploadFileBytes(bytes, path)
                        .onSuccess { url: String -> uploadedUrls.add(url) }
                }  catch (e: Exception) {
                    // Log error but continue
                }
            }
            
            if (uploadedUrls.isNotEmpty()) {
                // Send message with first attachment URL (or all as content for now)
                val type = when (pending.first().type) {
                    AttachmentType.Image -> "image"
                    AttachmentType.Video -> "video"
                    AttachmentType.Audio -> "audio"
                    else -> "file"
                }
                
                val content = caption ?: uploadedUrls.first()
                
                // DEBUG: Log image message sending details
                android.util.Log.d("DirectChatViewModel", "=== SENDING IMAGE MESSAGE ===")
                android.util.Log.d("DirectChatViewModel", "Message type: $type")
                android.util.Log.d("DirectChatViewModel", "Content: $content")
                android.util.Log.d("DirectChatViewModel", "Uploaded URLs: $uploadedUrls")
                android.util.Log.d("DirectChatViewModel", "Pending attachments count: ${pending.size}")
                
                chatRepository.sendMessage(
                    chatId = chatId,
                    senderId = senderId,
                    content = content,
                    messageType = type
                )
                
                _effects.send(ChatEffect.ShowSnackbar("Media sent"))
            } else {
                _effects.send(ChatEffect.ShowSnackbar("Failed to upload media"))
            }
            
            _uiState.update { 
                it.copy(
                    pendingAttachments = emptyList(),
                    isUploadingMedia = false,
                    inputText = androidx.compose.ui.text.input.TextFieldValue(""),
                    showMediaPicker = false
                )
            }
        }
    }

    fun loadUserChats() {
        // Prevent multiple identical requests or leaks
        if (loadChatsJob?.isActive == true) return

        loadChatsJob = viewModelScope.launch {
            // We only need a one-time snapshot for the dialog, or we can keep observing.
            // Observing is safer for consistency, but we must ensure we don't duplicate subscriptions.
            // Using take(1) to get the current list and stop.
            // But if the user adds a new chat while dialog is open, it won't show. That's acceptable for now.
            // To support live updates, we would need to persist the subscription.
            // Given the dialog is ephemeral, a snapshot is fine.

            chatRepository.getUserChats()
                .take(1)
                .collect { result ->
                     result.onSuccess { chats ->
                         val currentUid = currentUserId ?: return@collect

                         // Optimize: Bulk fetch participants for all direct chats
                         val directChats = chats.filter { !it.isGroup }
                         val directChatIds = directChats.map { it.id }

                         val participantsMap = if (directChatIds.isNotEmpty()) {
                             chatRepository.getParticipantsForChats(directChatIds).getOrDefault(emptyMap())
                         } else {
                             emptyMap()
                         }

                         // Optimize: Bulk fetch profiles for all other users
                         val otherUserIds = participantsMap.values.flatten().filter { it != currentUid }.distinct()
                         if (otherUserIds.isNotEmpty()) {
                             UserProfileManager.getUserProfiles(otherUserIds)
                         }

                         // Enrich with display names and avatars using parallel fetching
                         // Now that profiles are likely cached, this should be fast
                         val deferredChats = chats.map { chat ->
                             async {
                                 if (chat.isGroup) {
                                     ChatForwardUiModel(
                                         id = chat.id,
                                         displayName = chat.name ?: "Group Chat",
                                         avatarUrl = chat.avatarUrl,
                                         isGroup = true
                                     )
                                 } else {
                                     // Direct Chat: We need to find the OTHER user.

                                     var displayName = "Unknown User"
                                     var avatarUrl: String? = null

                                     try {
                                         // Use pre-fetched participants map
                                         val chatParticipants = participantsMap[chat.id] ?: emptyList()
                                         val otherUserId = chatParticipants.firstOrNull { it != currentUid }

                                         if (otherUserId != null) {
                                             // Should hit cache now
                                             val profile = UserProfileManager.getUserProfile(otherUserId)
                                             displayName = profile?.displayName ?: profile?.username ?: "User"
                                             avatarUrl = profile?.avatar
                                         }
                                     } catch (e: Exception) {
                                         // Ignore errors
                                     }

                                     ChatForwardUiModel(
                                         id = chat.id,
                                         displayName = displayName,
                                         avatarUrl = avatarUrl,
                                         isGroup = false
                                     )
                                 }
                             }
                         }

                         _availableChats.value = deferredChats.awaitAll()
                     }
                }
        }
    }

    private fun forwardMessage(originalMessageId: String, toChatIds: List<String>) {
        val currentUserId = currentUserId ?: return
        val currentChatId = currentChatId ?: return

        viewModelScope.launch {
            // 1. Find the original message content
            // First look in memory
            var messageToForward = messages.value.find { it.id == originalMessageId }

            // If not found in memory (e.g., scrolled out or partial load), fetch from repository
            if (messageToForward == null) {
                 // We need to fetch it. Since repository.getMessage(id) is not explicitly available in my snippet,
                 // I will iterate on current logic or assume we can't forward what we can't see?
                 // Wait, I can use the list I already have in repository cache if any, or just fail gracefully.
                 // Ideally, ChatRepository should expose `getMessage(id)`.
                 // Let's try to fetch it if we can, or just report error.

                 // Fallback: Check if we can find it in the full list if we have access to it,
                 // but 'messages' flow is the source of truth for UI.

                 // If the user long-pressed it, it MUST be in the UI list.
                 // The only edge case is if the list updated concurrently and removed it (unlikely).
                 // So the `find` should work 99% of time.

                 // However, to be robust, let's inform user.
                 _effects.send(ChatEffect.ShowSnackbar("Message no longer available"))
                 return@launch
            }

            // 2. Send to each chat
            var successCount = 0
            toChatIds.forEach { targetChatId ->
                // Determine type based on messageToForward
                val type = when(messageToForward.messageType) {
                    MessageType.Image -> "image"
                    MessageType.Video -> "video"
                    MessageType.Voice -> "audio"
                    else -> "text"
                }

                val content = if (type == "text") messageToForward.content else {
                    // If media, we need the URL.
                    // Prioritize the attachment URL, otherwise fall back to content (which might be the URL in some cases).
                    // Note: If the original message had a caption (stored in content), it is currently lost
                    // when forwarding as a media message because sendMessage only accepts one content string (URL or text).
                    // Future improvement: Support sending both caption and media URL.
                    messageToForward.attachments?.firstOrNull()?.url ?: messageToForward.content
                }

                val result = chatRepository.sendMessage(
                    chatId = targetChatId,
                    senderId = currentUserId,
                    content = content,
                    messageType = type
                )

                if (result.isSuccess) successCount++
            }

            if (successCount > 0) {
                 _effects.send(ChatEffect.ShowSnackbar("Forwarded to $successCount chat(s)"))
            } else {
                 _effects.send(ChatEffect.ShowSnackbar("Failed to forward message"))
            }
        }
    }
    
    private fun sendMessage(content: String, type: String = "text") {
        val chatId = currentChatId ?: return
        val senderId = currentUserId ?: return
        
        if (content.isBlank()) return
        
        val replyToId = _uiState.value.replyTo?.id
        val tempId = "temp_${System.currentTimeMillis()}"

        viewModelScope.launch {
            // 1. Trigger coordinated animation sequence
            _uiState.update { it.copy(isSendingAnimation = true) }
            
            // 2. Optimistic Update: Add to optimistic state
            val optimisticMessage = MessageUiModel(
                id = tempId,
                content = content,
                messageType = mapMessageType(type),
                senderId = senderId,
                senderName = "Me",
                senderAvatarUrl = null,
                timestamp = System.currentTimeMillis(),
                formattedTime = formatTime(System.currentTimeMillis()),
                isFromCurrentUser = true,
                deliveryStatus = DeliveryStatus.Sending,
                position = MessagePosition.Single,
                replyTo = _uiState.value.replyTo?.let {
                    ReplyPreviewData(
                        messageId = it.id,
                        senderName = it.senderName ?: "User",
                        content = it.content
                    )
                },
                isAnimating = true // Mark as newly sent for animation
            )
            
            _optimisticMessages.update { it + optimisticMessage }
            _uiState.update { it.copy(inputText = TextFieldValue(""), replyTo = null) }
            
            // 3. Reset animation flag after input clear animation completes
            delay(100) // Match SendInputClearDuration
            _uiState.update { it.copy(isSendingAnimation = false) }
            
            // 4. Network send
            val result = chatRepository.sendMessage(
                chatId = chatId,
                senderId = senderId,
                content = content,
                messageType = type,
                replyToId = replyToId
            )
            
            result.onSuccess { realMessageId ->
                // Success: Update the optimistic message with the Real ID.
                _optimisticMessages.update { list -> 
                    list.map { 
                        if (it.id == tempId) it.copy(id = realMessageId, deliveryStatus = DeliveryStatus.Sent, isAnimating = false) else it 
                    } 
                }
            }.onFailure { error ->
                // Mark optimistic message as failed
                _optimisticMessages.update { list ->
                    list.map {
                        if (it.id == tempId) it.copy(deliveryStatus = DeliveryStatus.Failed, isAnimating = false) else it
                    }
                }
                _uiState.update { it.copy(error = "Failed to send: ${error.message}") }
            }
        }
    }

    private fun deleteMessage(messageId: String) {
        viewModelScope.launch {
             chatRepository.deleteMessage(messageId).onSuccess {
                 // Optimistic update handled by Realtime if available, or force refresh
                 // For now, let's wait for realtime. Or we can manually remove from _dbMessages if needed.
                 _effects.send(ChatEffect.ShowSnackbar("Message deleted"))
             }.onFailure { e ->
                 _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
             }
        }
    }

    private fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            chatRepository.editMessage(messageId, newContent).onSuccess {
                _effects.send(ChatEffect.ShowSnackbar("Message edited"))
            }.onFailure { e ->
                 _uiState.update { it.copy(error = "Failed to edit: ${e.message}") }
            }
        }
    }

    // Mapper function
    private fun Message.toUiModel(currentUserId: String?): MessageUiModel {
        val isMe = this.senderId == currentUserId

        // DEBUG: Log message conversion details
        android.util.Log.d("DirectChatViewModel", "=== CONVERTING MESSAGE TO UI MODEL ===")
        android.util.Log.d("DirectChatViewModel", "Message ID: ${this.id}")
        android.util.Log.d("DirectChatViewModel", "Message type: ${this.messageType}")
        android.util.Log.d("DirectChatViewModel", "Content: ${this.content}")
        android.util.Log.d("DirectChatViewModel", "Attachments count: ${this.attachments?.size ?: 0}")
        android.util.Log.d("DirectChatViewModel", "Is media message: ${this.isMediaMessage()}")

        // Map Reply Preview if exists
        val replyPreview = if (this.replyToId != null) {
            // In a real app, we'd need to fetch the original message or look it up in cache
            // For now, simplistic approximation or null
             null
        } else null

        // Handle attachments: Use existing list OR create synthetic one from content if it's a media message
        val uiAttachments = if (!this.attachments.isNullOrEmpty()) {
            android.util.Log.d("DirectChatViewModel", "Using existing attachments: ${this.attachments.size}")
            this.attachments.map {
                AttachmentUiModel(
                    id = it.id,
                    url = it.url,
                    type = mapAttachmentType(it.type),
                    thumbnailUrl = it.thumbnailUrl,
                    fileName = it.fileName,
                    fileSize = it.fileSize
                )
            }
        } else if (this.isMediaMessage()) {
            android.util.Log.d("DirectChatViewModel", "Creating synthetic attachment from content for media message")
            // Create synthetic attachment for any media message, regardless of URL format
            listOf(AttachmentUiModel(
                id = this.id, // Use message ID for attachment ID
                url = this.content,
                type = mapAttachmentType(this.messageType),
                fileName = "Media", // Generic name
                fileSize = 0L,
                thumbnailUrl = null // No thumbnail available
            ))
        } else {
            android.util.Log.d("DirectChatViewModel", "No attachments found for message")
            emptyList()
        }

        android.util.Log.d("DirectChatViewModel", "Final UI attachments count: ${uiAttachments.size}")

        return MessageUiModel(
            id = this.id,
            content = this.content,
            messageType = mapMessageType(this.messageType),
            senderId = this.senderId,
            senderName = this.senderName ?: "User",
            senderAvatarUrl = this.senderAvatarUrl,
            timestamp = this.createdAt,
            formattedTime = formatTime(this.createdAt),
            isFromCurrentUser = isMe,
            deliveryStatus = DeliveryStatus.Read, // Placeholder
            position = MessagePosition.Single,
            replyTo = replyPreview,
            attachments = uiAttachments
        )
    }

    private fun mapMessageType(type: String): MessageType {
        return when (type) {
            "text" -> MessageType.Text
            "image" -> MessageType.Image
            "video" -> MessageType.Video
            "audio" -> MessageType.Voice
            else -> MessageType.Text
        }
    }

    private fun mapAttachmentType(type: String): AttachmentType {
        return when (type) {
            "image" -> AttachmentType.Image
            "video" -> AttachmentType.Video
            "audio" -> AttachmentType.Audio
            "document" -> AttachmentType.Document
            else -> AttachmentType.Unknown
        }
    }

    private fun formatTime(timestamp: Long): String {
        // Simple formatter
        val date = java.util.Date(timestamp)
        return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(date)
    }

    // ============================================================================================
    // TODO: FUTURE FEATURES IMPLEMENTATION
    // ============================================================================================

    /**
     * Block User
     */
    fun blockUser(userId: String) {
        val currentUserId = currentUserId ?: return
        viewModelScope.launch {
            _effects.send(ChatEffect.ShowSnackbar("Blocking user..."))
            val result = chatRepository.blockUser(blockerId = currentUserId, blockedId = userId)
            result.onSuccess {
                _effects.send(ChatEffect.ShowSnackbar("User blocked"))
                // Optionally navigate back or refresh
            }.onFailure {
                _effects.send(ChatEffect.ShowSnackbar("Failed to block: ${it.message}"))
            }
        }
    }

    /**
     * Report User
     */
    /**
     * Report User
     */
    fun reportUser(userId: String, reason: String) {
        val currentUserId = currentUserId ?: return
        viewModelScope.launch {
             _effects.send(ChatEffect.ShowSnackbar("Reporting user..."))
             val result = chatRepository.reportUser(reporterId = currentUserId, reportedId = userId, reason = reason)
             result.onSuccess {
                 _effects.send(ChatEffect.ShowSnackbar("User reported"))
             }.onFailure {
                 _effects.send(ChatEffect.ShowSnackbar("Failed to report: ${it.message}"))
             }
        }
    }

    /**
     * Delete Chat
     */
    fun deleteChat() {
        val chatId = currentChatId ?: return
        val currentUserId = currentUserId ?: return
        
        viewModelScope.launch {
            _effects.send(ChatEffect.ShowSnackbar("Deleting chat..."))
            val result = chatRepository.deleteChat(chatId, currentUserId)
            result.onSuccess {
                _effects.send(ChatEffect.ShowSnackbar("Chat deleted"))
                _effects.send(ChatEffect.NavigateBack) 
            }.onFailure {
                _effects.send(ChatEffect.ShowSnackbar("Failed to delete chat: ${it.message}"))
            }
        }
    }

    /**
     * TODO: Implement Attachment Upload
     * Backend Context:
     * 1. Supabase Storage: Use bucket 'chat-attachments'.
     * 2. Path structure: '{chat_id}/{timestamp}_{filename}'.
     */

    /**
     * Set Typing Status
     * Enhanced with TypingIndicatorManager for debouncing and lifecycle management
     */
    fun setTypingStatus(isTyping: Boolean) {
        val chatId = currentChatId ?: return
        val userId = currentUserId ?: return
        
        if (isTyping) {
            typingIndicatorManager.startTyping(chatId, userId)
        } else {
            typingIndicatorManager.stopTyping(chatId, userId)
        }
        
        // Also broadcast via realtime service for immediate feedback
        viewModelScope.launch {
            realtimeService.broadcastTyping(chatId, userId, isTyping)
        }
    }
    
    /**
     * Initialize presence tracking for the current chat
     */
    fun initializePresenceTracking() {
        val chatId = currentChatId ?: return
        val userId = currentUserId ?: return
        val otherUserId = _uiState.value.otherUser?.id ?: return
        
        viewModelScope.launch {
            // Set user as online and in this chat
            activeStatusManager.setOnline(userId)
            activeStatusManager.setActivityStatus(userId, "chatting", chatId)
            
            // Start heartbeat to maintain online status
            activeStatusManager.startHeartbeat(userId)
            
            // Monitor other user's presence
            activeStatusManager.addPresenceListener(otherUserId, object : 
                com.synapse.social.studioasinc.chat.ActiveStatusManager.PresenceListener {
                override fun onPresenceChanged(userId: String, presence: com.synapse.social.studioasinc.chat.ActiveStatusManager.UserPresence) {
                    _uiState.update { state ->
                        state.copy(
                            otherUserOnline = presence.isOnline,
                            otherUserLastSeen = presence.lastSeen,
                            otherUserActivity = presence.activityStatus
                        )
                    }
                }
            })
            
            // Start monitoring presence for other user
            activeStatusManager.startMonitoring(listOf(otherUserId))
            
            // Set up typing indicator listener
            typingIndicatorManager.addTypingListener(chatId, object :
                com.synapse.social.studioasinc.chat.TypingIndicatorManager.TypingListener {
                override fun onTypingUsersChanged(chatId: String, typingUsers: List<String>) {
                    _uiState.update { state ->
                        val displayNames = typingUsers.mapNotNull { userId ->
                            if (userId == otherUserId) state.otherUser?.username else null
                        }
                        state.copy(typingUsers = displayNames)
                    }
                }
            })
            
            // Start monitoring typing status
            typingIndicatorManager.startMonitoring(chatId, userId)
        }
    }
    
    /**
     * Clean up presence tracking
     */
    fun cleanupPresenceTracking() {
        val chatId = currentChatId ?: return
        val userId = currentUserId ?: return
        val otherUserId = _uiState.value.otherUser?.id ?: return
        
        viewModelScope.launch {
            // Set user as online but not in chat
            activeStatusManager.setActivityStatus(userId, "online")
            
            // Clean up managers
            typingIndicatorManager.cleanup(chatId, userId)
            activeStatusManager.removePresenceListener(otherUserId, object : 
                com.synapse.social.studioasinc.chat.ActiveStatusManager.PresenceListener {
                override fun onPresenceChanged(userId: String, presence: com.synapse.social.studioasinc.chat.ActiveStatusManager.UserPresence) {}
            })
        }
    }

    /**
     * Set screen active state for read receipt management
     */
    fun setScreenActive(isActive: Boolean) {
        _isScreenActive.value = isActive
        
        // Mark messages as read when screen becomes active
        if (isActive && currentChatId != null && currentUserId != null) {
            viewModelScope.launch {
                chatRepository.markMessagesAsRead(currentChatId!!, currentUserId!!)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupPresenceTracking()
        viewModelScope.launch {
            realtimeService.cleanup()
        }
    }
}
