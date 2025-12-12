package com.synapse.social.studioasinc.ui.auth

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UsernameRepository
import com.synapse.social.studioasinc.ui.auth.models.AuthNavigationEvent
import com.synapse.social.studioasinc.ui.auth.models.AuthUiState
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import com.synapse.social.studioasinc.ui.auth.models.PasswordStrength
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

/**
 * ViewModel for managing authentication UI state and business logic.
 * Handles sign-in, sign-up, email verification, and password reset flows.
 * 
 * @param authRepository Repository for authentication operations
 * @param sharedPreferences SharedPreferences for storing user preferences
 */
class AuthViewModel(
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
                    _uiState.value = AuthUiState.Success("Sign in successful")
                    delay(500) // Show success state briefly
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
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
     * Handle sign-up button click
     */
    fun onSignUpClick(email: String, password: String, username: String) {
        viewModelScope.launch {
            if (!validateSignUpForm(email, password, username)) {
                return@launch
            }

            // Check availability one last time before submitting
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

            // We use signUp here, but real implementation should probably insert the username into users table
            // The PRD says "Create user profile with minimal data on signup".
            // Since AuthRepository.signUp only creates auth user, we need to handle profile creation.
            // However, AuthRepository might need to be updated or we do it here.
            // Given constraints, I will assume AuthRepository.signUp or a trigger handles it,
            // OR I should use Supabase client to insert it.
            // PRD says: "supabaseClient.client.from("users").insert(...)"
            // Since I don't have supabaseClient here directly (except via repositories),
            // I should probably move that logic to AuthRepository or a new use case.
            // For now, I'll rely on AuthRepository.signUp and assume it's sufficient or updated later.
            // WAIT, PRD explicitely says "Update AuthViewModel ... signUpWithUsername ... insert(...)".
            // But I don't have SupabaseClient injected.
            // I'll stick to basic flow for now and assume backend trigger or AuthRepository handles it if possible.
            // Actually, I can add a method to UsernameRepository to create profile? Or AuthRepository.
            // Let's just proceed with basic authRepository.signUp for now as I cannot easily change AuthRepository signature/deps without risk.

            // Correction: PRD requires inserting the user.
            // I will use SupabaseClient directly as it is a singleton in this project (com.synapse.social.studioasinc.SupabaseClient)
            // or better, delegate to AuthRepository if I can modify it.
            // Modifying AuthRepository is safer.

            val result = authRepository.signUp(email, password)
            result.fold(
                onSuccess = { userId ->
                    try {
                        val client = com.synapse.social.studioasinc.SupabaseClient.client
                        val userMap = mapOf(
                            "uid" to userId,
                            "username" to username,
                            "email" to email,
                            "created_at" to java.time.Instant.now().toString()
                        )

                        client.from("users").insert(userMap)

                        sharedPreferences.edit()
                            .putBoolean("show_profile_completion_dialog", true)
                            .apply()

                        val currentUser = com.synapse.social.studioasinc.SupabaseClient.client.auth.currentUserOrNull()
                        if (currentUser?.emailConfirmedAt == null) {
                             // If verification needed, go to verification
                             sharedPreferences.edit()
                                .putString(PREF_KEY_VERIFICATION_EMAIL, email)
                                .apply()
                             _uiState.value = AuthUiState.EmailVerification(email = email)
                             _navigationEvent.emit(AuthNavigationEvent.NavigateToEmailVerification)
                        } else {
                            _uiState.value = AuthUiState.Success("Sign up successful")
                            delay(500)
                            _navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                        }
                    } catch (e: Exception) {
                        // If profile creation fails, we still created the auth user.
                        // Might need to retry or ignore.
                         _uiState.value = AuthUiState.SignUp(
                            email = email,
                            password = password,
                            username = username,
                            generalError = "Failed to create profile: ${e.message}"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.SignUp(
                        email = email,
                        password = password,
                        username = username,
                        generalError = error.message ?: "Sign up failed"
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

        val fragment = uri.fragment ?: return
        if (fragment.contains("type=recovery")) {
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

            // TODO: Implement resend verification when AuthRepository supports it
            // For now, just start cooldown
            startResendCooldown()
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
        // TODO: Implement OAuth when supported
        viewModelScope.launch {
            val state = _uiState.value
            when (state) {
                is AuthUiState.SignIn -> {
                    _uiState.value = state.copy(
                        generalError = "OAuth sign-in with $provider is not yet implemented"
                    )
                }
                is AuthUiState.SignUp -> {
                    _uiState.value = state.copy(
                        generalError = "OAuth sign-up with $provider is not yet implemented"
                    )
                }
                else -> {}
            }
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
                _uiState.value = state.copy(
                    canResend = false,
                    resendCooldownSeconds = seconds
                )
                delay(1000)
            }
            
            _uiState.value = state.copy(
                canResend = true,
                resendCooldownSeconds = 0
            )
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
