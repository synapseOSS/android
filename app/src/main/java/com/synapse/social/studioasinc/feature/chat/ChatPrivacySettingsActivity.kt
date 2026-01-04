package com.synapse.social.studioasinc

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.chat.service.PreferencesManager
import com.synapse.social.studioasinc.ui.settings.ChatPrivacyScreen
import com.synapse.social.studioasinc.ui.settings.ChatPrivacyViewModel
import com.synapse.social.studioasinc.ui.theme.SynapseTheme

/**
 * Activity for managing chat privacy settings.
 * Allows users to control read receipts and typing indicators.
 * 
 * Re-implemented with Jetpack Compose and Material 3 Expressive components.
 *
 * Requirements: 5.1, 5.5
 */
class ChatPrivacySettingsActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val viewModel: ChatPrivacyViewModel by viewModels {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatPrivacyViewModel(
                        PreferencesManager.getInstance(applicationContext)
                    ) as T
                }
            }
        }
        
        setContent {
            SynapseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatPrivacyScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}
