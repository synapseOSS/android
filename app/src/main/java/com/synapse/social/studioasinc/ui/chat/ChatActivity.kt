package com.synapse.social.studioasinc.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import com.synapse.social.studioasinc.ui.settings.AppearanceViewModel
import com.synapse.social.studioasinc.ui.chat.theme.ChatTheme
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import com.synapse.social.studioasinc.util.ActivityTransitions
import com.synapse.social.studioasinc.util.EdgeToEdgeUtils
import com.synapse.social.studioasinc.util.finishWithPremiumTransition
import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

/**
 * ChatActivity migrated to Jetpack Compose.
 * Refactored to remove main-thread blocking calls.
 */
@AndroidEntryPoint
class ChatActivity : ComponentActivity() {

    private val viewModel: DirectChatViewModel by viewModels()
    private var chatId: String? = null
    private var otherUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        EdgeToEdgeUtils.setupEdgeToEdgeActivity(this)

        handleIntent(intent)

        setContent {
            val appearanceViewModel: AppearanceViewModel = viewModel()
            val appearanceSettings by appearanceViewModel.appearanceSettings.collectAsState()
            
            val darkTheme = when (appearanceSettings.themeMode) {
                com.synapse.social.studioasinc.ui.settings.ThemeMode.LIGHT -> false
                com.synapse.social.studioasinc.ui.settings.ThemeMode.DARK -> true
                com.synapse.social.studioasinc.ui.settings.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            val dynamicColor = appearanceSettings.dynamicColorEnabled && 
                               Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            
            val uiState by viewModel.uiState.collectAsState()

            SynapseTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
                enableEdgeToEdge = true
            ) {
                ChatTheme(
                    darkTheme = darkTheme,
                    preset = uiState.themePreset
                ) {
                    chatId?.let { nonNullChatId ->
                        DirectChatScreen(
                            chatId = nonNullChatId,
                            otherUserId = otherUserId,
                            onBackClick = { finishWithPremiumTransition() },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // Parse Intent Extras safely
        chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: intent.getStringExtra("chatId")
        otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID) ?: intent.getStringExtra("uid") ?: ""

        if (chatId == null) {
            android.util.Log.e("ChatActivity", "ChatActivity started without chatId")
            finish()
            return
        }

        // Check authentication asynchronously
        lifecycleScope.launch {
            val user = try {
                SupabaseClient.client.auth.currentUserOrNull()
            } catch (e: Exception) {
                null
            }

            if (user == null) {
                finish()
            } else {
                // Initialize Chat only if user is logged in
                chatId?.let { nonNullChatId ->
                    viewModel.loadChat(nonNullChatId)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_CHAT_ID = "chat_id"
        private const val EXTRA_OTHER_USER_ID = "other_user_id"

        fun createIntent(context: Context, chatId: String): Intent {
            return Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_CHAT_ID, chatId)
            }
        }

        fun createIntent(context: Context, chatId: String, otherUserId: String): Intent {
            return Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_CHAT_ID, chatId)
                putExtra(EXTRA_OTHER_USER_ID, otherUserId)
            }
        }
    }
}
