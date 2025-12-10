package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.ui.profile.ProfileScreen
import com.synapse.social.studioasinc.ui.profile.ProfileViewModel
import com.synapse.social.studioasinc.ui.profile.ProfileViewModelFactory
import com.synapse.social.studioasinc.ui.settings.AppearanceViewModel
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking

/**
 * Jetpack Compose-based Profile Activity.
 * Replaces the old View-based ProfileActivity.
 * 
 * Usage:
 * ```
 * val intent = Intent(context, ProfileComposeActivity::class.java)
 * intent.putExtra("uid", userId)
 * startActivity(intent)
 * ```
 */
class ProfileComposeActivity : ComponentActivity() {
    
    private val viewModel: ProfileViewModel by viewModels { ProfileViewModelFactory(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val targetUserId = intent.getStringExtra("uid") ?: run {
            finish()
            return
        }
        
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
                dynamicColor = dynamicColor
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ProfileScreen(
                        userId = targetUserId,
                        currentUserId = currentUserId,
                        onNavigateBack = { finish() },
                        onNavigateToEditProfile = { navigateToEditProfile() },
                        onNavigateToFollowers = { navigateToFollowers(targetUserId) },
                        onNavigateToFollowing = { navigateToFollowing(targetUserId) },
                        onNavigateToSettings = { navigateToSettings() },
                        onNavigateToActivityLog = { navigateToActivityLog() },
                        onNavigateToUserProfile = { userId -> navigateToUserProfile(userId) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
    
    private fun navigateToEditProfile() {
        startActivity(Intent(this, ProfileEditActivity::class.java))
    }
    
    private fun navigateToFollowers(userId: String) {
        val intent = Intent(this, UserFollowsListActivity::class.java)
        intent.putExtra("uid", userId)
        intent.putExtra("tab", "followers")
        startActivity(intent)
    }
    
    private fun navigateToFollowing(userId: String) {
        val intent = Intent(this, UserFollowsListActivity::class.java)
        intent.putExtra("uid", userId)
        intent.putExtra("tab", "following")
        startActivity(intent)
    }
    
    private fun navigateToSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
    
    private fun navigateToActivityLog() {
        // TODO: Implement activity log navigation
    }
    
    private fun navigateToUserProfile(userId: String) {
        val intent = Intent(this, ProfileComposeActivity::class.java)
        intent.putExtra("uid", userId)
        startActivity(intent)
    }
}
