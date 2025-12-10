package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.ui.chat.DirectChatScreen
import com.synapse.social.studioasinc.ui.chat.DirectChatViewModel
import com.synapse.social.studioasinc.ui.settings.AppearanceViewModel
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import com.synapse.social.studioasinc.util.EdgeToEdgeUtils
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
        
        // Setup edge-to-edge display before setContent
        EdgeToEdgeUtils.setupEdgeToEdgeActivity(this)

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
            // Get appearance settings to apply theme preferences
            val appearanceViewModel: AppearanceViewModel = viewModel()
            val appearanceSettings by appearanceViewModel.appearanceSettings.collectAsState()
            
            // Determine dark theme based on settings
            val darkTheme = when (appearanceSettings.themeMode) {
                com.synapse.social.studioasinc.ui.settings.ThemeMode.LIGHT -> false
                com.synapse.social.studioasinc.ui.settings.ThemeMode.DARK -> true
                com.synapse.social.studioasinc.ui.settings.ThemeMode.SYSTEM -> 
                    isSystemInDarkTheme()
            }
            
            // Apply dynamic color only if enabled and supported (Android 12+)
            val dynamicColor = appearanceSettings.dynamicColorEnabled && 
                               Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            
            SynapseTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
                enableEdgeToEdge = true
            ) {
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
