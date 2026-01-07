package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.AuthActivity
import com.synapse.social.studioasinc.ui.main.MainActivity
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SynapseTheme {
                SplashScreen()
            }
        }

        // Check authentication state with enhanced retry mechanism
        lifecycleScope.launch {
            try {
                // Add initial delay to allow proper initialization
                delay(800)
                
                android.util.Log.d("SplashActivity", "Starting authentication check...")
                
                // First attempt at session restoration
                var isAuthenticated = authRepository.restoreSession()
                android.util.Log.d("SplashActivity", "First auth attempt result: $isAuthenticated")
                
                // Enhanced retry mechanism for intermittent failures
                if (!isAuthenticated) {
                    android.util.Log.d("SplashActivity", "First attempt failed, trying enhanced retry...")
                    
                    // Try up to 2 more times with increasing delays
                    for (attempt in 1..2) {
                        delay((500 * attempt).toLong()) // 500ms, then 1000ms
                        android.util.Log.d("SplashActivity", "Retry attempt $attempt...")
                        
                        isAuthenticated = authRepository.restoreSession()
                        if (isAuthenticated) {
                            android.util.Log.d("SplashActivity", "Authentication successful on retry $attempt")
                            break
                        }
                    }
                }
                
                // Final validation - check if user is actually logged in
                if (isAuthenticated) {
                    val userId = authRepository.getCurrentUserId()
                    val userEmail = authRepository.getCurrentUserEmail()
                    
                    if (userId != null && !userEmail.isNullOrBlank()) {
                        android.util.Log.d("SplashActivity", "User authenticated: $userId")
                        // User is authenticated, go to MainActivity
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    } else {
                        android.util.Log.w("SplashActivity", "Session restored but user data invalid, redirecting to auth")
                        isAuthenticated = false
                    }
                }
                
                if (!isAuthenticated) {
                    android.util.Log.d("SplashActivity", "User not authenticated, redirecting to auth screen")
                    // User is not authenticated, go to AuthActivity
                    startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SplashActivity", "Authentication check failed", e)
                // On any error, default to auth screen for safety
                startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
            } finally {
                finish()
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Synapse Logo",
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )
    }
}
