package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
// import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.databinding.ActivityForgotPasswordBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

/**
 * ForgotPasswordActivity handles the password reset flow.
 * Allows users to request a password reset link via email using Supabase authentication.
 */
class ForgotPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    
    // Email validation debounce handler
    private val emailValidationHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var emailValidationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupEnhancedAnimations()
        setupAccessibilityDescriptions()
    }

    /**
     * Check if animations should be reduced based on system preferences
     */
    private fun shouldReduceMotion(): Boolean {
        return try {
            val animationScale = android.provider.Settings.Global.getFloat(
                contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            animationScale == 0f
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Setup enhanced animations for UI elements
     */
    private fun setupEnhancedAnimations() {
        val reduceMotion = shouldReduceMotion()
        
        binding.apply {
            // Logo entrance animation with scale and overshoot interpolator
            cardLogo.apply {
                if (reduceMotion) {
                    scaleX = 1f
                    scaleY = 1f
                    alpha = 1f
                } else {
                    scaleX = 0f
                    scaleY = 0f
                    alpha = 0f
                    postDelayed({
                        animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(600)
                            .setInterpolator(OvershootInterpolator(1.5f))
                            .start()
                    }, 100)
                }
            }
            
            // Title fade-in animation with staggered delay
            tvTitle.apply {
                if (reduceMotion) {
                    alpha = 1f
                    translationY = 0f
                } else {
                    alpha = 0f
                    translationY = -20f
                    postDelayed({
                        animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(400)
                            .start()
                    }, 400)
                }
            }
            
            // Subtitle fade-in animation with staggered delay
            tvSubtitle.apply {
                if (reduceMotion) {
                    alpha = 1f
                    translationY = 0f
                } else {
                    alpha = 0f
                    translationY = -20f
                    postDelayed({
                        animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(400)
                            .start()
                    }, 600)
                }
            }
            
            // Setup input field focus animation
            setupInputFieldFocusAnimation()
            
            // Setup button press animation
            setupButtonPressAnimation()
        }
    }

    /**
     * Setup input field focus animation with scale effect
     */
    private fun setupInputFieldFocusAnimation() {
        val reduceMotion = shouldReduceMotion()
        
        binding.etEmail.setOnFocusChangeListener { view, hasFocus ->
            if (!reduceMotion) {
                if (hasFocus) {
                    binding.tilEmail.animate()
                        .scaleX(1.02f)
                        .scaleY(1.02f)
                        .setDuration(200)
                        .start()
                } else {
                    binding.tilEmail.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
            }
        }
    }

    /**
     * Setup button press animation with scale down/up on touch events
     */
    private fun setupButtonPressAnimation() {
        val reduceMotion = shouldReduceMotion()
        
        binding.btnContinue.setOnTouchListener { view, event ->
            if (!reduceMotion) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                }
            }
            false // Return false to allow click event to proceed
        }
    }

    /**
     * Setup accessibility content descriptions for all interactive elements
     */
    private fun setupAccessibilityDescriptions() {
        binding.apply {
            // Input field
            etEmail.contentDescription = "Email address input field"
            tilEmail.contentDescription = "Email address"
            
            // Buttons
            btnContinue.contentDescription = "Continue button. Double tap to send password reset link"
            btnBack.contentDescription = "Back button. Double tap to return to sign in screen"
            
            // Logo and branding
            cardLogo.contentDescription = "Synapse application logo"
            tvTitle.contentDescription = "Forgot Password"
            
            // Loading overlay
            loadingOverlay.contentDescription = "Loading. Please wait while we process your request"
        }
    }

    /**
     * Setup UI components and event listeners
     */
    private fun setupUI() {
        binding.apply {
            // Back button navigation
            btnBack.setOnClickListener {
                // Add haptic feedback
                it.performHapticFeedback(
                    HapticFeedbackConstants.CONTEXT_CLICK,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
                finish()
            }
            
            // Continue button click listener
            btnContinue.setOnClickListener {
                // Add haptic feedback
                it.performHapticFeedback(
                    HapticFeedbackConstants.CONTEXT_CLICK,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
                submitPasswordReset()
            }
            
            // Real-time email validation with debounce
            etEmail.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tilEmail.error = null
                    cardError.visibility = View.GONE
                    cardSuccess.visibility = View.GONE
                    
                    // Remove end icon while typing
                    tilEmail.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
                }
                override fun afterTextChanged(s: android.text.Editable?) {
                    // Cancel previous validation
                    emailValidationRunnable?.let { emailValidationHandler.removeCallbacks(it) }
                    
                    // Schedule new validation with 300ms debounce
                    emailValidationRunnable = Runnable {
                        validateEmailRealtime(s?.toString() ?: "")
                    }
                    emailValidationHandler.postDelayed(emailValidationRunnable!!, 300)
                }
            })
            
            // Handle IME action on email field
            etEmail.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    submitPasswordReset()
                    true
                } else {
                    false
                }
            }
        }
    }

    /**
     * Validate email in real-time with visual feedback
     */
    private fun validateEmailRealtime(email: String) {
        binding.apply {
            if (email.isEmpty()) {
                // No validation for empty field
                tilEmail.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
                tilEmail.error = null
                return
            }
            
            val isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            val reduceMotion = shouldReduceMotion()
            
            if (isValid) {
                // Show green checkmark icon
                tilEmail.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
                tilEmail.setEndIconDrawable(android.R.drawable.checkbox_on_background)
                tilEmail.setEndIconTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@ForgotPasswordActivity, R.color.success_green)
                ))
                tilEmail.error = null
                
                // Announce success for accessibility
                tilEmail.announceForAccessibility("Email address is valid")
                
                // Animate the entire TextInputLayout with subtle scale
                if (!reduceMotion) {
                    tilEmail.animate()
                        .scaleX(1.01f)
                        .scaleY(1.01f)
                        .setDuration(100)
                        .withEndAction {
                            tilEmail.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                        }
                        .start()
                }
            } else {
                // Show red error icon with error message
                tilEmail.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
                tilEmail.setEndIconDrawable(android.R.drawable.ic_dialog_alert)
                tilEmail.setEndIconTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@ForgotPasswordActivity, R.color.error_red)
                ))
                val errorMessage = "Please enter a valid email address"
                tilEmail.error = errorMessage
                
                // Announce error for accessibility
                tilEmail.announceForAccessibility("Email validation error: $errorMessage")
                
                // Animate error appearance with subtle shake
                if (!reduceMotion) {
                    tilEmail.animate()
                        .translationX(-5f)
                        .setDuration(50)
                        .withEndAction {
                            tilEmail.animate()
                                .translationX(5f)
                                .setDuration(50)
                                .withEndAction {
                                    tilEmail.animate()
                                        .translationX(0f)
                                        .setDuration(50)
                                        .start()
                                }
                                .start()
                        }
                        .start()
                }
            }
        }
    }

    /**
     * Submit password reset request
     */
    private fun submitPasswordReset() {
        val email = binding.etEmail.text.toString().trim()
        
        if (!validateInput(email)) {
            return
        }
        
        // Check if Supabase is configured
        if (!SupabaseClient.isConfigured()) {
            showError("Supabase is not configured. Please check your setup and try again.")
            return
        }
        
        // Show loading state
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                // Send password reset email via Supabase
                SupabaseClient.client.auth.resetPasswordForEmail(email)
                
                // Hide loading state
                showLoading(false)
                
                // Show success message with email confirmation
                showSuccess("Password reset link sent to $email. Please check your inbox.")
                
                // Announce success for accessibility
                binding.cardSuccess.announceForAccessibility("Password reset link sent successfully")
                
                // Navigate back to sign in after delay
                binding.root.postDelayed({
                    finish()
                }, 3000)
                
            } catch (e: Exception) {
                // Hide loading state
                showLoading(false)
                
                // Show user-friendly error message
                val errorMessage = getUserFriendlyErrorMessage(e)
                showError(errorMessage)
                
                // Announce error for accessibility
                binding.cardError.announceForAccessibility("Error: $errorMessage")
            }
        }
    }

    /**
     * Validate email input
     */
    private fun validateInput(email: String): Boolean {
        binding.apply {
            // Clear previous errors
            cardError.visibility = View.GONE
            cardSuccess.visibility = View.GONE
            
            // Validate email
            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                showError("Please enter your email address to continue.")
                
                // Focus on email field
                etEmail.requestFocus()
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(etEmail, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                
                return false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Please enter a valid email address"
                showError("Please enter a valid email address (e.g., example@email.com).")
                
                // Focus on email field
                etEmail.requestFocus()
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(etEmail, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                
                return false
            }
        }
        
        return true
    }

    /**
     * Show loading overlay
     */
    private fun showLoading(show: Boolean) {
        binding.apply {
            if (show) {
                loadingOverlay.visibility = View.VISIBLE
                loadingOverlay.alpha = 0f
                loadingOverlay.animate().alpha(1f).setDuration(200).start()
                btnContinue.isEnabled = false
                btnContinue.alpha = 0.6f
                
                // Announce loading for accessibility
                loadingOverlay.announceForAccessibility("Sending password reset link. Please wait.")
            } else {
                loadingOverlay.visibility = View.GONE
                btnContinue.isEnabled = true
                btnContinue.alpha = 1f
            }
        }
    }

    /**
     * Show error message with animation
     */
    private fun showError(message: String) {
        binding.apply {
            tvErrorMessage.text = message
            cardError.visibility = View.VISIBLE
            cardSuccess.visibility = View.GONE
            
            // Load and apply shake animation from resources
            val shakeAnimation = AnimationUtils.loadAnimation(this@ForgotPasswordActivity, R.anim.shake)
            cardError.startAnimation(shakeAnimation)
            
            // Fade in animation
            cardError.alpha = 0f
            cardError.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
            
            // Add haptic feedback
            cardError.performHapticFeedback(
                HapticFeedbackConstants.REJECT,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        }
    }

    /**
     * Show success message with animation
     */
    private fun showSuccess(message: String) {
        binding.apply {
            tvSuccessMessage.text = message
            cardSuccess.visibility = View.VISIBLE
            cardError.visibility = View.GONE
            
            // Fade in animation
            cardSuccess.alpha = 0f
            cardSuccess.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
            
            // Add haptic feedback
            cardSuccess.performHapticFeedback(
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        }
    }

    /**
     * Get user-friendly error message based on exception
     */
    private fun getUserFriendlyErrorMessage(exception: Exception): String {
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            // Network errors
            message.contains("network") || message.contains("connection") || message.contains("timeout") -> {
                "Unable to connect to the server. Please check your internet connection and try again."
            }
            
            // Email not found
            message.contains("user not found") || message.contains("email not found") -> {
                "No account found with this email address. Please check your email or create a new account."
            }
            
            // Rate limiting
            message.contains("rate limit") || message.contains("too many") -> {
                "Too many attempts. Please wait a few minutes before trying again."
            }
            
            // Server errors
            message.contains("500") || message.contains("server error") -> {
                "The server is experiencing issues. Please try again in a few moments."
            }
            
            // Default error message
            else -> {
                "Unable to send password reset link. Please try again or contact support if the problem persists."
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up validation handler to prevent memory leaks
        emailValidationRunnable?.let { emailValidationHandler.removeCallbacks(it) }
    }
}
