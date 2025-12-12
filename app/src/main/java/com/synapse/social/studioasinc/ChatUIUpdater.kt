package com.synapse.social.studioasinc

import android.content.Context
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

class ChatUIUpdater(
    private val context: Context,
    private val chatId: String,
    private val recyclerView: RecyclerView,
    private val adapter: RecyclerView.Adapter<*>,
    private val messagesList: ArrayList<HashMap<String, Any?>>
) {
    
    companion object {
        private const val TAG = "ChatUIUpdater"
    }
    
    private var statusTextView: TextView? = null

    // Dependencies for real-time updates
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var updatesJob: Job? = null

    fun setStatusTextView(textView: TextView?) {
        statusTextView = textView
    }
    
    fun startUpdates() {
        Log.d(TAG, "Starting chat UI updates for chat: $chatId")

        // Cancel any existing updates job
        updatesJob?.cancel()

        updatesJob = scope.launch {
            try {
                // Subscribe to the chat channel directly using SupabaseClient
                val channel = SupabaseClient.client.realtime.channel("chat:$chatId")
                channel.subscribe()

                // Listen for new messages
                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                    filter = "chat_id=eq.$chatId"
                }.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            handleNewMessage(action.record)
                        }
                        else -> {
                            // Handle other actions (Update, Delete) if needed
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in chat UI updates", e)
            }
        }
    }

    fun stopUpdates() {
        Log.d(TAG, "Stopping chat UI updates")

        // Cancel the updates job to stop listening
        updatesJob?.cancel()
        updatesJob = null

        // Unsubscribe from the channel
        scope.launch {
            try {
                // We recreate the channel object to unsubscribe as we don't hold a reference to the one in startUpdates.
                // Supabase Realtime client manages channels by topic.
                val channel = SupabaseClient.client.realtime.channel("chat:$chatId")
                channel.unsubscribe()
                SupabaseClient.client.realtime.removeChannel(channel)
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing from chat", e)
            }
        }

        // Note: We do NOT cancel the scope here to allow restarting updates.
        // If the ChatUIUpdater is being destroyed, the caller should ensure the scope is cleaned up
        // or we should add a destroy() method. For stopUpdates, just stopping the job is enough.
    }

    // Add a destroy method for final cleanup if needed
    fun destroy() {
        stopUpdates()
        scope.cancel()
    }

    private fun handleNewMessage(record: JsonObject) {
        try {
            val messageId = record["id"]?.toString()?.removeSurrounding("\"") ?: return

            // Check if message already exists (deduplication)
            val exists = messagesList.any { it["id"] == messageId }
            if (exists) return

            val newMessage = HashMap<String, Any?>()
            newMessage["id"] = messageId
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

            // Add to list and update UI
            messagesList.add(newMessage)
            adapter.notifyItemInserted(messagesList.size - 1)
            recyclerView.scrollToPosition(messagesList.size - 1)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling new message", e)
        }
    }
    
    fun initializeWithMessages(messages: List<HashMap<String, Any?>>) {
        messagesList.clear()
        messagesList.addAll(messages)
        adapter.notifyDataSetChanged()
        
        if (messages.isNotEmpty()) {
            recyclerView.scrollToPosition(0)
        }
    }
    
    fun addMessageImmediately(messageData: HashMap<String, Any?>) {
        messagesList.add(0, messageData)
        adapter.notifyItemInserted(0)
        recyclerView.scrollToPosition(0)
    }
}
