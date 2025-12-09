package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
// import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.databinding.ActivityAuthBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.SharedPreferences
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator
import android.view.HapticFeedbackConstants
import android.os.Build
import androidx.annotation.RequiresApi
import android.graphics.RenderEffect
import android.graphics.Shader
import android.widget.ScrollView

/**
 * AuthUIState sealed class for managing UI states
 */
sealed class AuthUIState {
    object Loading : AuthUIState()
    object SignInForm : AuthUIState()
    object SignUpForm : AuthUIState()
    data class EmailVerificationPending(val email: String) : AuthUIState()
    data class Authenticated(val user: UserInfo) : AuthUIState()
    data class Error(val message: String) : AuthUIState()
}

/**
 * Password strength levels with associated colors and messages
 */
sealed class PasswordStrength(val color: Int, val message: String, val progress: Int) {
    object Weak : PasswordStrength(R.color.password_weak, "Weak password", 33)
    object Fair : PasswordStrength(R.color.password_fair, "Fair password", 66)
    object Strong : PasswordStrength(R.color.password_strong, "Strong password", 100)
}

/**
 * Error field enum for field-specific error handling
 */
enum class ErrorField {
    EMAIL, PASSWORD, USERNAME, GENERAL
}

/**
 * Modern AuthActivity using Supabase authentication.
 * Handles user authentication with email/password using Supabase GoTrue.
 * Includes email verification flow handling and UI state management.
 */
class AuthActivity : BaseActivity() {

    private lateinit var binding: ActivityAuthBinding
    // Using Supabase client directly
    private lateinit var sharedPreferences: SharedPreferences
    
    private var isSignUpMode = false
    private var currentState: AuthUIState = AuthUIState.SignInForm
    private var resendCooldownSeconds = 0
    private var isResendCooldownActive = false
    
    // Validation debounce handlers
    private val emailValidationHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var emailValidationRunnable: Runnable? = null
    private val passwordValidationHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var passwordValidationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Using Supabase client directly
        sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        setupUI()
        setupKeyboardHandling()
        setupEnhancedAnimations()
        
        // Check if returning from successful email verification
        if (intent.getBooleanExtra("verification_success", false)) {
            Toast.makeText(this, "Email verified! Please sign in to continue.", Toast.LENGTH_LONG).show()
        }
        
        checkCurrentUser()
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
     * Get animation duration based on reduced motion preference
     */
    private fun getAnimationDuration(normalDuration: Long): Long {
        return if (shouldReduceMotion()) 0L else normalDuration
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
                    // Skip animation, just show the logo
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
            
            // App name fade-in animation with staggered delay
            tvAppName.apply {
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
            
            // Welcome message fade-in animation with staggered delay
            tvWelcome.apply {
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
            
            // Setup input field focus animations
            setupInputFieldFocusAnimations()
            
            // Setup button press animations
            setupButtonPressAnimations()
        }
    }
    
    /**
     * Setup input field focus animations with scale and glow effects
     * Note: This is called before setupKeyboardHandling, so these listeners
     * will be chained by the keyboard handling setup
     */
    private fun setupInputFieldFocusAnimations() {
        val reduceMotion = shouldReduceMotion()
        
        binding.apply {
            // Email field focus animation
            etEmail.setOnFocusChangeListener { view, hasFocus ->
                if (!reduceMotion) {
                    if (hasFocus) {
                        tilEmail.animate()
                            .scaleX(1.02f)
                            .scaleY(1.02f)
                            .setDuration(200)
                            .start()
                    } else {
                        tilEmail.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                    }
                }
            }
            
            // Password field focus animation
            etPassword.setOnFocusChangeListener { view, hasFocus ->
                if (!reduceMotion) {
                    if (hasFocus) {
                        tilPassword.animate()
                            .scaleX(1.02f)
                            .scaleY(1.02f)
                            .setDuration(200)
                            .start()
                    } else {
                        tilPassword.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                    }
                }
            }
            
            // Username field focus animation
            etUsername.setOnFocusChangeListener { view, hasFocus ->
                if (!reduceMotion) {
                    if (hasFocus) {
                        tilUsername.animate()
                            .scaleX(1.02f)
                            .scaleY(1.02f)
                            .setDuration(200)
                            .start()
                    } else {
                        tilUsername.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                    }
                }
            }
        }
    }
    
    /**
     * Setup button press animations with scale down/up on touch events
     */
    private fun setupButtonPressAnimations() {
        val reduceMotion = shouldReduceMotion()
        
        binding.apply {
            // Sign in/up button press animation - refined to 0.95x scale, 100ms
            btnSignIn.setOnTouchListener { view, event ->
                if (!reduceMotion && view.isEnabled) {
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
            
            // Mode toggle press animation
            tvToggleMode.setOnTouchListener { view, event ->
                if (!reduceMotion) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            view.animate()
                                .scaleX(0.97f)
                                .scaleY(0.97f)
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
    }
    
    /**
     * Setup social button press animations with scale down/up on touch events
     */
    private fun setupSocialButtonAnimations() {
        val reduceMotion = shouldReduceMotion()
        
        binding.apply {
            // Google button press animation
            btnGoogleAuth?.setOnTouchListener { view, event ->
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
            
            // Facebook button press animation
            btnFacebookAuth?.setOnTouchListener { view, event ->
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
            
            // Apple button press animation
            btnAppleAuth?.setOnTouchListener { view, event ->
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
    }
    
    /**
     * Handle social authentication button clicks with haptic feedback and placeholder messages
     */
    private fun handleSocialAuthClick(provider: String) {
        // Add haptic feedback on button press
        // CONTEXT_CLICK provides a light haptic feedback within 50ms requirement
        binding.root.performHapticFeedback(
            HapticFeedbackConstants.CONTEXT_CLICK,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
        
        // Show placeholder toast message for OAuth flow
        Toast.makeText(
            this,
            "Sign in with $provider - OAuth integration coming soon",
            Toast.LENGTH_SHORT
        ).show()
        
        // TODO: Implement OAuth flow for $provider
        // This will be implemented in a separate task
    }
    
    /**
     * Handle forgot password link click with haptic feedback and navigation
     */
    private fun handleForgotPasswordClick() {
        // Add haptic feedback on tap
        // CONTEXT_CLICK provides a light haptic feedback within 50ms requirement
        binding.tvForgotPassword?.performHapticFeedback(
            HapticFeedbackConstants.CONTEXT_CLICK,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
        
        // Navigate to ForgotPasswordActivity
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }
    
    /**
     * Setup accessibility content descriptions for all interactive elements
     */
    private fun setupAccessibilityDescriptions() {
        binding.apply {
            // Input fields
            etEmail.contentDescription = "Email address input field"
            tilEmail.contentDescription = "Email address"
            
            etPassword.contentDescription = "Password input field"
            tilPassword.contentDescription = "Password"
            
            etUsername.contentDescription = "Username input field"
            tilUsername.contentDescription = "Username"
            
            // Buttons
            btnSignIn.contentDescription = "Sign in button. Double tap to sign in with your credentials"
            tvToggleMode.contentDescription = "Switch authentication mode. Double tap to toggle between sign in and sign up"
            tvForgotPassword?.contentDescription = "Forgot password link. Double tap to reset your password"
            
            // Verification screen elements
            btnResendVerification.contentDescription = "Resend verification email button. Double tap to send a new verification email"
            btnBackToSignIn.contentDescription = "Back to sign in button. Double tap to return to the sign in screen"
            
            // Logo and branding
            cardLogo.contentDescription = "Synapse application logo"
            tvAppName.contentDescription = "Synapse"
            
            // Social authentication buttons
            btnGoogleAuth?.contentDescription = "Sign in with Google. Double tap to authenticate using your Google account"
            btnFacebookAuth?.contentDescription = "Sign in with Facebook. Double tap to authenticate using your Facebook account"
            btnAppleAuth?.contentDescription = "Sign in with Apple. Double tap to authenticate using your Apple ID"
            
            // Loading overlay
            loadingOverlay.contentDescription = "Loading. Please wait while we process your request"
            progressBar.contentDescription = "Loading progress indicator"
            
            // Error card
            cardError.contentDescription = "Error message"
            
            // Password strength indicator
            layoutPasswordStrength.contentDescription = "Password strength indicator"
            progressPasswordStrength.contentDescription = "Password strength progress bar"
        }
    }
    
    /**
     * Update content descriptions based on current mode
     */
    private fun updateAccessibilityForMode(isSignUp: Boolean) {
        binding.apply {
            if (isSignUp) {
                btnSignIn.contentDescription = "Create account button. Double tap to create your new account"
                tvToggleMode.contentDescription = "Switch to sign in mode. Double tap if you already have an account"
                tvWelcome.contentDescription = "Create your account"
            } else {
                btnSignIn.contentDescription = "Sign in button. Double tap to sign in with your credentials"
                tvToggleMode.contentDescription = "Switch to sign up mode. Double tap to create a new account"
                tvWelcome.contentDescription = "Welcome back"
            }
        }
    }
    
    /**
     * Setup keyboard handling for better UX
     */
    private fun setupKeyboardHandling() {
        binding.apply {
            // Handle IME action on password field
            etPassword.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    if (!isSignUpMode) {
                        performSignIn()
                        true
                    } else {
                        etUsername.requestFocus()
                        false
                    }
                } else {
                    false
                }
            }
            
            // Handle IME action on username field
            etUsername.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    performSignUp()
                    true
                } else {
                    false
                }
            }
            
            // Setup focus listeners to scroll input into view when keyboard appears
            // Store original focus listeners to chain them
            val originalEmailFocusListener = etEmail.onFocusChangeListener
            val originalPasswordFocusListener = etPassword.onFocusChangeListener
            val originalUsernameFocusListener = etUsername.onFocusChangeListener
            
            etEmail.setOnFocusChangeListener { view, hasFocus ->
                originalEmailFocusListener?.onFocusChange(view, hasFocus)
                if (hasFocus) {
                    scrollToView(view)
                }
            }
            
            etPassword.setOnFocusChangeListener { view, hasFocus ->
                originalPasswordFocusListener?.onFocusChange(view, hasFocus)
                if (hasFocus) {
                    scrollToView(view)
                }
            }
            
            etUsername.setOnFocusChangeListener { view, hasFocus ->
                originalUsernameFocusListener?.onFocusChange(view, hasFocus)
                if (hasFocus) {
                    scrollToView(view)
                }
            }
        }
    }
    
    /**
     * Scroll to view with keyboard padding to ensure it's visible
     */
    private fun scrollToView(view: View) {
        // Find the ScrollView in the layout hierarchy
        var parent = view.parent
        while (parent != null && parent !is android.widget.ScrollView) {
            parent = parent.parent
        }
        
        (parent as? android.widget.ScrollView)?.let { scrollView ->
            // Post delayed to ensure keyboard is shown
            view.postDelayed({
                val rect = android.graphics.Rect()
                view.getGlobalVisibleRect(rect)
                
                // Calculate scroll position with 16dp padding
                val keyboardPadding = resources.getDimensionPixelSize(R.dimen.auth_keyboard_padding)
                val scrollY = rect.top - keyboardPadding
                scrollView.smoothScrollTo(0, scrollY)
            }, 100)
        }
    }

    private fun setupUI() {
        // Setup accessibility content descriptions for all interactive elements
        setupAccessibilityDescriptions()
        
        binding.apply {
            // Setup button press animation with scale down/up on touch events
            btnSignIn.setOnTouchListener { view, event ->
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
                false // Return false to allow click event to proceed
            }
            
            btnSignIn.setOnClickListener {
                // Add haptic feedback on button press
                // CONTEXT_CLICK provides a light haptic feedback within 50ms requirement
                it.performHapticFeedback(
                    HapticFeedbackConstants.CONTEXT_CLICK,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
                
                if (isSignUpMode) {
                    performSignUp()
                } else {
                    performSignIn()
                }
            }
            
            tvToggleMode.setOnClickListener {
                toggleMode()
            }
            
            btnResendVerification.setOnClickListener {
                resendVerificationEmail()
            }
            
            btnBackToSignIn.setOnClickListener {
                handleBackToSignIn()
            }
            
            // Forgot password link click listener
            tvForgotPassword?.setOnClickListener {
                handleForgotPasswordClick()
            }
            
            // Social authentication button click listeners
            btnGoogleAuth?.setOnClickListener {
                handleSocialAuthClick("Google")
            }
            
            btnFacebookAuth?.setOnClickListener {
                handleSocialAuthClick("Facebook")
            }
            
            btnAppleAuth?.setOnClickListener {
                handleSocialAuthClick("Apple")
            }
            
            // Setup social button press animations
            setupSocialButtonAnimations()
            
            // Real-time email validation with debounce
            etEmail.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tilEmail.error = null
                    cardError.visibility = View.GONE
                    
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
            
            // Real-time password validation with debounce and strength indicator
            etPassword.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tilPassword.error = null
                    cardError.visibility = View.GONE
                }
                override fun afterTextChanged(s: android.text.Editable?) {
                    // Cancel previous validation
                    passwordValidationRunnable?.let { passwordValidationHandler.removeCallbacks(it) }
                    
                    // Schedule new validation with 300ms debounce
                    passwordValidationRunnable = Runnable {
                        validatePasswordRealtime(s?.toString() ?: "")
                    }
                    passwordValidationHandler.postDelayed(passwordValidationRunnable!!, 300)
                }
            })
            
            etUsername.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tilUsername.error = null
                    cardError.visibility = View.GONE
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
    }

    /**
     * Apply blur effect to background content when loading overlay is visible (API 31+)
     */
    private fun applyBlurEffect() {
        // Check if device supports RenderEffect (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Apply blur effect with 25f radius and CLAMP tile mode
                val blurEffect = RenderEffect.createBlurEffect(
                    25f, 25f,
                    Shader.TileMode.CLAMP
                )
                
                // Apply to the ScrollView which contains all the background content
                // The ScrollView is the first child of the CoordinatorLayout (binding.root)
                val scrollView = binding.root.getChildAt(0)
                scrollView?.setRenderEffect(blurEffect)
            } catch (e: Exception) {
                android.util.Log.w("AuthActivity", "Failed to apply blur effect: ${e.message}")
            }
        }
    }
    
    /**
     * Remove blur effect from background content when loading overlay is hidden (API 31+)
     */
    private fun removeBlurEffect() {
        // Check if device supports RenderEffect (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Remove blur effect from the ScrollView
                val scrollView = binding.root.getChildAt(0)
                scrollView?.setRenderEffect(null)
            } catch (e: Exception) {
                android.util.Log.w("AuthActivity", "Failed to remove blur effect: ${e.message}")
            }
        }
    }
    
    /**
     * Update UI state based on current authentication state
     */
    private fun updateUIState(state: AuthUIState) {
        currentState = state
        
        binding.apply {
            // Clear all errors first
            tilEmail.error = null
            tilPassword.error = null
            tilUsername.error = null
            
            when (state) {
                is AuthUIState.Loading -> {
                    loadingOverlay.visibility = View.VISIBLE
                    cardError.visibility = View.GONE
                    
                    // Update loading state transitions with smooth fade
                    if (!shouldReduceMotion()) {
                        loadingOverlay.alpha = 0f
                        loadingOverlay.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    } else {
                        loadingOverlay.alpha = 1f
                    }
                    
                    setButtonLoadingState(true)
                    
                    // Apply blur effect to background content (API 31+)
                    applyBlurEffect()
                    
                    // Note: Loading message is set by the caller using setLoadingMessage()
                }
                
                is AuthUIState.SignInForm -> {
                    isSignUpMode = false
                    loadingOverlay.visibility = View.GONE
                    
                    // Remove blur effect when hiding loading overlay
                    removeBlurEffect()
                    
                    layoutMainForm.visibility = View.VISIBLE
                    layoutEmailVerification.visibility = View.GONE
                    
                    // Show forgot password link in sign-in mode
                    tvForgotPassword?.visibility = View.VISIBLE
                    
                    // Hide password strength indicator in sign-in mode with fade out
                    if (layoutPasswordStrength.visibility == View.VISIBLE) {
                        if (!shouldReduceMotion()) {
                            layoutPasswordStrength.animate()
                                .alpha(0f)
                                .setDuration(150)
                                .withEndAction {
                                    layoutPasswordStrength.visibility = View.GONE
                                    layoutPasswordStrength.alpha = 1f
                                }
                                .start()
                        } else {
                            layoutPasswordStrength.visibility = View.GONE
                        }
                    }
                    
                    // Animate username field out with smooth slide up transition (300ms)
                    if (tilUsername.visibility == View.VISIBLE) {
                        if (!shouldReduceMotion()) {
                            tilUsername.animate()
                                .alpha(0f)
                                .translationY(-20f)
                                .setDuration(300)
                                .withEndAction {
                                    tilUsername.visibility = View.GONE
                                    tilUsername.alpha = 1f
                                    tilUsername.translationY = 0f
                                }
                                .start()
                        } else {
                            tilUsername.visibility = View.GONE
                        }
                    }
                    
                    setButtonEnabledState(true)
                    
                    // Update button text and icon smoothly with crossfade
                    if (!shouldReduceMotion()) {
                        btnSignIn.animate()
                            .alpha(0f)
                            .setDuration(75)
                            .withEndAction {
                                btnSignIn.text = "Sign In"
                                setButtonIcon(R.drawable.ic_arrow_forward)
                                btnSignIn.animate()
                                    .alpha(1f)
                                    .setDuration(75)
                                    .start()
                            }
                            .start()
                    } else {
                        btnSignIn.text = "Sign In"
                        setButtonIcon(R.drawable.ic_arrow_forward)
                    }
                    
                    // Update accessibility descriptions for sign in mode
                    updateAccessibilityForMode(false)
                    
                    // Crossfade welcome text (150ms)
                    if (!shouldReduceMotion()) {
                        tvWelcome.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction {
                                tvWelcome.text = "Welcome back"
                                tvWelcome.animate()
                                    .alpha(1f)
                                    .setDuration(150)
                                    .start()
                            }
                            .start()
                    } else {
                        tvWelcome.text = "Welcome back"
                    }
                    
                    // Crossfade toggle text (150ms)
                    if (!shouldReduceMotion()) {
                        val toggleButton = tvToggleMode
                        toggleButton.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction {
                                (toggleButton as? android.widget.TextView)?.text = "Don't have an account? Sign Up"
                                toggleButton.animate()
                                    .alpha(1f)
                                    .setDuration(150)
                                    .start()
                            }
                            .start()
                    } else {
                        (tvToggleMode as? android.widget.TextView)?.text = "Don't have an account? Sign Up"
                    }
                    
                    cardError.visibility = View.GONE
                }
                
                is AuthUIState.SignUpForm -> {
                    isSignUpMode = true
                    loadingOverlay.visibility = View.GONE
                    
                    // Remove blur effect when hiding loading overlay
                    removeBlurEffect()
                    
                    layoutMainForm.visibility = View.VISIBLE
                    layoutEmailVerification.visibility = View.GONE
                    
                    // Hide forgot password link in sign-up mode
                    tvForgotPassword?.visibility = View.GONE
                    
                    // Trigger password validation to show strength indicator if password exists
                    val currentPassword = etPassword.text?.toString() ?: ""
                    if (currentPassword.isNotEmpty()) {
                        validatePasswordRealtime(currentPassword)
                    }
                    
                    // Animate username field in with smooth slide down transition (300ms)
                    if (tilUsername.visibility != View.VISIBLE) {
                        tilUsername.visibility = View.VISIBLE
                        if (!shouldReduceMotion()) {
                            tilUsername.alpha = 0f
                            tilUsername.translationY = -20f
                            tilUsername.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(300)
                                .start()
                        } else {
                            tilUsername.alpha = 1f
                            tilUsername.translationY = 0f
                        }
                    }
                    
                    setButtonEnabledState(true)
                    
                    // Update button text and icon smoothly with crossfade
                    if (!shouldReduceMotion()) {
                        btnSignIn.animate()
                            .alpha(0f)
                            .setDuration(75)
                            .withEndAction {
                                btnSignIn.text = "Create Account"
                                setButtonIcon(R.drawable.ic_arrow_forward)
                                btnSignIn.animate()
                                    .alpha(1f)
                                    .setDuration(75)
                                    .start()
                            }
                            .start()
                    } else {
                        btnSignIn.text = "Create Account"
                        setButtonIcon(R.drawable.ic_arrow_forward)
                    }
                    
                    // Update accessibility descriptions for sign up mode
                    updateAccessibilityForMode(true)
                    
                    // Crossfade welcome text (150ms)
                    if (!shouldReduceMotion()) {
                        tvWelcome.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction {
                                tvWelcome.text = "Create your account"
                                tvWelcome.animate()
                                    .alpha(1f)
                                    .setDuration(150)
                                    .start()
                            }
                            .start()
                    } else {
                        tvWelcome.text = "Create your account"
                    }
                    
                    // Crossfade toggle text (150ms)
                    if (!shouldReduceMotion()) {
                        val toggleButton = tvToggleMode
                        toggleButton.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction {
                                (toggleButton as? android.widget.TextView)?.text = "Already have an account? Sign In"
                                toggleButton.animate()
                                    .alpha(1f)
                                    .setDuration(150)
                                    .start()
                            }
                            .start()
                    } else {
                        (tvToggleMode as? android.widget.TextView)?.text = "Already have an account? Sign In"
                    }
                    
                    cardError.visibility = View.GONE
                }
                
                is AuthUIState.EmailVerificationPending -> {
                    loadingOverlay.visibility = View.GONE
                    
                    // Remove blur effect when hiding loading overlay
                    removeBlurEffect()
                    
                    // Enhanced verification screen transition with fade and slide
                    layoutMainForm.animate()
                        .alpha(0f)
                        .translationY(-50f)
                        .setDuration(300)
                        .withEndAction {
                            layoutMainForm.visibility = View.GONE
                            layoutMainForm.alpha = 1f
                            layoutMainForm.translationY = 0f
                            
                            layoutEmailVerification.visibility = View.VISIBLE
                            layoutEmailVerification.alpha = 0f
                            layoutEmailVerification.translationY = 50f
                            layoutEmailVerification.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(300)
                                .setStartDelay(100)
                                .withEndAction {
                                    // Start animated email icon after transition completes
                                    startEmailIconAnimation()
                                }
                                .start()
                        }
                        .start()
                    
                    tvVerificationEmail.text = state.email
                    
                    // Save email for resend functionality
                    saveEmailForResend(state.email)
                    
                    // Start automatic verification checking for auto sign in
                    startVerificationChecking(state.email)
                }
                
                is AuthUIState.Authenticated -> {
                    // Show success state before navigation
                    showButtonSuccessState()
                }
                
                is AuthUIState.Error -> {
                    loadingOverlay.visibility = View.GONE
                    
                    // Remove blur effect when hiding loading overlay
                    removeBlurEffect()
                    
                    setButtonEnabledState(true)
                    resetSignInButton()
                    
                    // Use the enhanced showError function
                    // Note: This state is kept for backward compatibility
                    // but new code should call showError() directly
                    showError(state.message, ErrorField.GENERAL)
                }
            }
        }
    }
    
    /**
     * Set loading message with fade transition animation
     */
    private fun setLoadingMessage(message: String) {
        binding.tvLoadingMessage.apply {
            // Fade out current message
            animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    // Update text
                    text = message
                    // Fade in new message
                    animate()
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        }
    }
    
    /**
     * Set button to enabled state with proper styling
     */
    private fun setButtonEnabledState(enabled: Boolean) {
        binding.btnSignIn.apply {
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.6f
        }
    }
    
    /**
     * Set button to loading state with progress indicator
     */
    private fun setButtonLoadingState(loading: Boolean) {
        val reduceMotion = shouldReduceMotion()
        
        binding.btnSignIn.apply {
            if (loading) {
                // Disable button and show loading state with smooth transition
                isEnabled = false
                
                if (!reduceMotion) {
                    animate()
                        .alpha(0.6f)
                        .setDuration(150)
                        .withEndAction {
                            text = ""
                            icon = null
                        }
                        .start()
                } else {
                    alpha = 0.6f
                    text = ""
                    icon = null
                }
                
                // Note: For a true inline progress indicator, we would need to add
                // a ProgressBar to the button layout. For now, we use the overlay.
            } else {
                // Reset to normal state with smooth transition
                if (!reduceMotion) {
                    text = if (isSignUpMode) "Create Account" else "Sign In"
                    setButtonIcon(R.drawable.ic_arrow_forward)
                    animate()
                        .alpha(1f)
                        .setDuration(150)
                        .withEndAction {
                            setButtonEnabledState(true)
                        }
                        .start()
                } else {
                    setButtonEnabledState(true)
                    text = if (isSignUpMode) "Create Account" else "Sign In"
                    setButtonIcon(R.drawable.ic_arrow_forward)
                }
            }
        }
    }
    
    /**
     * Set button icon with proper configuration
     */
    private fun setButtonIcon(iconRes: Int) {
        binding.btnSignIn.apply {
            icon = ContextCompat.getDrawable(this@AuthActivity, iconRes)
            iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_END
        }
    }
    
    /**
     * Show brief success state with checkmark icon before navigation
     */
    private fun showButtonSuccessState() {
        val reduceMotion = shouldReduceMotion()
        
        binding.btnSignIn.apply {
            // Show success state
            isEnabled = false
            text = "Success!"
            icon = ContextCompat.getDrawable(this@AuthActivity, R.drawable.ic_check_circle)
            iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_START
            iconTint = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this@AuthActivity, R.color.success_green)
            )
            
            // Add haptic feedback on success
            // CONFIRM provides a positive haptic feedback within 50ms requirement
            performHapticFeedback(
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            
            // Enhanced success state with pulse (1.05x → 1.0x)
            if (!reduceMotion) {
                animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(150)
                    .withEndAction {
                        animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start()
                    }
                    .start()
            }
        }
        
        // Start fade out animation for entire auth screen after brief success display
        binding.root.postDelayed({
            // Add screen fade-out before navigation (300ms)
            if (!reduceMotion) {
                binding.root.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        // Navigate to main after fade out completes
                        navigateToMain()
                    }
                    .start()
            } else {
                // Skip fade animation if reduced motion is enabled
                navigateToMain()
            }
        }, 500) // Delay navigation by 500ms to show success feedback
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
        
        val email = getSavedEmailForResend()
        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "No email found for resend", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading state for resend button
        binding.btnResendVerification.isEnabled = false
        binding.btnResendVerification.text = "Sending..."
        
        lifecycleScope.launch {
            try {
                // Note: Resend functionality may need to be implemented differently
                // For now, just show a message
                // SupabaseClient.client.auth.resend(email = email)
                Toast.makeText(this@AuthActivity, "Verification email sent! Please check your inbox.", Toast.LENGTH_LONG).show()
                startResendCooldown()
                
                // Restart verification checking after resend
                startVerificationChecking(email)
            } catch (e: Exception) {
                Toast.makeText(this@AuthActivity, "Failed to resend: ${e.message}", Toast.LENGTH_LONG).show()
                resetResendButton()
            }
        }
    }

    /**
     * Start cooldown timer for resend verification button with circular progress indicator
     */
    private fun startResendCooldown() {
        isResendCooldownActive = true
        resendCooldownSeconds = 60 // 60 second cooldown
        val totalSeconds = 60
        
        binding.apply {
            btnResendVerification.isEnabled = false
            layoutResendCooldown.visibility = View.VISIBLE
            progressResendCooldown.max = totalSeconds
            progressResendCooldown.progress = totalSeconds
        }
        
        lifecycleScope.launch {
            while (resendCooldownSeconds > 0) {
                binding.apply {
                    tvResendCooldown.text = resendCooldownSeconds.toString()
                    progressResendCooldown.setProgressCompat(resendCooldownSeconds, true)
                }
                delay(1000)
                resendCooldownSeconds--
            }
            
            // Cooldown finished - start pulsing animation on resend button
            isResendCooldownActive = false
            binding.apply {
                btnResendVerification.isEnabled = true
                btnResendVerification.text = "Resend Verification Email"
                layoutResendCooldown.visibility = View.GONE
                
                // Start pulsing animation to draw attention
                startResendButtonPulseAnimation()
            }
        }
    }
    
    /**
     * Start pulsing animation on resend button when it becomes available
     */
    private fun startResendButtonPulseAnimation() {
        if (shouldReduceMotion()) {
            return
        }
        
        binding.btnResendVerification.apply {
            val pulseAnimation = AnimationUtils.loadAnimation(this@AuthActivity, R.anim.pulse)
            startAnimation(pulseAnimation)
            
            // Stop animation after 3 cycles (6 seconds)
            postDelayed({
                clearAnimation()
            }, 6000)
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
                // Show green checkmark icon with smooth transition
                tilEmail.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
                tilEmail.setEndIconDrawable(android.R.drawable.checkbox_on_background)
                tilEmail.setEndIconTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@AuthActivity, R.color.success_green)
                ))
                tilEmail.error = null
                
                // Announce success for accessibility
                tilEmail.announceForAccessibility("Email address is valid")
                
                // Add validation success pulse animation (1.01x → 1.0x)
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
                    ContextCompat.getColor(this@AuthActivity, R.color.error_red)
                ))
                val errorMessage = "Please enter a valid email address"
                tilEmail.error = errorMessage
                
                // Announce error for accessibility (TalkBack)
                tilEmail.announceForAccessibility("Email validation error: $errorMessage")
                
                // Refine validation error shake animation
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
                                .start()
                        }
                        .start()
                }
            }
        }
    }
    
    /**
     * Validate password in real-time with strength indicator
     */
    private fun validatePasswordRealtime(password: String) {
        binding.apply {
            if (password.isEmpty()) {
                // Hide strength indicator for empty password
                layoutPasswordStrength.visibility = View.GONE
                return
            }
            
            // Only show strength indicator in sign-up mode
            if (!isSignUpMode) {
                layoutPasswordStrength.visibility = View.GONE
                return
            }
            
            // Evaluate password strength
            val strength = evaluatePasswordStrength(password)
            val reduceMotion = shouldReduceMotion()
            
            // Show strength indicator with animation
            if (layoutPasswordStrength.visibility != View.VISIBLE) {
                layoutPasswordStrength.visibility = View.VISIBLE
                if (!reduceMotion) {
                    layoutPasswordStrength.alpha = 0f
                    layoutPasswordStrength.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                } else {
                    layoutPasswordStrength.alpha = 1f
                }
            }
            
            // Update progress bar color and value with animation
            val color = ContextCompat.getColor(this@AuthActivity, strength.color)
            progressPasswordStrength.setIndicatorColor(color)
            progressPasswordStrength.setProgressCompat(strength.progress, true)
            
            // Update helper text with color
            tvPasswordStrength.text = strength.message
            tvPasswordStrength.setTextColor(color)
            
            // Announce password strength for accessibility (TalkBack)
            layoutPasswordStrength.announceForAccessibility("Password strength: ${strength.message}")
            
            // Animate text change
            if (!reduceMotion) {
                tvPasswordStrength.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(100)
                    .withEndAction {
                        tvPasswordStrength.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }
        }
    }
    
    /**
     * Evaluate password strength based on length and complexity
     */
    private fun evaluatePasswordStrength(password: String): PasswordStrength {
        val length = password.length
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        val complexityScore = listOf(hasUpperCase, hasLowerCase, hasDigit, hasSpecialChar).count { it }
        
        return when {
            length < 6 -> PasswordStrength.Weak
            length < 8 && complexityScore < 2 -> PasswordStrength.Weak
            length < 10 && complexityScore < 3 -> PasswordStrength.Fair
            length >= 10 && complexityScore >= 3 -> PasswordStrength.Strong
            length >= 8 && complexityScore >= 2 -> PasswordStrength.Fair
            else -> PasswordStrength.Weak
        }
    }
    
    /**
     * Enhanced error display with shake animation, haptic feedback, and field-specific highlighting
     */
    private fun showError(message: String, field: ErrorField = ErrorField.GENERAL) {
        binding.apply {
            // Update error message
            tvErrorMessage.text = message
            cardError.visibility = View.VISIBLE
            
            // Load and apply shake animation from resources
            val shakeAnimation = AnimationUtils.loadAnimation(this@AuthActivity, R.anim.shake)
            cardError.startAnimation(shakeAnimation)
            
            // Fade in animation
            cardError.alpha = 0f
            cardError.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
            
            // Add haptic feedback using HapticFeedbackConstants.REJECT
            // REJECT provides a strong haptic feedback for errors within 50ms requirement
            cardError.performHapticFeedback(
                HapticFeedbackConstants.REJECT,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            
            // Announce error for accessibility (TalkBack)
            // This ensures screen readers announce the error message with descriptive context
            val accessibilityMessage = when (field) {
                ErrorField.EMAIL -> "Email error: $message"
                ErrorField.PASSWORD -> "Password error: $message"
                ErrorField.USERNAME -> "Username error: $message"
                ErrorField.GENERAL -> "Error: $message"
            }
            cardError.announceForAccessibility(accessibilityMessage)
            
            // Implement field-specific error highlighting with animated borders
            highlightErrorField(field)
            
            // Auto-focus to first invalid field
            focusFirstInvalidField(field)
        }
    }
    
    /**
     * Highlight specific error field with animated border
     */
    private fun highlightErrorField(field: ErrorField) {
        binding.apply {
            // Clear previous highlights
            clearFieldHighlights()
            
            val targetLayout = when (field) {
                ErrorField.EMAIL -> tilEmail
                ErrorField.PASSWORD -> tilPassword
                ErrorField.USERNAME -> tilUsername
                ErrorField.GENERAL -> null
            }
            
            targetLayout?.let { layout ->
                // Animate the field with shake effect
                layout.animate()
                    .translationX(-10f)
                    .setDuration(50)
                    .withEndAction {
                        layout.animate()
                            .translationX(10f)
                            .setDuration(50)
                            .withEndAction {
                                layout.animate()
                                    .translationX(-5f)
                                    .setDuration(50)
                                    .withEndAction {
                                        layout.animate()
                                            .translationX(5f)
                                            .setDuration(50)
                                            .withEndAction {
                                                layout.animate()
                                                    .translationX(0f)
                                                    .setDuration(50)
                                                    .start()
                                            }
                                            .start()
                                    }
                                    .start()
                            }
                            .start()
                    }
                    .start()
                
                // Set error icon and color
                when (field) {
                    ErrorField.EMAIL -> {
                        tilEmail.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
                        tilEmail.setEndIconDrawable(android.R.drawable.ic_dialog_alert)
                        tilEmail.setEndIconTintList(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(this@AuthActivity, R.color.error_red)
                        ))
                    }
                    ErrorField.PASSWORD -> {
                        // Password field already has toggle, so we'll just set error state
                    }
                    ErrorField.USERNAME -> {
                        tilUsername.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
                        tilUsername.setEndIconDrawable(android.R.drawable.ic_dialog_alert)
                        tilUsername.setEndIconTintList(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(this@AuthActivity, R.color.error_red)
                        ))
                    }
                    ErrorField.GENERAL -> {}
                }
            }
        }
    }
    
    /**
     * Clear all field error highlights
     */
    private fun clearFieldHighlights() {
        binding.apply {
            tilEmail.error = null
            tilPassword.error = null
            tilUsername.error = null
            
            // Reset email end icon to validation state if email is valid
            val email = etEmail.text?.toString() ?: ""
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
                tilEmail.setEndIconDrawable(android.R.drawable.checkbox_on_background)
                tilEmail.setEndIconTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@AuthActivity, R.color.success_green)
                ))
            } else if (email.isEmpty()) {
                tilEmail.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
            }
        }
    }
    
    /**
     * Auto-focus to first invalid field on validation failure
     */
    private fun focusFirstInvalidField(field: ErrorField) {
        binding.apply {
            val targetEditText = when (field) {
                ErrorField.EMAIL -> etEmail
                ErrorField.PASSWORD -> etPassword
                ErrorField.USERNAME -> etUsername
                ErrorField.GENERAL -> null
            }
            
            targetEditText?.let {
                it.requestFocus()
                // Show keyboard
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(it, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }
    
    /**
     * Get user-friendly error message based on exception
     */
    private fun getUserFriendlyErrorMessage(exception: Exception, context: String): Pair<String, ErrorField> {
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            // Network errors
            message.contains("network") || message.contains("connection") || message.contains("timeout") -> {
                Pair("Unable to connect to the server. Please check your internet connection and try again.", ErrorField.GENERAL)
            }
            
            // Authentication errors
            message.contains("invalid login credentials") || message.contains("invalid credentials") -> {
                Pair("The email or password you entered is incorrect. Please check your credentials and try again.", ErrorField.EMAIL)
            }
            
            message.contains("invalid") && context == "signin" -> {
                Pair("Invalid email or password. Please double-check your credentials.", ErrorField.EMAIL)
            }
            
            // Email verification errors
            message.contains("email not confirmed") || message.contains("email not verified") -> {
                Pair("Your email address has not been verified yet. Please check your inbox for the verification link.", ErrorField.GENERAL)
            }
            
            // Account exists errors
            message.contains("already registered") || message.contains("user already registered") -> {
                Pair("An account with this email address already exists. Please sign in instead or use a different email.", ErrorField.EMAIL)
            }
            
            // Email format errors
            message.contains("invalid email") || message.contains("email format") -> {
                Pair("Please enter a valid email address (e.g., example@email.com).", ErrorField.EMAIL)
            }
            
            // Password errors
            message.contains("password") && message.contains("weak") -> {
                Pair("Your password is too weak. Please use at least 6 characters with a mix of letters and numbers.", ErrorField.PASSWORD)
            }
            
            message.contains("password") && message.contains("short") -> {
                Pair("Your password is too short. Please use at least 6 characters.", ErrorField.PASSWORD)
            }
            
            // Rate limiting
            message.contains("rate limit") || message.contains("too many") -> {
                Pair("Too many attempts. Please wait a few minutes before trying again.", ErrorField.GENERAL)
            }
            
            // Server errors
            message.contains("500") || message.contains("server error") -> {
                Pair("The server is experiencing issues. Please try again in a few moments.", ErrorField.GENERAL)
            }
            
            // Default error messages
            else -> {
                when (context) {
                    "signin" -> Pair("Unable to sign in. Please check your email and password.", ErrorField.EMAIL)
                    "signup" -> Pair("Unable to create your account. Please try again or contact support if the problem persists.", ErrorField.GENERAL)
                    else -> Pair(exception.message ?: "An unexpected error occurred. Please try again.", ErrorField.GENERAL)
                }
            }
        }
    }

    private fun checkCurrentUser() {
        // First check if Supabase is configured
        if (!SupabaseClient.isConfigured()) {
            updateUIState(AuthUIState.Error("Supabase not configured. Please set up your credentials in gradle.properties"))
            return
        }
        
        // Check if there's a pending verification email
        val pendingEmail = getSavedEmailForResend()
        if (!pendingEmail.isNullOrEmpty()) {
            updateUIState(AuthUIState.EmailVerificationPending(pendingEmail))
            return
        }
        
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser != null && currentUser.id.isNotEmpty()) {
                    // User is authenticated, navigate to main
                    updateUIState(AuthUIState.Authenticated(currentUser))
                } else {
                    // User not authenticated, show sign in form
                    updateUIState(AuthUIState.SignInForm)
                    android.util.Log.d("AuthActivity", "No authenticated user found")
                }
            } catch (e: Exception) {
                // User not authenticated, show sign in form
                updateUIState(AuthUIState.SignInForm)
                android.util.Log.e("AuthActivity", "Error checking current user: ${e.message}")
            }
        }
    }



    private fun performSignIn() {
        // Check if Supabase is configured
        if (!SupabaseClient.isConfigured()) {
            showError("Supabase is not configured. Please check your setup and try again.", ErrorField.GENERAL)
            return
        }
        
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (validateInput(email, password)) {
            // Show loading state with contextual message
            updateUIState(AuthUIState.Loading)
            setLoadingMessage("Signing you in...")
            
            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    
                    val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                    if (currentUser?.emailConfirmedAt != null) {
                        // Successful authentication - clear any saved email and navigate
                        clearSavedEmail()
                        updateUIState(AuthUIState.Authenticated(currentUser))
                    } else {
                        // Email verification required - navigate to EmailVerificationActivity
                        Toast.makeText(this@AuthActivity, "Please verify your email address to continue", Toast.LENGTH_LONG).show()
                        navigateToEmailVerification(email, password)
                    }
                } catch (e: Exception) {
                    // Clean up session on exception
                    cleanupFailedSession()
                    
                    val isEmailNotVerified = e.message?.contains("email not confirmed", ignoreCase = true) == true ||
                                           e.message?.contains("Email not confirmed", ignoreCase = true) == true
                    
                    if (isEmailNotVerified) {
                        // Navigate to EmailVerificationActivity for unverified email
                        Toast.makeText(this@AuthActivity, "Please verify your email address to continue", Toast.LENGTH_LONG).show()
                        navigateToEmailVerification(email, password)
                    } else {
                        // Use enhanced error handling with user-friendly messages
                        val (errorMessage, errorField) = getUserFriendlyErrorMessage(e, "signin")
                        updateUIState(AuthUIState.SignInForm) // Return to sign in form
                        showError(errorMessage, errorField)
                    }
                }
            }
        }
    }

    private fun resetSignInButton() {
        binding.btnSignIn.apply {
            setButtonEnabledState(true)
            text = if (isSignUpMode) "Create Account" else "Sign In"
            setButtonIcon(R.drawable.ic_arrow_forward)
            // Reset icon tint to default
            iconTint = null
        }
    }

    private fun performSignUp() {
        // Check if Supabase is configured
        if (!SupabaseClient.isConfigured()) {
            showError("Supabase is not configured. Please check your setup and try again.", ErrorField.GENERAL)
            return
        }
        
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        
        if (validateInput(email, password, username)) {
            // Show loading state with contextual message
            updateUIState(AuthUIState.Loading)
            setLoadingMessage("Creating your account...")
            
            lifecycleScope.launch {
                try {
                    android.util.Log.d("AuthActivity", "Starting sign up for email: $email")
                    SupabaseClient.client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    
                    val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                    if (currentUser?.emailConfirmedAt != null) {
                        // Account created and verified - proceed to main
                        Toast.makeText(this@AuthActivity, "✅ Account created successfully!", Toast.LENGTH_SHORT).show()
                        clearSavedEmail()
                        navigateToMain()
                    } else {
                        // Email verification required - navigate to EmailVerificationActivity
                        Toast.makeText(this@AuthActivity, "✅ Account created successfully! Please check your email for verification.", Toast.LENGTH_LONG).show()
                        navigateToEmailVerification(email, password)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AuthActivity", "Sign up failed: ${e.message}", e)
                    
                    // Check if this is actually a successful creation with verification needed
                    if (e.message?.contains("email not confirmed", ignoreCase = true) == true ||
                        e.message?.contains("Email not confirmed", ignoreCase = true) == true) {
                        // Account was created but needs verification
                        android.util.Log.d("AuthActivity", "Account created but needs email verification")
                        Toast.makeText(this@AuthActivity, "✅ Account created successfully! Please check your email for verification.", Toast.LENGTH_LONG).show()
                        navigateToEmailVerification(email, password)
                        return@launch
                    }
                    
                    // Clean up session on actual sign up failure
                    cleanupFailedSession()
                    
                    // Use enhanced error handling with user-friendly messages
                    val (errorMessage, errorField) = getUserFriendlyErrorMessage(e, "signup")
                    android.util.Log.e("AuthActivity", "Showing error to user: $errorMessage")
                    updateUIState(AuthUIState.SignUpForm) // Return to sign up form
                    showError(errorMessage, errorField)
                }
            }
        }
    }

    private fun validateInput(email: String, password: String, username: String? = null): Boolean {
        var isValid = true
        var firstErrorField: ErrorField? = null
        var firstErrorMessage: String? = null
        
        binding.apply {
            // Clear previous errors
            cardError.visibility = View.GONE
            
            // Validate email
            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                if (firstErrorField == null) {
                    firstErrorField = ErrorField.EMAIL
                    firstErrorMessage = "Please enter your email address to continue."
                }
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Please enter a valid email address"
                if (firstErrorField == null) {
                    firstErrorField = ErrorField.EMAIL
                    firstErrorMessage = "Please enter a valid email address (e.g., example@email.com)."
                }
                isValid = false
            }
            
            // Validate password
            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                if (firstErrorField == null) {
                    firstErrorField = ErrorField.PASSWORD
                    firstErrorMessage = "Please enter your password to continue."
                }
                isValid = false
            } else if (password.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
                if (firstErrorField == null) {
                    firstErrorField = ErrorField.PASSWORD
                    firstErrorMessage = "Your password must be at least 6 characters long for security."
                }
                isValid = false
            }
            
            // Validate username (only in sign-up mode)
            if (isSignUpMode && username.isNullOrEmpty()) {
                tilUsername.error = "Username is required"
                if (firstErrorField == null) {
                    firstErrorField = ErrorField.USERNAME
                    firstErrorMessage = "Please choose a username for your account."
                }
                isValid = false
            } else if (isSignUpMode && username != null && username.length < 3) {
                tilUsername.error = "Username must be at least 3 characters"
                if (firstErrorField == null) {
                    firstErrorField = ErrorField.USERNAME
                    firstErrorMessage = "Your username must be at least 3 characters long."
                }
                isValid = false
            }
        }
        
        // Show enhanced error if validation failed
        if (!isValid && firstErrorField != null && firstErrorMessage != null) {
            showError(firstErrorMessage, firstErrorField)
        }
        
        return isValid
    }

    private fun toggleMode() {
        // Add haptic feedback using HapticFeedbackConstants
        // CONTEXT_CLICK provides a light haptic feedback within 50ms requirement
        binding.tvToggleMode.performHapticFeedback(
            HapticFeedbackConstants.CONTEXT_CLICK,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
        
        if (isSignUpMode) {
            updateUIState(AuthUIState.SignInForm)
        } else {
            updateUIState(AuthUIState.SignUpForm)
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /**
     * Navigate to EmailVerificationActivity for dedicated verification flow
     */
    private fun navigateToEmailVerification(email: String, password: String) {
        val intent = Intent(this, EmailVerificationActivity::class.java)
        intent.putExtra(EmailVerificationActivity.EXTRA_EMAIL, email)
        intent.putExtra(EmailVerificationActivity.EXTRA_PASSWORD, password)
        startActivity(intent)
        // Don't finish() here so user can come back if needed
    }

    /**
     * Clean up session for failed authentication attempts
     */
    private fun cleanupFailedSession() {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
            } catch (e: Exception) {
                // Ignore sign out errors during cleanup
                android.util.Log.w("AuthActivity", "Failed to clean up session: ${e.message}")
            }
        }
    }

    /**
     * Implement automatic retry of sign in after email verification
     */
    private fun attemptAutoSignInAfterVerification(email: String, password: String) {
        lifecycleScope.launch {
            try {
                // Try to sign in to check if email is now verified
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser?.emailConfirmedAt != null) {
                    // Email is verified, sign in successful
                    android.util.Log.d("AuthActivity", "Email verified, automatic sign in successful")
                    clearSavedEmail()
                    Toast.makeText(this@AuthActivity, "Email verified! Welcome back.", Toast.LENGTH_SHORT).show()
                    updateUIState(AuthUIState.Authenticated(currentUser))
                } else {
                    android.util.Log.d("AuthActivity", "Email not yet verified")
                }
            } catch (e: Exception) {
                android.util.Log.w("AuthActivity", "Error during auto sign in check: ${e.message}")
                // Don't show error to user for auto sign in failures
            }
        }
    }

    /**
     * Perform automatic sign in after email verification
     */
    private fun performAutoSignIn(email: String, password: String) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser?.emailConfirmedAt != null) {
                    // Successful authentication after verification
                    clearSavedEmail()
                    Toast.makeText(this@AuthActivity, "Email verified! Welcome back.", Toast.LENGTH_SHORT).show()
                    updateUIState(AuthUIState.Authenticated(currentUser))
                }
            } catch (e: Exception) {
                android.util.Log.w("AuthActivity", "Auto sign in exception: ${e.message}")
                // Don't show error to user for auto sign in failures
            }
        }
    }

    /**
     * Start periodic verification checking when in verification pending state
     * with enhanced auto-refresh indicator
     */
    private fun startVerificationChecking(email: String) {
        // Get the password from the form if available for auto sign in
        val password = binding.etPassword.text.toString().trim()
        
        if (password.isNotEmpty()) {
            lifecycleScope.launch {
                // Show auto-refresh indicator
                binding.layoutAutoRefresh.visibility = View.VISIBLE
                
                // Check verification status every 30 seconds for up to 10 minutes
                repeat(20) { attempt ->
                    delay(30000) // Wait 30 seconds
                    
                    // Only continue checking if still in verification pending state
                    if (currentState is AuthUIState.EmailVerificationPending) {
                        // Update auto-refresh message
                        binding.tvAutoRefresh.text = "Checking verification status... (${attempt + 1}/20)"
                        
                        // Pulse the auto-refresh indicator
                        if (!shouldReduceMotion()) {
                            binding.layoutAutoRefresh.animate()
                                .scaleX(1.05f)
                                .scaleY(1.05f)
                                .setDuration(200)
                                .withEndAction {
                                    binding.layoutAutoRefresh.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(200)
                                        .start()
                                }
                                .start()
                        }
                        
                        // Show loading overlay with verification message during check
                        binding.loadingOverlay.visibility = View.VISIBLE
                        applyBlurEffect()
                        setLoadingMessage("Verifying email...")
                        
                        attemptAutoSignInAfterVerification(email, password)
                        
                        // Hide loading overlay after check completes
                        delay(2000) // Give time for the check to complete
                        if (currentState is AuthUIState.EmailVerificationPending) {
                            binding.loadingOverlay.visibility = View.GONE
                            removeBlurEffect()
                        }
                    } else {
                        // Exit checking if state changed
                        binding.layoutAutoRefresh.visibility = View.GONE
                        return@launch
                    }
                }
                
                // Hide auto-refresh indicator after all attempts
                binding.layoutAutoRefresh.visibility = View.GONE
            }
        }
    }
    
    /**
     * Start animated email icon animation
     */
    private fun startEmailIconAnimation() {
        if (shouldReduceMotion()) {
            return
        }
        
        binding.ivEmailIcon.apply {
            // Check if the drawable is an AnimatedVectorDrawable
            val drawable = drawable
            if (drawable is android.graphics.drawable.AnimatedVectorDrawable) {
                drawable.start()
            } else {
                // Fallback to simple scale animation if not using animated vector
                scaleX = 0.8f
                scaleY = 0.8f
                alpha = 0f
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(600)
                    .setInterpolator(OvershootInterpolator(1.2f))
                    .start()
            }
        }
    }

    /**
     * Handle back to sign in from verification pending state with enhanced animations
     */
    private fun handleBackToSignIn() {
        // Clear saved email when going back to sign in
        clearSavedEmail()
        
        // Hide auto-refresh indicator if visible
        binding.layoutAutoRefresh.visibility = View.GONE
        
        // Enhanced animate transition back to sign in with fade and slide
        binding.apply {
            // Fade out email icon first
            if (!shouldReduceMotion()) {
                ivEmailIcon.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .alpha(0f)
                    .setDuration(200)
                    .start()
            }
            
            layoutEmailVerification.animate()
                .alpha(0f)
                .translationY(50f)
                .setDuration(300)
                .setStartDelay(if (shouldReduceMotion()) 0 else 100)
                .withEndAction {
                    layoutEmailVerification.visibility = View.GONE
                    layoutEmailVerification.alpha = 1f
                    layoutEmailVerification.translationY = 0f
                    
                    // Reset email icon for next time
                    ivEmailIcon.scaleX = 1f
                    ivEmailIcon.scaleY = 1f
                    ivEmailIcon.alpha = 1f
                    
                    // Reset form fields
                    etEmail.text?.clear()
                    etPassword.text?.clear()
                    etUsername.text?.clear()
                    
                    // Show main form with slide animation
                    layoutMainForm.visibility = View.VISIBLE
                    layoutMainForm.alpha = 0f
                    layoutMainForm.translationY = -50f
                    layoutMainForm.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setStartDelay(100)
                        .withEndAction {
                            // Navigate back to sign in form
                            updateUIState(AuthUIState.SignInForm)
                        }
                        .start()
                }
                .start()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Preserve form state on orientation changes
        binding.apply {
            outState.putString("email", etEmail.text?.toString())
            outState.putString("password", etPassword.text?.toString())
            outState.putString("username", etUsername.text?.toString())
            outState.putBoolean("isSignUpMode", isSignUpMode)
            outState.putInt("currentStateType", when (currentState) {
                is AuthUIState.Loading -> 0
                is AuthUIState.SignInForm -> 1
                is AuthUIState.SignUpForm -> 2
                is AuthUIState.EmailVerificationPending -> 3
                is AuthUIState.Authenticated -> 4
                is AuthUIState.Error -> 5
            })
            if (currentState is AuthUIState.EmailVerificationPending) {
                outState.putString("verificationEmail", (currentState as AuthUIState.EmailVerificationPending).email)
            }
        }
    }
    
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore form state after orientation changes
        binding.apply {
            savedInstanceState.getString("email")?.let { etEmail.setText(it) }
            savedInstanceState.getString("password")?.let { etPassword.setText(it) }
            savedInstanceState.getString("username")?.let { etUsername.setText(it) }
            
            val wasSignUpMode = savedInstanceState.getBoolean("isSignUpMode", false)
            val stateType = savedInstanceState.getInt("currentStateType", 1)
            
            // Restore UI state
            when (stateType) {
                0 -> {
                    updateUIState(AuthUIState.Loading)
                    setLoadingMessage("Please wait...")
                }
                1 -> updateUIState(AuthUIState.SignInForm)
                2 -> updateUIState(AuthUIState.SignUpForm)
                3 -> {
                    val email = savedInstanceState.getString("verificationEmail") ?: ""
                    if (email.isNotEmpty()) {
                        updateUIState(AuthUIState.EmailVerificationPending(email))
                    } else {
                        updateUIState(if (wasSignUpMode) AuthUIState.SignUpForm else AuthUIState.SignInForm)
                    }
                }
                else -> updateUIState(if (wasSignUpMode) AuthUIState.SignUpForm else AuthUIState.SignInForm)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up validation handlers to prevent memory leaks
        emailValidationRunnable?.let { emailValidationHandler.removeCallbacks(it) }
        passwordValidationRunnable?.let { passwordValidationHandler.removeCallbacks(it) }
    }
}
