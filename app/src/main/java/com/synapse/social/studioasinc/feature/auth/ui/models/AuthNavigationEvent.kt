package com.synapse.social.studioasinc.feature.auth.ui.models

/**
 * Sealed class representing navigation events in the authentication flow.
 * These events are emitted by the ViewModel and consumed by the UI to trigger navigation.
 */
sealed class AuthNavigationEvent {
    /**
     * Navigate to the main application screen after successful authentication
     */
    object NavigateToMain : AuthNavigationEvent()

    /**
     * Navigate to the sign-in screen
     */
    object NavigateToSignIn : AuthNavigationEvent()

    /**
     * Navigate to the sign-up screen
     */
    object NavigateToSignUp : AuthNavigationEvent()

    /**
     * Navigate to the email verification screen
     */
    object NavigateToEmailVerification : AuthNavigationEvent()

    /**
     * Navigate to the forgot password screen
     */
    object NavigateToForgotPassword : AuthNavigationEvent()

    /**
     * Navigate to the reset password screen
     * @param token The password reset token from the email link
     */
    data class NavigateToResetPassword(val token: String) : AuthNavigationEvent()

    /**
     * Navigate back to the previous screen
     */
    object NavigateBack : AuthNavigationEvent()

    /**
     * Open a URL in an external browser or custom tab
     */
    data class OpenUrl(val url: String) : AuthNavigationEvent()
}
