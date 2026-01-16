package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import com.synapse.social.studioasinc.feature.auth.ui.components.ProfileCompletionDialogFragment
import com.synapse.social.studioasinc.ui.home.HomeScreen
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.theme.ThemeManager
import com.synapse.social.studioasinc.core.ui.animation.ActivityTransitions
import com.synapse.social.studioasinc.feature.stories.viewer.StoryViewerActivity
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        applyThemeFromSettings()

        setContent {
            val settingsRepository = com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl.getInstance(this@HomeActivity)
            val appearanceSettings by settingsRepository.appearanceSettings.collectAsState(
                initial = com.synapse.social.studioasinc.ui.settings.AppearanceSettings()
            )
            
            val darkTheme = when (appearanceSettings.themeMode) {
                com.synapse.social.studioasinc.ui.settings.ThemeMode.LIGHT -> false
                com.synapse.social.studioasinc.ui.settings.ThemeMode.DARK -> true
                com.synapse.social.studioasinc.ui.settings.ThemeMode.SYSTEM -> 
                    isSystemInDarkTheme()
            }
            
            val dynamicColor = appearanceSettings.dynamicColorEnabled && 
                               Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            
            SynapseTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                HomeScreen(
                    onNavigateToSearch = {
                        ActivityTransitions.startActivityWithTransition(
                            this,
                            Intent(this, SearchActivity::class.java)
                        )
                    },
                    onNavigateToProfile = { userId ->
                        val targetUid = if (userId == "me") SupabaseClient.client.auth.currentUserOrNull()?.id else userId
                        if (targetUid != null) {
                            val intent = Intent(this, ProfileActivity::class.java).apply {
                                putExtra("uid", targetUid)
                            }
                            ActivityTransitions.startActivityWithTransition(this, intent)
                        }
                    },
                    onNavigateToInbox = {
                        ActivityTransitions.startActivityWithTransition(
                            this,
                            Intent(this, InboxActivity::class.java)
                        )
                    },
                    onNavigateToCreatePost = {
                        ActivityTransitions.startActivityWithTransition(
                            this,
                            Intent(this, CreatePostActivity::class.java)
                        )
                    },
                    onNavigateToStoryViewer = { userId ->
                        val intent = Intent(this@HomeActivity, StoryViewerActivity::class.java).apply {
                            putExtra("user_id", userId)
                        }
                        ActivityTransitions.startActivityWithTransition(this@HomeActivity, intent)
                    }
                )
            }
        }
        
        checkProfileCompletionDialog()
    }
    
    private fun applyThemeFromSettings() {
        val settingsRepository = com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl.getInstance(this)
        lifecycleScope.launch {
            try {
                settingsRepository.appearanceSettings.collect { settings ->
                    ThemeManager.applyThemeMode(settings.themeMode)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeActivity", "Failed to apply theme from settings", e)
            }
        }
    }

    private fun checkProfileCompletionDialog() {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val showDialog = sharedPreferences.getBoolean("show_profile_completion_dialog", false)

        if (showDialog) {
            ProfileCompletionDialogFragment().show(supportFragmentManager, ProfileCompletionDialogFragment.TAG)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        if (currentUser != null) {
            // TODO: Re-implement presence management if needed
        }
    }

    // Removed the overridden onBackPressed to let Jetpack Compose Navigation handle the back stack.
    // If we need the exit confirmation, it should be handled in the HomeScreen composable
    // using BackHandler when at the start destination.
}
