package com.synapse.social.studioasinc

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UsernameRepository
import com.synapse.social.studioasinc.ui.auth.AuthScreen
import com.synapse.social.studioasinc.ui.auth.AuthViewModel
import com.synapse.social.studioasinc.ui.theme.AuthTheme

/**
 * Modern Compose-based AuthActivity.
 * Entry point for the new authentication flow.
 */
class AuthComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authRepository = AuthRepository()
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        val viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(authRepository, sharedPreferences)
        )[AuthViewModel::class.java]

        // Handle deep link if present (e.g. password reset)
        viewModel.handleDeepLink(intent.data)

        setContent {
            AuthTheme {
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
