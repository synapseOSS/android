package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.ui.settings.AppearanceViewModel
import com.synapse.social.studioasinc.ui.settings.SettingsNavHost
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import kotlinx.coroutines.launch

/**
 * Main Settings Activity for the comprehensive settings feature.
 * 
 * This activity hosts the SettingsNavHost which manages navigation between
 * all settings screens. It integrates with existing activities (ProfileEditActivity,
 * ChatPrivacySettingsActivity) and applies the SynapseTheme with dynamic color support.
 * 
 * Requirements: 1.2, 2.2, 6.6
 */
class SettingsActivity : ComponentActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        setContent {
            // Get appearance settings to apply theme preferences
            val appearanceViewModel: AppearanceViewModel = viewModel()
            val appearanceSettings by appearanceViewModel.appearanceSettings.collectAsState()
            
            // Determine dark theme based on settings
            val darkTheme = when (appearanceSettings.themeMode) {
                com.synapse.social.studioasinc.ui.settings.ThemeMode.LIGHT -> false
                com.synapse.social.studioasinc.ui.settings.ThemeMode.DARK -> true
                com.synapse.social.studioasinc.ui.settings.ThemeMode.SYSTEM -> 
                    androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            // Apply dynamic color only if enabled and supported (Android 12+)
            val dynamicColor = appearanceSettings.dynamicColorEnabled && 
                               Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            
            SynapseTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsNavHost(
                        onBackClick = {
                            // Exit settings and return to previous screen
                            finish()
                        },
                        onNavigateToProfileEdit = {
                            // Navigate to existing ProfileEditActivity
                            startActivity(Intent(this@SettingsActivity, ProfileEditActivity::class.java))
                        },
                        onNavigateToChatPrivacy = {
                            // Navigate to existing ChatPrivacySettingsActivity
                            startActivity(Intent(this@SettingsActivity, ChatPrivacySettingsActivity::class.java))
                        },
                        onLogout = {
                            performLogout()
                        }
                    )
                }
            }
        }
    }

    /**
     * Performs user logout by signing out from auth repository
     * and navigating to AuthActivity.
     */
    private fun performLogout() {
        lifecycleScope.launch {
            authRepository.signOut()
            startActivity(Intent(this@SettingsActivity, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}
