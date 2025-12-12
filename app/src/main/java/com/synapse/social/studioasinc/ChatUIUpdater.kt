package com.synapse.social.studioasinc

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.format.DateTimeParseException

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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updatesJob: Job? = null
    private var channel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    fun startUpdates(chatId: String) {
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
                    // TODO: Add filter when API is available - filter = "chat_id=eq.$chatId"
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

        // Cancel the specific job for updates
        updatesJob?.cancel()
        updatesJob = null

        scope.launch {
            try {
                channel?.unsubscribe()
                channel = null
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping updates", e)
            }
        }
    }

    // Add a destroy method for final cleanup
    fun destroy() {
        stopUpdates()
        scope.cancel()
    }
    
    fun initializeWithMessages(messages: List<HashMap<String, Any?>>) {
        messagesList.clear()
        messagesList.addAll(messages)
        adapter.notifyDataSetChanged()
        
        if (messages.isNotEmpty()) {
            // Scroll to bottom (newest)
            recyclerView.scrollToPosition(messagesList.size - 1)
        }
    }
    
    fun addMessageImmediately(messageData: HashMap<String, Any?>) {
        messagesList.add(messageData)
        adapter.notifyItemInserted(messagesList.size - 1)
        recyclerView.scrollToPosition(messagesList.size - 1)
    }

    private fun handleNewMessage(record: JsonObject) {
        // Convert JsonObject to HashMap for compatibility
        val messageData = HashMap<String, Any?>()
        
        record.forEach { (key, value) ->
            messageData[key] = when {
                value.jsonPrimitive.isString -> value.jsonPrimitive.content
                else -> value.toString()
            }
        }
        
        // Add to messages list and notify adapter
        messagesList.add(messageData)
        adapter.notifyItemInserted(messagesList.size - 1)
        recyclerView.scrollToPosition(messagesList.size - 1)
    }
}
