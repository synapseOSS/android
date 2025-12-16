package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.ui.deletion.MessageDeletionViewModel
import com.synapse.social.studioasinc.ui.inbox.InboxScreen
import com.synapse.social.studioasinc.ui.settings.AppearanceViewModel
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import com.synapse.social.studioasinc.util.ActivityTransitions
import com.synapse.social.studioasinc.util.EdgeToEdgeUtils

/**
 * Activity for the new Compose-based Inbox.
 * Replaces the old InboxActivity.
 */
class InboxComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge display before setContent
        EdgeToEdgeUtils.setupEdgeToEdgeActivity(this)
        
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val messageDeletionViewModel: MessageDeletionViewModel = hiltViewModel()
                    
                    InboxScreen(
                        onNavigateBack = { finish() },
                        onNavigateToChat = { chatId, userId ->
                            // Navigate to ChatActivity with premium transition
                            Log.d("InboxComposeActivity", "Navigating to chat - chatId: $chatId, userId: $userId")
                            
                            val intent = ChatActivity.createIntent(this, chatId, userId)
                            ActivityTransitions.startActivityWithTransition(this, intent)
                        },
                        messageDeletionViewModel = messageDeletionViewModel
                    )
                }
            }
        }
    }
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, InboxComposeActivity::class.java)
            context.startActivity(intent)
        }
    }
}
