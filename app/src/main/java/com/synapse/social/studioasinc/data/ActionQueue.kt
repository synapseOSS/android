package com.synapse.social.studioasinc.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synapse.social.studioasinc.domain.model.PendingAction
import java.util.UUID

/**
 * Manages a queue of pending message actions using SharedPreferences
 * Actions are queued when offline and processed when connection is restored
 */
class ActionQueue(context: Context) {

    companion object {
        private const val TAG = "ActionQueue"
        private const val PREFS_NAME = "action_queue_prefs"
        private const val KEY_PENDING_ACTIONS = "pending_actions"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Add a pending action to the queue
     * @param action The PendingAction to add
     */
    fun add(action: PendingAction) {
        try {
            Log.d(TAG, "Adding action to queue: ${action.actionType} for message ${action.messageId}")

            val currentActions = getAll().toMutableList()
            currentActions.add(action)

            saveActions(currentActions)

            Log.d(TAG, "Action added to queue. Total queued actions: ${currentActions.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding action to queue", e)
        }
    }

    /**
     * Remove a pending action from the queue
     * @param actionId The ID of the action to remove
     */
    fun remove(actionId: String) {
        try {
            Log.d(TAG, "Removing action from queue: $actionId")

            val currentActions = getAll().toMutableList()
            val removed = currentActions.removeIf { it.id == actionId }

            if (removed) {
                saveActions(currentActions)
                Log.d(TAG, "Action removed from queue. Remaining actions: ${currentActions.size}")
            } else {
                Log.w(TAG, "Action not found in queue: $actionId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing action from queue", e)
        }
    }

    /**
     * Get all pending actions from the queue
     * @return List of PendingAction objects
     */
    fun getAll(): List<PendingAction> {
        return try {
            val json = prefs.getString(KEY_PENDING_ACTIONS, null)
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<PendingAction>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all actions from queue", e)
            emptyList()
        }
    }

    /**
     * Update an existing action in the queue (e.g., to increment retry count)
     * @param action The updated PendingAction
     */
    fun update(action: PendingAction) {
        try {
            Log.d(TAG, "Updating action in queue: ${action.id}")

            val currentActions = getAll().toMutableList()
            val index = currentActions.indexOfFirst { it.id == action.id }

            if (index != -1) {
                currentActions[index] = action
                saveActions(currentActions)
                Log.d(TAG, "Action updated in queue")
            } else {
                Log.w(TAG, "Action not found in queue for update: ${action.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating action in queue", e)
        }
    }

    /**
     * Clear all pending actions from the queue
     */
    fun clear() {
        try {
            Log.d(TAG, "Clearing all actions from queue")
            prefs.edit().remove(KEY_PENDING_ACTIONS).apply()
            Log.d(TAG, "Action queue cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing action queue", e)
        }
    }

    /**
     * Get the count of pending actions
     * @return Number of pending actions
     */
    fun size(): Int {
        return getAll().size
    }

    /**
     * Check if the queue is empty
     * @return true if queue is empty, false otherwise
     */
    fun isEmpty(): Boolean {
        return getAll().isEmpty()
    }

    /**
     * Save actions list to SharedPreferences
     */
    private fun saveActions(actions: List<PendingAction>) {
        try {
            val json = gson.toJson(actions)
            prefs.edit().putString(KEY_PENDING_ACTIONS, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving actions to queue", e)
        }
    }

    /**
     * Create a new PendingAction for editing a message
     */
    fun createEditAction(messageId: String, newContent: String): PendingAction {
        return PendingAction(
            id = UUID.randomUUID().toString(),
            actionType = PendingAction.ActionType.EDIT,
            messageId = messageId,
            parameters = mapOf("newContent" to kotlinx.serialization.json.JsonPrimitive(newContent))
        )
    }

    /**
     * Create a new PendingAction for deleting a message
     */
    fun createDeleteAction(messageId: String, deleteForEveryone: Boolean): PendingAction {
        return PendingAction(
            id = UUID.randomUUID().toString(),
            actionType = PendingAction.ActionType.DELETE,
            messageId = messageId,
            parameters = mapOf("deleteForEveryone" to kotlinx.serialization.json.JsonPrimitive(deleteForEveryone))
        )
    }

    /**
     * Create a new PendingAction for forwarding a message
     */
    fun createForwardAction(
        messageId: String,
        messageData: Map<String, Any?>,
        targetChatIds: List<String>
    ): PendingAction {
        return PendingAction(
            id = UUID.randomUUID().toString(),
            actionType = PendingAction.ActionType.FORWARD,
            messageId = messageId,
            parameters = mapOf(
                "messageData" to kotlinx.serialization.json.JsonPrimitive(gson.toJson(messageData)),
                "targetChatIds" to kotlinx.serialization.json.JsonPrimitive(gson.toJson(targetChatIds))
            )
        )
    }
}
