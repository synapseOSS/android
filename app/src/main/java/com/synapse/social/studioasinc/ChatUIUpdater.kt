package com.synapse.social.studioasinc

import android.content.Context
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
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
    
    private var statusTextView: TextView? = null
    private var channel: RealtimeChannel? = null

    // Use a persistent scope for the lifecycle of this class instance
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    // Track the update job specifically so we can cancel it without killing the scope
    private var updatesJob: Job? = null

    fun setStatusTextView(textView: TextView?) {
        statusTextView = textView
    }
    
    fun startUpdates() {
        if (channel != null || updatesJob?.isActive == true) {
            Log.d(TAG, "Updates already running for chat: $chatId")
            return
        }

        Log.d(TAG, "Starting chat UI updates for chat: $chatId")

        updatesJob = scope.launch {
            try {
                // SupabaseClient is in the same package
                val client = SupabaseClient.client
                val newChannel = client.realtime.channel("messages_$chatId")
                channel = newChannel

                val changeFlow = newChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                    eq("chat_id", chatId)
                }

                newChannel.subscribe()

                changeFlow.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> handleInsert(action.record)
                        is PostgresAction.Update -> handleUpdate(action.record)
                        is PostgresAction.Delete -> handleDelete(action.oldRecord)
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting realtime updates", e)
                channel = null // Reset channel on error
            }
        }
    }

    private suspend fun handleInsert(record: JsonObject) {
         withContext(Dispatchers.Main) {
             try {
                // Check if message already exists (deduplication)
                val id = record["id"]?.jsonPrimitive?.contentOrNull
                if (messagesList.any { it["id"] == id }) {
                    return@withContext
                }

                val messageMap = parseRecord(record)
                // Add to end of list (position size-1)
                messagesList.add(messageMap)
                adapter.notifyItemInserted(messagesList.size - 1)
                recyclerView.scrollToPosition(messagesList.size - 1)
             } catch (e: Exception) {
                 Log.e(TAG, "Error handling insert", e)
             }
         }
    }

    private suspend fun handleUpdate(record: JsonObject) {
        withContext(Dispatchers.Main) {
            try {
                val id = record["id"]?.jsonPrimitive?.contentOrNull ?: return@withContext
                val index = messagesList.indexOfFirst { it["id"] == id }
                if (index != -1) {
                    val messageMap = parseRecord(record)
                    messagesList[index] = messageMap
                    adapter.notifyItemChanged(index)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling update", e)
            }
        }
    }

    private suspend fun handleDelete(record: JsonObject) {
         withContext(Dispatchers.Main) {
             try {
                 val id = record["id"]?.jsonPrimitive?.contentOrNull ?: return@withContext
                 val index = messagesList.indexOfFirst { it["id"] == id }
                 if (index != -1) {
                     messagesList.removeAt(index)
                     adapter.notifyItemRemoved(index)
                 }
             } catch (e: Exception) {
                 Log.e(TAG, "Error handling delete", e)
             }
         }
    }

    private fun parseRecord(record: JsonObject): HashMap<String, Any?> {
        val map = HashMap<String, Any?>()

        map["id"] = record["id"]?.jsonPrimitive?.contentOrNull
        map["chat_id"] = record["chat_id"]?.jsonPrimitive?.contentOrNull
        map["sender_id"] = record["sender_id"]?.jsonPrimitive?.contentOrNull
        map["uid"] = record["sender_id"]?.jsonPrimitive?.contentOrNull // Compatibility
        map["content"] = record["content"]?.jsonPrimitive?.contentOrNull
        map["message_text"] = record["content"]?.jsonPrimitive?.contentOrNull // Compatibility
        map["message_type"] = record["message_type"]?.jsonPrimitive?.contentOrNull

        val createdAtStr = record["created_at"]?.jsonPrimitive?.contentOrNull
        val createdTime = parseTimestamp(createdAtStr)
        map["created_at"] = createdTime
        map["push_date"] = createdTime // Compatibility

        map["is_deleted"] = record["is_deleted"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        map["is_edited"] = record["is_edited"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        map["delete_for_everyone"] = record["delete_for_everyone"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        map["delivery_status"] = "delivered"

        // Add other fields if present
        record.entries.forEach { (key, value) ->
            if (!map.containsKey(key)) {
                map[key] = value.jsonPrimitive.contentOrNull
            }
        }

        return map
    }

    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp == null) return System.currentTimeMillis()

        // Try parsing as Long (if backend sends millis)
        timestamp.toLongOrNull()?.let { return it }

        // Try parsing as ISO-8601
        try {
            return Instant.parse(timestamp).toEpochMilli()
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Failed to parse timestamp: $timestamp", e)
            return System.currentTimeMillis()
        } catch (e: Exception) {
             // Fallback
             Log.e(TAG, "Error parsing timestamp: $timestamp", e)
             return System.currentTimeMillis()
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
}
