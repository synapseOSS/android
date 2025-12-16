package com.synapse.social.studioasinc.chat.service

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Service for managing message selection using new BaaS tables:
 * - message_selection_sessions
 * - message_actions_history
 */
class MessageSelectionService {
    
    private val supabase = SupabaseClient.client
    
    // Local state for current selection session
    private val _currentSession = MutableStateFlow<MessageSelectionSession?>(null)
    val currentSession: StateFlow<MessageSelectionSession?> = _currentSession.asStateFlow()
    
    private val _selectedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessageIds: StateFlow<Set<String>> = _selectedMessageIds.asStateFlow()
    
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    
    /**
     * Start a new message selection session
     */
    suspend fun startSelectionSession(userId: String, chatId: String): String {
        val sessionId = UUID.randomUUID().toString()
        
        val session = MessageSelectionSessionDto(
            id = sessionId,
            user_id = userId,
            chat_id = chatId,
            selected_message_ids = emptyList(),
            selection_mode = true
        )
        
        supabase.from("message_selection_sessions").insert(session)
        
        _currentSession.value = MessageSelectionSession(
            id = sessionId,
            userId = userId,
            chatId = chatId,
            selectedMessageIds = emptySet(),
            isActive = true
        )
        _isSelectionMode.value = true
        
        return sessionId
    }
    
    /**
     * Toggle message selection
     */
    suspend fun toggleMessageSelection(messageId: String) {
        val session = _currentSession.value ?: return
        val currentIds = _selectedMessageIds.value
        
        val newIds = if (currentIds.contains(messageId)) {
            currentIds - messageId
        } else {
            if (currentIds.size >= 50) return // Limit to 50 selections
            currentIds + messageId
        }
        
        _selectedMessageIds.value = newIds
        
        // Update session in database
        supabase.from("message_selection_sessions")
            .update(mapOf("selected_message_ids" to newIds.toList())) {
                filter {
                    eq("id", session.id)
                }
            }
    }
    
    /**
     * Execute action on selected messages
     */
    suspend fun executeAction(
        action: MessageAction,
        userId: String,
        additionalData: Map<String, @Contextual Any> = emptyMap()
    ) {
        val session = _currentSession.value ?: return
        val selectedIds = _selectedMessageIds.value
        
        if (selectedIds.isEmpty()) return
        
        // Record action in history
        val actionHistory = MessageActionHistoryDto(
            user_id = userId,
            action_type = action.name.lowercase(),
            message_ids = selectedIds.toList(),
            chat_id = session.chatId,
            action_data = additionalData
        )
        
        supabase.from("message_actions_history").insert(actionHistory)
        
        // Clear selection after action
        endSelectionSession()
    }
    
    /**
     * End current selection session
     */
    suspend fun endSelectionSession() {
        val session = _currentSession.value ?: return
        
        // Update session as inactive
        supabase.from("message_selection_sessions")
            .update(mapOf("selection_mode" to false)) {
                filter {
                    eq("id", session.id)
                }
            }
        
        // Clear local state
        _currentSession.value = null
        _selectedMessageIds.value = emptySet()
        _isSelectionMode.value = false
    }
    
    /**
     * Get action history for analytics
     */
    suspend fun getActionHistory(userId: String, limit: Int = 20): List<MessageActionHistory> {
        val response = supabase.from("message_actions_history")
            .select() {
                filter {
                    eq("user_id", userId)
                }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<MessageActionHistoryDto>()
        
        return response.map { it.toDomain() }
    }
}

// Domain Models
data class MessageSelectionSession(
    val id: String,
    val userId: String,
    val chatId: String,
    val selectedMessageIds: Set<String>,
    val isActive: Boolean
)

data class MessageActionHistory(
    val id: String,
    val userId: String,
    val actionType: String,
    val messageIds: List<String>,
    val chatId: String?,
    val actionData: Map<String, @Contextual Any>,
    val createdAt: String
)

enum class MessageAction {
    DELETE, COPY, FORWARD, STAR, UNSTAR
}

// DTOs for Supabase
@Serializable
data class MessageSelectionSessionDto(
    val id: String,
    val user_id: String,
    val chat_id: String,
    val selected_message_ids: List<String>,
    val selection_mode: Boolean
)

@Serializable
data class MessageActionHistoryDto(
    val id: String? = null,
    val user_id: String,
    val action_type: String,
    val message_ids: List<String>,
    val chat_id: String?,
    val action_data: Map<String, @Contextual Any>,
    val created_at: String? = null
) {
    fun toDomain() = MessageActionHistory(
        id = id ?: "",
        userId = user_id,
        actionType = action_type,
        messageIds = message_ids,
        chatId = chat_id,
        actionData = action_data,
        createdAt = created_at ?: ""
    )
}
