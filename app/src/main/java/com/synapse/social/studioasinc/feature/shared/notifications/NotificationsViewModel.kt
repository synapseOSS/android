package com.synapse.social.studioasinc.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<UiNotification> = emptyList(),
    val isLoading: Boolean = false,
    val unreadCount: Int = 0
)

class NotificationsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Implementing direct Supabase fetch since NotificationRepository is unavailable in current context
                // Adjust table name and columns based on schema
                val client = com.synapse.social.studioasinc.core.network.SupabaseClient.client
                val currentUserId = client.auth.currentUserOrNull()?.id

                if (currentUserId != null) {
                    val result = client.from("notifications")
                        .select {
                            filter { eq("receiver_id", currentUserId) }
                            order("created_at", Order.DESCENDING)
                        }
                        .decodeList<kotlinx.serialization.json.JsonObject>()

                    val notifications = result.mapNotNull { json ->
                        try {
                            UiNotification(
                                id = json["id"]?.toString()?.replace("\"", "") ?: "",
                                type = json["type"]?.toString()?.replace("\"", "") ?: "system",
                                actorName = "User",
                                actorAvatar = null,
                                message = json["content"]?.toString()?.replace("\"", "") ?: "New notification",
                                timestamp = json["created_at"]?.toString()?.replace("\"", "") ?: "",
                                isRead = json["is_read"]?.toString()?.toBoolean() ?: false,
                                targetId = json["target_id"]?.toString()?.replace("\"", "")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                     _uiState.update {
                        it.copy(
                            notifications = notifications,
                            isLoading = false,
                            unreadCount = notifications.count { !it.isRead }
                        )
                    }
                } else {
                     _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refresh() {
        loadNotifications()
    }

    fun markAsRead(notificationId: String) {
        // Mark as read API call
        // Optimistic update
        _uiState.update { state ->
            val updatedList = state.notifications.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            state.copy(notifications = updatedList)
        }
    }
}
