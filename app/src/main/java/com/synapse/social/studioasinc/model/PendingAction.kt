package com.synapse.social.studioasinc.model

/**
 * Represents a pending message action that needs to be executed when online
 */
data class PendingAction(
    val id: String,
    val actionType: ActionType,
    val messageId: String,
    val parameters: Map<String, Any?>,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
) {
    enum class ActionType {
        EDIT,
        DELETE,
        FORWARD
    }
}
