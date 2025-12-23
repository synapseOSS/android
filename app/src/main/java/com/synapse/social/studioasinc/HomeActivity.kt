package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import com.synapse.social.studioasinc.ui.auth.components.ProfileCompletionDialogFragment
import com.synapse.social.studioasinc.ui.home.HomeScreen
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.theme.ThemeManager
import com.synapse.social.studioasinc.util.ActivityTransitions
import kotlinx.coroutines.launch

class HomeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        applyThemeFromSettings()

        setContent {
            SynapseTheme {
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
                            val intent = Intent(this, ProfileComposeActivity::class.java).apply {
                                putExtra("uid", targetUid)
                            }
                            ActivityTransitions.startActivityWithTransition(this, intent)
                        }
                    },
                    onNavigateToInbox = {
                        ActivityTransitions.startActivityWithTransition(
                            this,
                            Intent(this, InboxComposeActivity::class.java)
                        )
                    },
                    onNavigateToCreatePost = {
                        ActivityTransitions.startActivityWithTransition(
                            this,
                            Intent(this, CreatePostActivity::class.java)
                        )
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
            ChatPresenceManager.setActivity(currentUser.id, "In Home")
        }
    }

    // Removed the overridden onBackPressed to let Jetpack Compose Navigation handle the back stack.
    // If we need the exit confirmation, it should be handled in the HomeScreen composable
    // using BackHandler when at the start destination.
}
