package com.synapse.social.studioasinc.feature.auth.ui.models

/**
 * Sealed class representing all possible UI states in the authentication flow.
 * Each state contains the data needed to render the corresponding screen.
 */
sealed class AuthUiState {
    /**
     * Initial state before any authentication action
     */
    object Initial : AuthUiState()

    /**
     * Loading state during authentication operations
     */
    object Loading : AuthUiState()

    /**
     * Sign-in screen state
     * @param email Current email input value
     * @param password Current password input value
     * @param emailError Error message for email field, null if valid
     * @param passwordError Error message for password field, null if valid
     * @param generalError General error message for the form, null if no error
     * @param isEmailValid Whether the email format is valid
     */
    data class SignIn(
        val email: String = "",
        val password: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val generalError: String? = null,
        val isEmailValid: Boolean = false
    ) : AuthUiState()

    /**
     * Sign-up screen state
     * @param email Current email input value
     * @param password Current password input value
     * @param username Current username input value
     * @param emailError Error message for email field, null if valid
     * @param passwordError Error message for password field, null if valid
     * @param usernameError Error message for username field, null if valid
     * @param generalError General error message for the form, null if no error
     * @param isEmailValid Whether the email format is valid
     * @param passwordStrength Current password strength level
     */
    data class SignUp(
        val email: String = "",
        val password: String = "",
        val username: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val usernameError: String? = null,
        val generalError: String? = null,
        val isEmailValid: Boolean = false,
        val passwordStrength: PasswordStrength = PasswordStrength.Weak,
        val isCheckingUsername: Boolean = false
    ) : AuthUiState()

    /**
     * Email verification screen state
     * @param email The email address that needs verification
     * @param canResend Whether the resend button is enabled (not in cooldown)
     * @param resendCooldownSeconds Remaining seconds in cooldown period, 0 if not in cooldown
     */
    data class EmailVerification(
        val email: String,
        val canResend: Boolean = true,
        val resendCooldownSeconds: Int = 0,
        val isResent: Boolean = false,
        val resendError: String? = null
    ) : AuthUiState()

    /**
     * Forgot password screen state
     * @param email Current email input value
     * @param emailError Error message for email field, null if valid
     * @param isEmailValid Whether the email format is valid
     * @param emailSent Whether the reset email has been sent successfully
     */
    data class ForgotPassword(
        val email: String = "",
        val emailError: String? = null,
        val isEmailValid: Boolean = false,
        val emailSent: Boolean = false
    ) : AuthUiState()

    /**
     * Reset password screen state
     * @param password Current password input value
     * @param confirmPassword Current confirm password input value
     * @param passwordError Error message for password field, null if valid
     * @param confirmPasswordError Error message for confirm password field, null if valid
     * @param passwordStrength Current password strength level
     */
    data class ResetPassword(
        val password: String = "",
        val confirmPassword: String = "",
        val passwordError: String? = null,
        val confirmPasswordError: String? = null,
        val passwordStrength: PasswordStrength = PasswordStrength.Weak
    ) : AuthUiState()

    /**
     * Success state after successful authentication operation
     * @param message Success message to display
     */
    data class Success(val message: String) : AuthUiState()

    /**
     * Error state for critical errors
     * @param message Error message to display
     */
    data class Error(val message: String) : AuthUiState()
}
