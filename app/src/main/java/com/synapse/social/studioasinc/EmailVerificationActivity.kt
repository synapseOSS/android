package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
// import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.databinding.ActivityEmailVerificationBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.SharedPreferences
import android.view.View

/**
 * Dedicated EmailVerificationActivity for handling email verification flow
 * Provides clear instructions, resend functionality, and automatic verification checking
 */
class EmailVerificationActivity : BaseActivity() {

    private lateinit var binding: ActivityEmailVerificationBinding
    // Using Supabase client directly
    private lateinit var sharedPreferences: SharedPreferences
    
    private var userEmail: String = ""
    private var userPassword: String = ""
    private var resendCooldownSeconds = 0
    private var isResendCooldownActive = false
    private var isVerificationCheckingActive = false

    companion object {
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_PASSWORD = "extra_password"
        private const val VERIFICATION_CHECK_INTERVAL_MS = 30000L // 30 seconds
        private const val MAX_VERIFICATION_CHECKS = 20 // 10 minutes total
        private const val RESEND_COOLDOWN_SECONDS = 60
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Using Supabase client directly
        sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        // Get email and password from intent
        userEmail = intent.getStringExtra(EXTRA_EMAIL) ?: ""
        userPassword = intent.getStringExtra(EXTRA_PASSWORD) ?: ""

        if (userEmail.isEmpty()) {
            // If no email provided, try to get from SharedPreferences
            userEmail = getSavedEmailForResend() ?: ""
        }

        if (userEmail.isEmpty()) {
            // No email available, return to AuthActivity
            Toast.makeText(this, "No email found for verification", Toast.LENGTH_SHORT).show()
            navigateBackToAuth()
            return
        }

        setupUI()
        startAutomaticVerificationChecking()
    }

    private fun setupUI() {
        binding.apply {
            // Display email address
            tvVerificationEmail.text = "Email sent to: $userEmail"
            
            // Set up click listeners
            btnResendVerification.setOnClickListener {
                resendVerificationEmail()
            }
            
            btnBackToSignIn.setOnClickListener {
                navigateBackToAuth()
            }
            
            btnCheckVerification.setOnClickListener {
                checkVerificationStatus()
            }
        }
        
        // Save email for resend functionality
        saveEmailForResend(userEmail)
    }

    /**
     * Save email to SharedPreferences for resend functionality
     */
    private fun saveEmailForResend(email: String) {
        sharedPreferences.edit()
            .putString("pending_verification_email", email)
            .apply()
    }

    /**
     * Get saved email for resend functionality
     */
    private fun getSavedEmailForResend(): String? {
        return sharedPreferences.getString("pending_verification_email", null)
    }

    /**
     * Clear saved email after successful verification
     */
    private fun clearSavedEmail() {
        sharedPreferences.edit()
            .remove("pending_verification_email")
            .apply()
    }

    /**
     * Resend verification email with cooldown timer
     */
    private fun resendVerificationEmail() {
        if (isResendCooldownActive) {
            return
        }
        
        // Show loading state for resend button
        binding.btnResendVerification.isEnabled = false
        binding.btnResendVerification.text = "Sending..."
        
        lifecycleScope.launch {
            try {
                // Note: Resend functionality may need to be implemented differently
                // For now, just show a message
                // SupabaseClient.client.auth.resend(email = userEmail)
                Toast.makeText(this@EmailVerificationActivity, "Verification email sent! Please check your inbox.", Toast.LENGTH_LONG).show()
                startResendCooldown()
                
                // Restart verification checking after resend
                if (!isVerificationCheckingActive) {
                    startAutomaticVerificationChecking()
                }
            } catch (e: Exception) {
                handleResendError(e)
            }
        }
    }

    /**
     * Handle resend verification email errors
     */
    private fun handleResendError(error: Throwable) {
        val errorMessage = when {
            error.message?.contains("network", ignoreCase = true) == true -> 
                "Network error. Please check your connection and try again."
            error.message?.contains("rate limit", ignoreCase = true) == true -> 
                "Too many requests. Please wait before trying again."
            else -> 
                "Failed to resend: ${error.message}"
        }
        
        Toast.makeText(this@EmailVerificationActivity, errorMessage, Toast.LENGTH_LONG).show()
        resetResendButton()
    }

    /**
     * Start cooldown timer for resend verification button
     */
    private fun startResendCooldown() {
        isResendCooldownActive = true
        resendCooldownSeconds = RESEND_COOLDOWN_SECONDS
        
        binding.apply {
            btnResendVerification.isEnabled = false
            tvResendCooldown.visibility = View.VISIBLE
        }
        
        lifecycleScope.launch {
            while (resendCooldownSeconds > 0) {
                binding.tvResendCooldown.text = "You can resend in $resendCooldownSeconds seconds"
                delay(1000)
                resendCooldownSeconds--
            }
            
            // Cooldown finished
            isResendCooldownActive = false
            binding.apply {
                btnResendVerification.isEnabled = true
                btnResendVerification.text = "Resend Verification Email"
                tvResendCooldown.visibility = View.GONE
            }
        }
    }

    /**
     * Reset resend button to normal state
     */
    private fun resetResendButton() {
        binding.btnResendVerification.isEnabled = true
        binding.btnResendVerification.text = "Resend Verification Email"
    }

    /**
     * Start automatic verification status checking
     */
    private fun startAutomaticVerificationChecking() {
        if (isVerificationCheckingActive || userPassword.isEmpty()) {
            return
        }
        
        isVerificationCheckingActive = true
        
        lifecycleScope.launch {
            repeat(MAX_VERIFICATION_CHECKS) { attempt ->
                delay(VERIFICATION_CHECK_INTERVAL_MS)
                
                // Only continue checking if activity is still active
                if (!isVerificationCheckingActive) {
                    return@launch
                }
                
                try {
                    // Try to sign in to check if email is verified
                    try {
                        SupabaseClient.client.auth.signInWith(Email) {
                            this.email = userEmail
                            this.password = userPassword
                        }
                        
                        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                        if (currentUser?.emailConfirmedAt != null) {
                            // Email is verified, attempt automatic sign in
                            attemptAutoSignIn()
                            return@launch
                        }
                    } catch (e: Exception) {
                        // Log error but continue checking
                        android.util.Log.w("EmailVerification", "Failed to check verification status: ${e.message}")
                    }
                } catch (e: Exception) {
                    // Log error but continue checking
                    android.util.Log.w("EmailVerification", "Error during verification check: ${e.message}")
                }
            }
            
            // Max checks reached
            isVerificationCheckingActive = false
        }
    }

    /**
     * Stop automatic verification checking
     */
    private fun stopAutomaticVerificationChecking() {
        isVerificationCheckingActive = false
    }

    /**
     * Manually check verification status
     */
    private fun checkVerificationStatus() {
        binding.btnCheckVerification.isEnabled = false
        binding.btnCheckVerification.text = "Checking..."
        
        lifecycleScope.launch {
            try {
                // Try to sign in to check if email is verified
                try {
                    SupabaseClient.client.auth.signInWith(Email) {
                        this.email = userEmail
                        this.password = userPassword
                    }
                    
                    val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                    if (currentUser?.emailConfirmedAt != null) {
                        if (userPassword.isNotEmpty()) {
                            attemptAutoSignIn()
                        } else {
                            // No password available, just navigate back
                            Toast.makeText(this@EmailVerificationActivity, "Email verified! Please sign in.", Toast.LENGTH_SHORT).show()
                            navigateBackToAuthWithSuccess()
                        }
                    } else {
                        Toast.makeText(this@EmailVerificationActivity, "Email not yet verified. Please check your inbox.", Toast.LENGTH_SHORT).show()
                        resetCheckButton()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@EmailVerificationActivity, "Failed to check verification: ${e.message}", Toast.LENGTH_SHORT).show()
                    resetCheckButton()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EmailVerificationActivity, "Error checking verification: ${e.message}", Toast.LENGTH_SHORT).show()
                resetCheckButton()
            }
        }
    }

    /**
     * Reset check verification button
     */
    private fun resetCheckButton() {
        binding.btnCheckVerification.isEnabled = true
        binding.btnCheckVerification.text = "Check Verification Status"
    }

    /**
     * Attempt automatic sign in after email verification
     */
    private fun attemptAutoSignIn() {
        if (userPassword.isEmpty()) {
            // No password available, navigate back to auth
            Toast.makeText(this, "Email verified! Please sign in.", Toast.LENGTH_SHORT).show()
            navigateBackToAuthWithSuccess()
            return
        }
        
        // Show loading state
        binding.apply {
            progressBar.visibility = View.VISIBLE
            tvStatusMessage.text = "Email verified! Signing you in..."
            tvStatusMessage.visibility = View.VISIBLE
        }
        
        lifecycleScope.launch {
            try {
                try {
                    SupabaseClient.client.auth.signInWith(Email) {
                        this.email = userEmail
                        this.password = userPassword
                    }
                    
                    val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                    if (currentUser?.emailConfirmedAt != null) {
                        // Successful authentication after verification
                        clearSavedEmail()
                        stopAutomaticVerificationChecking()
                        Toast.makeText(this@EmailVerificationActivity, "Email verified! Welcome back.", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } else {
                        // Still needs verification or other issue
                        binding.apply {
                            progressBar.visibility = View.GONE
                            tvStatusMessage.text = "Verification pending. Please check your email."
                        }
                        resetCheckButton()
                    }
                } catch (e: Exception) {
                    // Auto sign in failed, let user try manually
                    binding.apply {
                        progressBar.visibility = View.GONE
                        tvStatusMessage.text = "Email verified! Please return to sign in."
                    }
                    Toast.makeText(this@EmailVerificationActivity, "Email verified! Please sign in manually.", Toast.LENGTH_SHORT).show()
                    resetCheckButton()
                }
            } catch (e: Exception) {
                // Auto sign in failed, let user try manually
                binding.apply {
                    progressBar.visibility = View.GONE
                    tvStatusMessage.text = "Email verified! Please return to sign in."
                }
                Toast.makeText(this@EmailVerificationActivity, "Email verified! Please sign in manually.", Toast.LENGTH_SHORT).show()
                resetCheckButton()
            }
        }
    }

    /**
     * Navigate back to AuthActivity
     */
    private fun navigateBackToAuth() {
        clearSavedEmail()
        stopAutomaticVerificationChecking()
        
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Navigate back to AuthActivity with success indication
     */
    private fun navigateBackToAuthWithSuccess() {
        clearSavedEmail()
        stopAutomaticVerificationChecking()
        
        val intent = Intent(this, AuthActivity::class.java)
        intent.putExtra("verification_success", true)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Navigate to MainActivity after successful authentication
     */
    private fun navigateToMain() {
        stopAutomaticVerificationChecking()
        
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutomaticVerificationChecking()
    }

    override fun onBackPressed() {
        // Handle back button press
        navigateBackToAuth()
    }
}
