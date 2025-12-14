package com.synapse.social.studioasinc.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.LinkPreviewService
import com.synapse.social.studioasinc.chat.service.SupabaseRealtimeService
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.model.Message
import com.synapse.social.studioasinc.model.Chat
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.util.LinkDetectionService
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

/**
 * ViewModel for DirectChatScreen
 * Adapter for existing ChatRepository to Compose UI State
 */
class DirectChatViewModel(application: Application) : AndroidViewModel(application) {

    // Dependencies
    private val chatDao = AppDatabase.getDatabase(application).chatDao()
    private val chatRepository = ChatRepository(chatDao)
    private val searchRepository = com.synapse.social.studioasinc.data.repository.SearchRepositoryImpl()
    private val authService = SupabaseAuthenticationService(application)
    
    // UI State
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Available chats for forwarding
    private val _availableChats = MutableStateFlow<List<ChatForwardUiModel>>(emptyList())
    val availableChats: StateFlow<List<ChatForwardUiModel>> = _availableChats.asStateFlow()

    private var loadChatsJob: Job? = null
    private var linkPreviewJob: Job? = null
    
    // Link Preview Service
    private val linkPreviewService = LinkPreviewService()
    
    // Messages list (Source of Truth: Realtime/DB)
    private val _dbMessages = MutableStateFlow<List<MessageUiModel>>(emptyList())
    
    // Storage Service
    private val storageService = com.synapse.social.studioasinc.backend.SupabaseStorageService()

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
    
    init {
        loadCurrentUser()
        observeConnectionState()
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
                    
                    // Fetch link previews for messages containing URLs
                    fetchLinkPreviewsForMessages(uiMessages)
                    
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
                                 avatarUrl = userProfile?.profileImageUrl
                             ))
                         }
                     }
                 }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    // Realtime Service
    private val realtimeService = SupabaseRealtimeService()

    /**
     * Start observing everything: Messages and Typing
     */
    private fun observeRealtimeMessages(chatId: String) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            // Variable to capture the flow
            var changesFlow: Flow<PostgresAction>? = null
            
            // 1. Subscribe to channel with configuration block
            val channel = realtimeService.subscribeToChat(chatId) { ch ->
                // Configure Postgres changes listener BEFORE subscription
                changesFlow = ch.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                    filter("chat_id", FilterOperator.EQ, chatId)
                }
            }

            // 2. Observe Messages (Insert/Update/Delete)
            launch {
                try {
                    // Use the flow captured during configuration, or set up a new one if channel was reused
                    val flowToCollect = changesFlow ?: channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "messages"
                        filter("chat_id", FilterOperator.EQ, chatId)
                    }

                    flowToCollect.collect { action ->
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
                    _uiState.update { it.copy(error = "Realtime message error: ${e.message}") }
                }
            }
            
            // 3. Observe Typing Events
            launch {
                 channel.broadcastFlow<JsonObject>(event = "typing")
                     .collect { event ->
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
        
        // Fetch link preview if message contains URL
        fetchLinkPreviewForMessage(uiMessage)
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

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage(intent.content)
            is ChatIntent.UpdateInputText -> {
                _uiState.update { it.copy(inputText = intent.text) }
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
                // Debounced link detection
                detectLinksDebounced(intent.text.text)
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
     * Debounced link detection - triggers link preview fetch after user stops typing
     */
    private fun detectLinksDebounced(text: String) {
        linkPreviewJob?.cancel()
        
        // Clear preview if text is empty or no links
        if (text.isBlank() || !LinkDetectionService.containsUrl(text)) {
            _uiState.update { it.copy(detectedLinkPreview = null, linkPreviewLoading = false) }
            return
        }
        
        linkPreviewJob = viewModelScope.launch {
            delay(500) // 500ms debounce
            
            val url = LinkDetectionService.extractFirstUrl(text) ?: return@launch
            
            // Don't refetch if same URL
            if (_uiState.value.detectedLinkPreview?.url == url) return@launch
            
            _uiState.update { it.copy(linkPreviewLoading = true) }
            
            linkPreviewService.fetchLinkPreview(url)
                .onSuccess { preview ->
                    _uiState.update { 
                        it.copy(detectedLinkPreview = preview, linkPreviewLoading = false)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(linkPreviewLoading = false) }
                }
        }
    }

    /**
     * Fetch link preview for a single message and update it in the message list
     */
    private fun fetchLinkPreviewForMessage(message: MessageUiModel) {
        // Skip if no URL or already has preview
        if (message.linkPreview != null) return
        
        val url = LinkDetectionService.extractFirstUrl(message.content) ?: return
        
        viewModelScope.launch {
            linkPreviewService.fetchLinkPreview(url)
                .onSuccess { preview ->
                    // Update the message with the fetched preview
                    _dbMessages.update { current ->
                        current.map { msg ->
                            if (msg.id == message.id) msg.copy(linkPreview = preview) else msg
                        }
                    }
                    // Also update optimistic messages if applicable
                    _optimisticMessages.update { current ->
                        current.map { msg ->
                            if (msg.id == message.id) msg.copy(linkPreview = preview) else msg
                        }
                    }
                }
        }
    }
    
    /**
     * Fetch link previews for all messages containing URLs (batch processing)
     */
    private fun fetchLinkPreviewsForMessages(messages: List<MessageUiModel>) {
        viewModelScope.launch {
            messages.forEach { message ->
                fetchLinkPreviewForMessage(message)
            }
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
                        .onSuccess { url -> uploadedUrls.add(url) }
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
                                             avatarUrl = profile?.profileImageUrl
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

        // Map Reply Preview if exists
        val replyPreview = if (this.replyToId != null) {
            // In a real app, we'd need to fetch the original message or look it up in cache
            // For now, simplistic approximation or null
             null
        } else null

        // Handle attachments: Use existing list OR create synthetic one from content if it's a media message
        val uiAttachments = if (!this.attachments.isNullOrEmpty()) {
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
        } else if (this.isMediaMessage() && this.content.startsWith("http")) {
            // Synthetic attachment from content URL
            listOf(AttachmentUiModel(
                id = this.id, // Use message ID for attachment ID
                url = this.content,
                type = mapAttachmentType(this.messageType),
                fileName = "Media", // Generic name
                fileSize = 0L,
                thumbnailUrl = null // No thumbnail available
            ))
        } else {
            emptyList()
        }

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
     * Backend: Broadcasts 'typing' event to 'chat:{id}' channel
     */
    fun setTypingStatus(isTyping: Boolean) {
        val chatId = currentChatId ?: return
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            realtimeService.broadcastTyping(chatId, userId, isTyping)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            realtimeService.cleanup()
        }
    }
}
