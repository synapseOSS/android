package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UsernameRepository
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import com.synapse.social.studioasinc.feature.auth.ui.models.PasswordStrength
import com.synapse.social.studioasinc.feature.auth.ui.util.UsernameValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing authentication UI state and business logic.
 * Handles sign-in, sign-up, email verification, and password reset flows.
 * 
 * @param authRepository Repository for authentication operations
 * @param sharedPreferences SharedPreferences for storing user preferences
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val usernameRepository: UsernameRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Navigation events
    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent.asSharedFlow()

    // Debounced input flows for validation
    private val emailInputFlow = MutableStateFlow("")
    private val passwordInputFlow = MutableStateFlow("")
    private val usernameInputFlow = MutableStateFlow("")

    // Cooldown timer job
    private var cooldownJob: Job? = null

    companion object {
        private const val EMAIL_DEBOUNCE_MS = 300L
        private const val RESEND_COOLDOWN_SECONDS = 60
        private const val PREF_KEY_VERIFICATION_EMAIL = "verification_email"
        
        // Email validation regex - matches standard email format
        private val EMAIL_REGEX = Regex(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
        
        // Password validation constants
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MIN_USERNAME_LENGTH = 3
    }

    init {
        _uiState.value = AuthUiState.SignIn()
        setupInputValidation()
        observeAuthState()
        checkForOrphanedProfile()
    }

    /**
     * Check for orphaned auth users and ensure profiles exist
     */
    private fun checkForOrphanedProfile() {
        viewModelScope.launch {
            // Check current user
            val currentUserId = authRepository.getCurrentUserId()
            val currentEmail = authRepository.getCurrentUserEmail()
            
            if (currentUserId != null && currentEmail != null) {
                authRepository.ensureProfileExistsPublic(currentUserId, currentEmail)
            }
        }
    }

    private fun observeAuthState() {
        authRepository.observeAuthState()
            .onEach { isLoggedIn ->
                if (isLoggedIn) {
                    // Only navigate to main if we are not in a specific flow that requires attention
                    // For example, if we are in Reset Password flow, we might be technically logged in via recovery token,
                    // but we shouldn't jump to Main immediately?
                    // Actually, recovery token logs you in.
                    // If we navigate to Main, the user can change password there.
                    // But usually we want to show Reset Password screen.
                    // We handle this conflict by checking the deep link type in handleDeepLink.

                    // However, for OAuth login, this observer is what triggers navigation to Main
                    // after the session is restored from the deep link.

                    // We check if we are already handling a deep link navigation manually.
                    // If not, we go to Main.

                    // Since handleDeepLink is called first, it might set state.
                    // We should be careful.

                    // Let's rely on explicit navigation for now.
                    // But if OAuth login happens, the AuthState changes.
                    // If we don't observe it, we won't know.

                    // Improved strategy:
                    // handleDeepLink will handle session restoration.
                    // If it detects OAuth login success, IT will emit NavigateToMain.
                    // So we don't need this observer strictly for navigation, preventing conflicts.
                    // But it's good to keep UI in sync.
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Set up debounced input validation for email, password, and username fields
     */
    @OptIn(FlowPreview::class)
    private fun setupInputValidation() {
        // Debounced email validation
        emailInputFlow
            .debounce(EMAIL_DEBOUNCE_MS)
            .onEach { email ->
                when (val state = _uiState.value) {
                    is AuthUiState.SignIn -> {
                        val isValid = validateEmail(email)
                        _uiState.value = state.copy(
                            email = email,
                            isEmailValid = isValid,
                            emailError = if (email.isNotEmpty() && !isValid) "Invalid email format" else null
                        )
                    }
                    is AuthUiState.SignUp -> {
                        val isValid = validateEmail(email)
                        _uiState.value = state.copy(
                            email = email,
                            isEmailValid = isValid,
                            emailError = if (email.isNotEmpty() && !isValid) "Invalid email format" else null
                        )
                    }
                    is AuthUiState.ForgotPassword -> {
                        val isValid = validateEmail(email)
                        _uiState.value = state.copy(
                            email = email,
                            isEmailValid = isValid,
                            emailError = if (email.isNotEmpty() && !isValid) "Invalid email format" else null
                        )
                    }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)

        // Password strength calculation for sign-up
        passwordInputFlow
            .debounce(EMAIL_DEBOUNCE_MS)
            .onEach { password ->
                when (val state = _uiState.value) {
                    is AuthUiState.SignUp -> {
                        val strength = calculatePasswordStrength(password)
                        _uiState.value = state.copy(
                            password = password,
                            passwordStrength = strength
                        )
                    }
                    is AuthUiState.ResetPassword -> {
                        val strength = calculatePasswordStrength(password)
                        _uiState.value = state.copy(
                            password = password,
                            passwordStrength = strength
                        )
                    }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)

        // Username validation and availability check
        usernameInputFlow
            .debounce(EMAIL_DEBOUNCE_MS)
            .onEach { username ->
                when (val state = _uiState.value) {
                    is AuthUiState.SignUp -> {
                        val validationResult = UsernameValidator.validate(username)
                        if (username.isNotEmpty() && validationResult is UsernameValidator.ValidationResult.Valid) {
                            // Check availability
                            checkUsernameAvailability(username)
                        } else {
                            val errorMessage = if (validationResult is UsernameValidator.ValidationResult.Error) validationResult.message else null
                            _uiState.value = state.copy(
                                username = username,
                                usernameError = if (username.isNotEmpty()) errorMessage else null
                            )
                        }
                    }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun checkUsernameAvailability(username: String) {
        val state = _uiState.value as? AuthUiState.SignUp ?: return

        _uiState.value = state.copy(isCheckingUsername = true)

        val result = usernameRepository.checkAvailability(username)
        result.fold(
            onSuccess = { isAvailable ->
                val currentState = _uiState.value as? AuthUiState.SignUp ?: return@fold
                _uiState.value = currentState.copy(
                    isCheckingUsername = false,
                    usernameError = if (!isAvailable) "Username is already taken" else null
                )
            },
            onFailure = {
                val currentState = _uiState.value as? AuthUiState.SignUp ?: return@fold
                _uiState.value = currentState.copy(isCheckingUsername = false)
            }
        )
    }

    // ========== User Actions ==========

    /**
     * Handle sign-in button click
     */
    fun onSignInClick(email: String, password: String) {
        viewModelScope.launch {
            if (!validateSignInForm(email, password)) {
                return@launch
            }

            _uiState.value = AuthUiState.Loading

            val result = authRepository.signIn(email, password)
            result.fold(
                onSuccess = {
                    if (authRepository.isEmailVerified()) {
                        _uiState.value = AuthUiState.Success("Sign in successful")
                        delay(500) // Show success state briefly
                        _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                    } else {
                        sharedPreferences.edit()
                            .putString(PREF_KEY_VERIFICATION_EMAIL, email)
                            .apply()
                        _uiState.value = AuthUiState.EmailVerification(email = email)
                        _navigationEvent.emit(AuthNavigationEvent.NavigateToEmailVerification)

                        // Start polling for verification
                        launch {
                            checkEmailVerification(email)
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.SignIn(
                        email = email,
                        password = password,
                        generalError = error.message ?: "Sign in failed"
                    )
                }
            )
        }
    }

    /**
     * Handle sign-up - robust client-side profile creation
     */
    fun onSignUpClick(email: String, password: String, username: String) {
        viewModelScope.launch {
            if (!validateSignUpForm(email, password, username)) {
                return@launch
            }

            // Check username availability
            val availabilityResult = usernameRepository.checkAvailability(username)
            if (availabilityResult.isSuccess && availabilityResult.getOrNull() == false) {
                 _uiState.value = AuthUiState.SignUp(
                    email = email,
                    password = password,
                    username = username,
                    usernameError = "Username is already taken"
                )
                return@launch
            }

            _uiState.value = AuthUiState.Loading

            val result = authRepository.signUpWithProfile(email, password, username)
            result.fold(
                onSuccess = { userId ->
                    sharedPreferences.edit()
                        .putString("user_id", userId)
                        .putString("username", username)
                        .putString("email", email)
                        .apply()

                    val currentUser = com.synapse.social.studioasinc.core.network.SupabaseClient.client.auth.currentUserOrNull()
                    if (currentUser?.emailConfirmedAt == null) {
                        sharedPreferences.edit()
                            .putString(PREF_KEY_VERIFICATION_EMAIL, email)
                            .apply()
                        _uiState.value = AuthUiState.EmailVerification(email = email)
                        _navigationEvent.emit(AuthNavigationEvent.NavigateToEmailVerification)

                        launch { checkEmailVerification(email) }
                    } else {
                        _uiState.value = AuthUiState.Success("Account created successfully")
                        delay(500)
                        _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                    }
                },
                onFailure = { error ->
                    val errorMessage = when {
                        error.message?.contains("over_email_send_rate_limit") == true -> 
                            "Too many email requests. Please wait and check your inbox."
                        error.message?.contains("email") == true && error.message?.contains("already") == true ->
                            "Email already registered. Try signing in instead."
                        else -> error.message ?: "Sign up failed"
                    }
                    
                    _uiState.value = AuthUiState.SignUp(
                        email = email,
                        password = password,
                        username = username,
                        generalError = errorMessage
                    )
                }
            )
        }
    }

    /**
     * Handle forgot password button click
     */
    fun onForgotPasswordClick() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.ForgotPassword()
            _navigationEvent.emit(AuthNavigationEvent.NavigateToForgotPassword)
        }
    }

    private var recoveryToken: String? = null

    /**
     * Handle deep link intent
     */
    fun handleDeepLink(uri: android.net.Uri?) {
        if (uri == null) return

        val fragment = uri.fragment
        val query = uri.query

        if (fragment != null && fragment.contains("type=recovery")) {
             // Handle Password Recovery
             val params = fragment.split("&").associate {
                 val parts = it.split("=")
                 if (parts.size == 2) parts[0] to parts[1] else "" to ""
             }

             val token = params["access_token"]
             if (!token.isNullOrEmpty()) {
                 recoveryToken = token
                 viewModelScope.launch {
                     authRepository.recoverSession(token)
                     _uiState.value = AuthUiState.ResetPassword()
                     _navigationEvent.emit(AuthNavigationEvent.NavigateToResetPassword(token))
                 }
             }
        } else {
            // Assume OAuth login or Magic Link
            viewModelScope.launch {
                val result = authRepository.handleOAuthCallback(uri)
                result.fold(
                    onSuccess = {
                        _uiState.value = AuthUiState.Success("Authenticated successfully")
                        delay(500)
                        _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                    },
                    onFailure = { error ->
                        // Only show error if we were expecting an OAuth callback (e.g. user was in Loading state)
                        // Or if the URI definitely looks like an Auth callback (has error param)
                        if (uri.toString().contains("error")) {
                            _uiState.value = AuthUiState.SignIn(
                                generalError = "Authentication failed: ${error.message}"
                            )
                        }
                        // Otherwise it might be just opening the app normally, do nothing
                    }
                )
            }
        }
    }

    /**
     * Handle reset password button click
     */
    fun onResetPasswordClick(password: String, confirmPassword: String, token: String) {
        val actualToken = if (token.isNotEmpty()) token else recoveryToken

        viewModelScope.launch {
            if (!validateResetPasswordForm(password, confirmPassword)) {
                return@launch
            }

            if (actualToken.isNullOrEmpty()) {
                val currentState = _uiState.value
                if (currentState is AuthUiState.ResetPassword) {
                    _uiState.value = currentState.copy(passwordError = "Missing recovery token")
                }
                return@launch
            }

            _uiState.value = AuthUiState.Loading

            val result = authRepository.updateUserPassword(password)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState.Success("Password reset successful")
                    delay(500)
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToSignIn)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.ResetPassword(
                        password = password,
                        confirmPassword = confirmPassword,
                        passwordError = error.message ?: "Failed to reset password"
                    )
                }
            )
        }
    }

    /**
     * Send password reset email
     */
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            if (!validateEmail(email)) {
                val currentState = _uiState.value
                if (currentState is AuthUiState.ForgotPassword) {
                    _uiState.value = currentState.copy(emailError = "Invalid email format")
                }
                return@launch
            }

            _uiState.value = AuthUiState.Loading

            val result = authRepository.sendPasswordResetEmail(email)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState.ForgotPassword(
                        email = email,
                        emailSent = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.ForgotPassword(
                        email = email,
                        emailError = error.message ?: "Failed to send reset email"
                    )
                }
            )
        }
    }

    /**
     * Handle resend verification email button click
     */
    fun onResendVerificationClick() {
        viewModelScope.launch {
            val state = _uiState.value as? AuthUiState.EmailVerification ?: return@launch

            if (!state.canResend) {
                return@launch
            }

            // Start cooldown immediately to prevent spamming
            startResendCooldown()

            val result = authRepository.resendVerificationEmail(state.email)
            result.fold(
                onSuccess = {
                    val currentState = _uiState.value as? AuthUiState.EmailVerification
                    if (currentState != null) {
                        _uiState.value = currentState.copy(
                            isResent = true,
                            resendError = null
                        )
                    }
                    android.util.Log.d("AuthViewModel", "Verification email resent successfully to ${state.email}")
                },
                onFailure = { e ->
                    val currentState = _uiState.value as? AuthUiState.EmailVerification
                    if (currentState != null) {
                        _uiState.value = currentState.copy(
                            isResent = false,
                            resendError = e.message ?: "Failed to resend email"
                        )
                    }
                    android.util.Log.e("AuthViewModel", "Failed to resend verification email", e)
                }
            )
        }
    }

    /**
     * Handle back to sign-in button click
     */
    fun onBackToSignInClick() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.SignIn()
            _navigationEvent.emit(AuthNavigationEvent.NavigateToSignIn)
        }
    }

    /**
     * Handle toggle between sign-in and sign-up modes
     */
    fun onToggleModeClick() {
        viewModelScope.launch {
            when (_uiState.value) {
                is AuthUiState.SignIn -> {
                    _uiState.value = AuthUiState.SignUp()
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToSignUp)
                }
                is AuthUiState.SignUp -> {
                    _uiState.value = AuthUiState.SignIn()
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToSignIn)
                }
                else -> {}
            }
        }
    }

    /**
     * Handle OAuth provider button click
     */
    fun onOAuthClick(provider: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.getOAuthUrl(provider)
            result.fold(
                onSuccess = { url ->
                    _navigationEvent.emit(AuthNavigationEvent.OpenUrl(url))
                    // State remains Loading until app returns via deep link
                },
                onFailure = { error ->
                    val message = "Failed to initiate $provider sign-in: ${error.message}"
                    // Restore previous state with error
                    // We need to know if we came from SignIn or SignUp.
                    // For now, default to SignIn as that's the safest fallback
                     _uiState.value = AuthUiState.SignIn(
                        generalError = message
                    )
                }
            )
        }
    }

    // ========== Input Change Handlers ==========

    /**
     * Handle email input change
     */
    fun onEmailChanged(email: String) {
        emailInputFlow.value = email
        
        // Clear error immediately when user starts typing
        when (val state = _uiState.value) {
            is AuthUiState.SignIn -> {
                _uiState.value = state.copy(email = email, emailError = null, generalError = null)
            }
            is AuthUiState.SignUp -> {
                _uiState.value = state.copy(email = email, emailError = null, generalError = null)
            }
            is AuthUiState.ForgotPassword -> {
                _uiState.value = state.copy(email = email, emailError = null)
            }
            else -> {}
        }
    }

    /**
     * Handle password input change
     */
    fun onPasswordChanged(password: String) {
        passwordInputFlow.value = password
        
        // Clear error immediately when user starts typing
        when (val state = _uiState.value) {
            is AuthUiState.SignIn -> {
                _uiState.value = state.copy(password = password, passwordError = null, generalError = null)
            }
            is AuthUiState.SignUp -> {
                _uiState.value = state.copy(password = password, passwordError = null, generalError = null)
            }
            is AuthUiState.ResetPassword -> {
                _uiState.value = state.copy(password = password, passwordError = null)
            }
            else -> {}
        }
    }

    /**
     * Handle username input change
     */
    fun onUsernameChanged(username: String) {
        usernameInputFlow.value = username
        
        // Clear error immediately when user starts typing
        when (val state = _uiState.value) {
            is AuthUiState.SignUp -> {
                _uiState.value = state.copy(username = username, usernameError = null, generalError = null)
            }
            else -> {}
        }
    }

    /**
     * Handle confirm password input change
     */
    fun onConfirmPasswordChanged(confirmPassword: String) {
        when (val state = _uiState.value) {
            is AuthUiState.ResetPassword -> {
                _uiState.value = state.copy(
                    confirmPassword = confirmPassword,
                    confirmPasswordError = null
                )
            }
            else -> {}
        }
    }

    // ========== Validation Methods ==========

    /**
     * Validate email format
     * @param email Email address to validate
     * @return true if email is valid, false otherwise
     */
    fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && EMAIL_REGEX.matches(email)
    }

    /**
     * Validate password meets minimum requirements
     * @param password Password to validate
     * @return true if password is valid, false otherwise
     */
    fun validatePassword(password: String): Boolean {
        return password.length >= MIN_PASSWORD_LENGTH
    }

    /**
     * Validate username meets minimum requirements
     * @param username Username to validate
     * @return true if username is valid, false otherwise
     */
    fun validateUsername(username: String): Boolean {
        return UsernameValidator.validate(username) is UsernameValidator.ValidationResult.Valid
    }

    /**
     * Calculate password strength based on length and complexity
     * @param password Password to evaluate
     * @return PasswordStrength level (Weak, Fair, or Strong)
     */
    fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return PasswordStrength.Weak
        }

        var score = 0

        // Length score
        when {
            password.length >= 12 -> score += 2
            password.length >= 10 -> score += 1
        }

        // Complexity score
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score >= 5 -> PasswordStrength.Strong
            score >= 3 -> PasswordStrength.Fair
            else -> PasswordStrength.Weak
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Validate sign-in form
     */
    private fun validateSignInForm(email: String, password: String): Boolean {
        val emailValid = validateEmail(email)
        val passwordValid = validatePassword(password)

        if (!emailValid || !passwordValid) {
            _uiState.value = AuthUiState.SignIn(
                email = email,
                password = password,
                emailError = if (!emailValid) "Invalid email format" else null,
                passwordError = if (!passwordValid) "Password must be at least $MIN_PASSWORD_LENGTH characters" else null
            )
            return false
        }

        return true
    }

    /**
     * Validate sign-up form
     */
    private fun validateSignUpForm(email: String, password: String, username: String): Boolean {
        val emailValid = validateEmail(email)
        val passwordValid = validatePassword(password)
        val usernameValid = validateUsername(username)

        if (!emailValid || !passwordValid || !usernameValid) {
            _uiState.value = AuthUiState.SignUp(
                email = email,
                password = password,
                username = username,
                emailError = if (!emailValid) "Invalid email format" else null,
                passwordError = if (!passwordValid) "Password must be at least $MIN_PASSWORD_LENGTH characters" else null,
                usernameError = if (!usernameValid) "Username must be at least $MIN_USERNAME_LENGTH characters" else null
            )
            return false
        }

        return true
    }

    /**
     * Validate reset password form
     */
    private fun validateResetPasswordForm(password: String, confirmPassword: String): Boolean {
        val passwordValid = validatePassword(password)
        val passwordsMatch = password == confirmPassword

        if (!passwordValid || !passwordsMatch) {
            _uiState.value = AuthUiState.ResetPassword(
                password = password,
                confirmPassword = confirmPassword,
                passwordError = if (!passwordValid) "Password must be at least $MIN_PASSWORD_LENGTH characters" else null,
                confirmPasswordError = if (!passwordsMatch) "Passwords do not match" else null
            )
            return false
        }

        return true
    }

    /**
     * Start the resend verification cooldown timer
     */
    private fun startResendCooldown() {
        cooldownJob?.cancel()
        
        val state = _uiState.value as? AuthUiState.EmailVerification ?: return
        
        cooldownJob = viewModelScope.launch {
            for (seconds in RESEND_COOLDOWN_SECONDS downTo 1) {
                val current = _uiState.value as? AuthUiState.EmailVerification ?: break
                _uiState.value = current.copy(
                    canResend = false,
                    resendCooldownSeconds = seconds
                )
                delay(1000)
            }
            
            val finalState = _uiState.value as? AuthUiState.EmailVerification
            if (finalState != null) {
                _uiState.value = finalState.copy(
                    canResend = true,
                    resendCooldownSeconds = 0,
                    isResent = false, // Clear sent status after cooldown so button looks fresh if they need to click again
                    resendError = null
                )
            }
        }
    }

    /**
     * Check if email has been verified
     */
    private suspend fun checkEmailVerification(email: String) {
        val pollInterval = 3000L // 3 seconds

        while (_uiState.value is AuthUiState.EmailVerification) {
            delay(pollInterval)

            // We verify if we are still in the correct state before making network calls
            if (_uiState.value !is AuthUiState.EmailVerification) break

            val result = authRepository.refreshSession()
            if (result.isSuccess && authRepository.isEmailVerified()) {
                _uiState.value = AuthUiState.Success("Email verified successfully")
                delay(1000) // Let user see the success message
                _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                break
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
    }
}
