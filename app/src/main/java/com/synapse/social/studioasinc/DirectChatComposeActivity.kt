package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.synapse.social.studioasinc.ui.chat.DirectChatScreen
import com.synapse.social.studioasinc.ui.chat.DirectChatViewModel
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking

/**
 * Jetpack Compose implementation of the Direct Chat screen.
 * Eventually replaces ChatActivity.kt.
 */
class DirectChatComposeActivity : ComponentActivity() {

    private val viewModel: DirectChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: run {
            finish()
            return
        }
        
        // Ensure user is logged in
        val currentUserId = runBlocking {
            try {
                SupabaseClient.client.auth.currentUserOrNull()?.id
            } catch (e: Exception) {
                null
            }
        } ?: run {
            finish()
            return
        }

        // Initialize Chat
        viewModel.loadChat(chatId)

        setContent {
            SynapseTheme {
                val uiState by viewModel.uiState.collectAsState()
                val messages by viewModel.messages.collectAsState()
                val context = LocalContext.current

                DirectChatScreen(
                    uiState = uiState,
                    messages = messages,
                    currentUserId = currentUserId,
                    onIntent = { intent -> viewModel.handleIntent(intent) },
                    onNavigateBack = { finish() },
                    onNavigateToProfile = { userId ->
                        // Reuse existing profile navigation
                        val intent = Intent(context, ProfileComposeActivity::class.java).apply {
                            putExtra("uid", userId)
                        }
                        context.startActivity(intent)
                    },
                    onNavigateToMediaViewer = { mediaId ->
                        // TODO: Open media viewer
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_CHAT_ID = "chat_id"
        private const val EXTRA_OTHER_USER_ID = "other_user_id" // Legacy compatibility if needed

        fun createIntent(context: Context, chatId: String): Intent {
            return Intent(context, DirectChatComposeActivity::class.java).apply {
                putExtra(EXTRA_CHAT_ID, chatId)
            }
        }
    }
}
