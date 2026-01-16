package com.synapse.social.studioasinc

import android.content.Intent
import android.net.Uri
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.ui.main.MainActivity
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UsernameRepository
import com.synapse.social.studioasinc.feature.auth.ui.AuthScreen
import com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.AuthViewModel
import com.synapse.social.studioasinc.ui.theme.AuthTheme
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

/**
 * Modern Compose-based AuthActivity.
 * Refactored for cleaner lifecycle management and deeper link handling.
 */
@AndroidEntryPoint
class AuthActivity : ComponentActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge display before setContent
        enableEdgeToEdge()

        // Configure URL Opener for Supabase
        // Note: Consider moving this to a central initializer to avoid reassignment if possible,
        // but keeping here as it might be activity-context dependent (though it's a lambda).
        SupabaseClient.openUrl = { url ->
            try {
                val customTabsIntent = CustomTabsIntent.Builder().build()
                customTabsIntent.launchUrl(this, Uri.parse(url))
            } catch (e: Exception) {
                // Fallback to standard browser if Custom Tabs fail
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }

        // Initialize ViewModel with Hilt
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Handle deep link if present
        intent?.let { handleDeepLink(it) }

        setContent {
            AuthTheme(enableEdgeToEdge = true) {
                AuthScreen(
                    viewModel = viewModel,
                    onNavigateToMain = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null) {
            viewModel.handleDeepLink(data)
        }
    }
}
