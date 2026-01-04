package com.synapse.social.studioasinc.core.util

import android.util.Log
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * Manager for handling real-time updates from Supabase
 * Provides flows for posts, comments, and reactions changes
 */
object RealtimeUpdateManager {
    private const val TAG = "RealtimeUpdateManager"
    private val client = SupabaseClient.client

    /**
     * Listen for post changes (INSERT, UPDATE, DELETE)
     */
    fun observePostChanges(
        scope: CoroutineScope,
        onInsert: (JsonObject) -> Unit = {},
        onUpdate: (JsonObject) -> Unit = {},
        onDelete: (String) -> Unit = {}
    ) {
        try {
            val channel = client.realtime.channel("posts_changes")
            
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "posts"
            }.onEach { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        Log.d(TAG, "Post inserted: ${action.record}")
                        onInsert(action.record)
                    }
                    is PostgresAction.Update -> {
                        Log.d(TAG, "Post updated: ${action.record}")
                        onUpdate(action.record)
                    }
                    is PostgresAction.Delete -> {
                        val postId = action.oldRecord["id"]?.toString() ?: return@onEach
                        Log.d(TAG, "Post deleted: $postId")
                        onDelete(postId)
                    }
                    else -> {}
                }
            }.launchIn(scope)
            
            scope.launch { channel.subscribe() }
        } catch (e: Exception) {
            Log.e(TAG, "Error observing post changes", e)
        }
    }

    /**
     * Listen for comment changes (INSERT, UPDATE, DELETE)
     */
    fun observeCommentChanges(
        scope: CoroutineScope,
        postId: String? = null,
        onInsert: (JsonObject) -> Unit = {},
        onUpdate: (JsonObject) -> Unit = {},
        onDelete: (String) -> Unit = {}
    ) {
        try {
            val channel = client.realtime.channel("comments_changes")
            
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "comments"
                if (postId != null) {
                    filter("post_id", FilterOperator.EQ, postId)
                }
            }.onEach { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        Log.d(TAG, "Comment inserted: ${action.record}")
                        onInsert(action.record)
                    }
                    is PostgresAction.Update -> {
                        Log.d(TAG, "Comment updated: ${action.record}")
                        onUpdate(action.record)
                    }
                    is PostgresAction.Delete -> {
                        val commentId = action.oldRecord["id"]?.toString() ?: return@onEach
                        Log.d(TAG, "Comment deleted: $commentId")
                        onDelete(commentId)
                    }
                    else -> {}
                }
            }.launchIn(scope)
            
            scope.launch { channel.subscribe() }
        } catch (e: Exception) {
            Log.e(TAG, "Error observing comment changes", e)
        }
    }

    /**
     * Listen for reaction changes (INSERT, UPDATE, DELETE)
     */
    fun observeReactionChanges(
        scope: CoroutineScope,
        postId: String? = null,
        onInsert: (JsonObject) -> Unit = {},
        onUpdate: (JsonObject) -> Unit = {},
        onDelete: (String) -> Unit = {}
    ) {
        try {
            val channel = client.realtime.channel("reactions_changes")
            
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "reactions"
                if (postId != null) {
                    filter("post_id", FilterOperator.EQ, postId)
                }
            }.onEach { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        Log.d(TAG, "Reaction inserted: ${action.record}")
                        onInsert(action.record)
                    }
                    is PostgresAction.Update -> {
                        Log.d(TAG, "Reaction updated: ${action.record}")
                        onUpdate(action.record)
                    }
                    is PostgresAction.Delete -> {
                        val reactionId = action.oldRecord["id"]?.toString() ?: return@onEach
                        Log.d(TAG, "Reaction deleted: $reactionId")
                        onDelete(reactionId)
                    }
                    else -> {}
                }
            }.launchIn(scope)
            
            scope.launch { channel.subscribe() }
        } catch (e: Exception) {
            Log.e(TAG, "Error observing reaction changes", e)
        }
    }

    /**
     * Unsubscribe from all channels
     */
    suspend fun unsubscribeAll() {
        try {
            client.realtime.removeAllChannels()
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from channels", e)
        }
    }
}
