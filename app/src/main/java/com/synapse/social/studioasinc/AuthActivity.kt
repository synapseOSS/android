package com.synapse.social.studioasinc

import android.content.Intent
import android.net.Uri
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UsernameRepository
import com.synapse.social.studioasinc.ui.auth.AuthScreen
import com.synapse.social.studioasinc.ui.auth.AuthViewModel
import com.synapse.social.studioasinc.ui.theme.AuthTheme
import com.synapse.social.studioasinc.util.EdgeToEdgeUtils

/**
 * Modern Compose-based AuthActivity.
 * Entry point for the new authentication flow.
 */
class AuthActivity : ComponentActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge display before setContent
        EdgeToEdgeUtils.setupEdgeToEdgeActivity(this)

        // Configure URL Opener for Supabase
        SupabaseClient.openUrl = { url ->
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(this, Uri.parse(url))
        }

        val authRepository = AuthRepository()
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(authRepository, sharedPreferences)
        )[AuthViewModel::class.java]

        // Handle deep link if present (e.g. password reset or OAuth callback)
        handleDeepLink(intent)

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

        // Setup observer for external URL opening
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null) {
            // Check if it's OAuth callback or Recovery
            // OAuth callback typically has code or tokens.
            // Recovery has type=recovery.

            // Delegate to ViewModel to decide
            viewModel.handleDeepLink(data)
        }
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository, UsernameRepository(), sharedPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
