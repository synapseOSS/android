package com.synapse.social.studioasinc.data.remote.services

import android.content.Context
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import android.util.Log
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.io.IOException

/**
 * Authentication error handler for error classification and user-friendly messages
 */
class AuthErrorHandler {
    companion object {
        private const val TAG = "AuthErrorHandler"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
        
        fun handleAuthError(error: Throwable): AuthError {
            Log.d(TAG, "Handling auth error: ${error.message}")
            logAuthenticationError(error)
            
            return when (error) {
                is RestException -> {
                    val msg = error.message ?: ""
                    when {
                        msg.contains("email not confirmed", ignoreCase = true) -> AuthError.EMAIL_NOT_VERIFIED
                        msg.contains("invalid login credentials", ignoreCase = true) -> AuthError.INVALID_CREDENTIALS
                        msg.contains("invalid", ignoreCase = true) -> AuthError.INVALID_CREDENTIALS
                        else -> AuthError.UNKNOWN_ERROR
                    }
                }
                is HttpRequestException -> AuthError.NETWORK_ERROR
                is java.net.UnknownHostException -> AuthError.NETWORK_ERROR
                is java.net.SocketTimeoutException -> AuthError.NETWORK_ERROR
                is java.io.IOException -> AuthError.NETWORK_ERROR
                else -> {
                    val msg = error.message ?: ""
                    when {
                        msg.contains("network", ignoreCase = true) -> AuthError.NETWORK_ERROR
                        msg.contains("connection", ignoreCase = true) -> AuthError.NETWORK_ERROR
                        msg.contains("timeout", ignoreCase = true) -> AuthError.NETWORK_ERROR
                        msg.contains("unreachable", ignoreCase = true) -> AuthError.NETWORK_ERROR
                        else -> AuthError.UNKNOWN_ERROR
                    }
                }
            }
        }
        
        fun getErrorMessage(error: AuthError): String {
            return when (error) {
                AuthError.EMAIL_NOT_VERIFIED -> 
                    "Please verify your email address to continue"
                AuthError.INVALID_CREDENTIALS -> 
                    "Invalid email or password"
                AuthError.NETWORK_ERROR -> 
                    "Network connection error. Please check your internet connection and try again."
                AuthError.SUPABASE_NOT_CONFIGURED -> 
                    "Authentication service not configured"
                AuthError.UNKNOWN_ERROR -> 
                    "An unexpected error occurred"
            }
        }
        
        /**
         * Determines if an error is recoverable and should be retried
         */
        fun isRecoverableError(error: AuthError): Boolean {
            return when (error) {
                AuthError.NETWORK_ERROR -> true
                AuthError.UNKNOWN_ERROR -> true
                else -> false
            }
        }
        
        /**
         * Gets recovery action for specific error types
         */
        fun getRecoveryAction(error: AuthError): RecoveryAction {
            return when (error) {
                AuthError.EMAIL_NOT_VERIFIED -> RecoveryAction.RESEND_VERIFICATION
                AuthError.INVALID_CREDENTIALS -> RecoveryAction.RETRY_WITH_CORRECT_CREDENTIALS
                AuthError.NETWORK_ERROR -> RecoveryAction.RETRY_WITH_DELAY
                AuthError.SUPABASE_NOT_CONFIGURED -> RecoveryAction.CHECK_CONFIGURATION
                AuthError.UNKNOWN_ERROR -> RecoveryAction.RETRY_WITH_DELAY
            }
        }
        
        /**
         * Executes retry logic with exponential backoff for network-related failures
         */
        suspend fun <T> executeWithRetry(
            maxAttempts: Int = MAX_RETRY_ATTEMPTS,
            initialDelay: Long = RETRY_DELAY_MS,
            operation: suspend () -> T
        ): T {
            var currentDelay = initialDelay
            var lastException: Exception? = null
            
            repeat(maxAttempts) { attempt ->
                try {
                    Log.d(TAG, "Executing operation, attempt ${attempt + 1}/$maxAttempts")
                    return operation()
                } catch (e: Exception) {
                    lastException = e
                    val authError = handleAuthError(e)
                    
                    Log.w(TAG, "Operation failed on attempt ${attempt + 1}: ${e.message}")
                    
                    // Only retry for recoverable errors
                    if (!isRecoverableError(authError) || attempt == maxAttempts - 1) {
                        Log.e(TAG, "Non-recoverable error or max attempts reached", e)
                        throw e
                    }
                    
                    // Wait before retry with exponential backoff
                    Log.d(TAG, "Retrying in ${currentDelay}ms...")
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay *= 2 // Exponential backoff
                }
            }
            
            throw lastException ?: Exception("Operation failed after $maxAttempts attempts")
        }
        
        /**
         * Logs authentication errors and verification attempts for debugging
         */
        private fun logAuthenticationError(error: Throwable) {
            Log.e(TAG, "Authentication error occurred", error)
            
            // Log additional context for debugging
            when {
                error.message?.contains("email not confirmed", ignoreCase = true) == true -> {
                    Log.i(TAG, "Email verification required - user needs to check email")
                }
                error.message?.contains("invalid", ignoreCase = true) == true -> {
                    Log.i(TAG, "Invalid credentials provided - user should check email/password")
                }
                error is java.net.UnknownHostException -> {
                    Log.w(TAG, "Network connectivity issue - DNS resolution failed")
                }
                error is java.net.SocketTimeoutException -> {
                    Log.w(TAG, "Network timeout - slow connection or server issues")
                }
                error is java.io.IOException -> {
                    Log.w(TAG, "IO error during authentication - network or server issue")
                }
            }
        }
        
        /**
         * Logs verification attempts for monitoring
         */
        fun logVerificationAttempt(email: String, success: Boolean, errorMessage: String? = null) {
            if (success) {
                Log.i(TAG, "Email verification successful for: $email")
            } else {
                Log.w(TAG, "Email verification failed for: $email, error: $errorMessage")
            }
        }
        
        /**
         * Logs resend verification attempts
         */
        fun logResendVerificationAttempt(email: String, success: Boolean, errorMessage: String? = null) {
            if (success) {
                Log.i(TAG, "Resend verification email successful for: $email")
            } else {
                Log.w(TAG, "Resend verification email failed for: $email, error: $errorMessage")
            }
        }
    }
}

/**
 * Recovery actions for different error types
 */
enum class RecoveryAction {
    RESEND_VERIFICATION,
    RETRY_WITH_CORRECT_CREDENTIALS,
    RETRY_WITH_DELAY,
    CHECK_CONFIGURATION
}

/**
 * Supabase Authentication Service
 * Handles user authentication using Supabase Auth with configurable settings
 */
class SupabaseAuthenticationService : com.synapse.social.studioasinc.data.remote.services.interfaces.IAuthenticationService {
    
    private val context: Context
    private val client = SupabaseClient.client
    private var authConfig: AuthConfig
    
    /**
     * Primary constructor with context
     */
    constructor(context: Context) {
        this.context = context.applicationContext
        this.authConfig = AuthConfig.create(this.context)
    }
    
    /**
     * Secondary constructor for backward compatibility
     * Uses singleton instance if available, otherwise throws exception
     */
    constructor() {
        val instance = INSTANCE ?: throw IllegalStateException(
            "SupabaseAuthenticationService not initialized. Use constructor with context or call initialize(context) first."
        )
        this.context = instance.context
        this.authConfig = instance.authConfig
    }
    
    companion object {
        private const val TAG = "SupabaseAuth"
        
        @Volatile
        private var INSTANCE: SupabaseAuthenticationService? = null
        
        /**
         * Get singleton instance of SupabaseAuthenticationService
         * This method provides backward compatibility for existing code
         */
        @JvmStatic
        fun getInstance(context: Context): SupabaseAuthenticationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseAuthenticationService(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        /**
         * Create SupabaseAuthenticationService instance with default configuration
         * This is a convenience method for backward compatibility
         */
        @JvmStatic
        fun create(context: Context): SupabaseAuthenticationService {
            return SupabaseAuthenticationService(context)
        }
        
        /**
         * Create SupabaseAuthenticationService instance with development mode enabled
         */
        @JvmStatic
        fun createForDevelopment(context: Context): SupabaseAuthenticationService {
            val service = SupabaseAuthenticationService(context)
            service.enableDevelopmentMode()
            return service
        }
        
        /**
         * Create SupabaseAuthenticationService instance with production settings
         */
        @JvmStatic
        fun createForProduction(context: Context): SupabaseAuthenticationService {
            val service = SupabaseAuthenticationService(context)
            service.disableDevelopmentMode()
            return service
        }
        
        /**
         * Initialize the singleton instance - should be called from Application.onCreate()
         */
        @JvmStatic
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = SupabaseAuthenticationService(context.applicationContext)
                    }
                }
            }
        }
        
        /**
         * Get the singleton instance without context (for backward compatibility)
         * Throws exception if not initialized
         */
        @JvmStatic
        fun getInstance(): SupabaseAuthenticationService {
            return INSTANCE ?: throw IllegalStateException(
                "SupabaseAuthenticationService not initialized. Call initialize(context) first or use getInstance(context)."
            )
        }
    }
    
    /**
     * Update authentication configuration
     */
    fun updateConfig(newConfig: AuthConfig) {
        authConfig = newConfig
        AuthConfig.save(context, newConfig)
        debugLog("Authentication configuration updated: $newConfig")
    }
    
    /**
     * Get current authentication configuration
     */
    fun getConfig(): AuthConfig = authConfig
    
    /**
     * Enable development mode bypass for email verification
     */
    fun enableDevelopmentMode() {
        authConfig = AuthConfig.enableDevelopmentMode(context)
        debugLog("Development mode enabled - email verification bypassed")
    }
    
    /**
     * Disable development mode and restore production settings
     */
    fun disableDevelopmentMode() {
        authConfig = AuthConfig.disableDevelopmentMode(context)
        debugLog("Development mode disabled - email verification required")
    }
    
    /**
     * Debug logging helper that respects configuration
     */
    private fun debugLog(message: String, throwable: Throwable? = null) {
        if (authConfig.isDebugLoggingEnabled()) {
            if (throwable != null) {
                Log.d(TAG, message, throwable)
            } else {
                Log.d(TAG, message)
            }
        }
    }
    
    /**
     * Log authentication flow steps for debugging
     */
    private fun logAuthenticationStep(step: String, email: String? = null, success: Boolean? = null) {
        if (authConfig.isDebugLoggingEnabled()) {
            val emailPart = email?.let { " for email: $it" } ?: ""
            val successPart = success?.let { " - ${if (it) "SUCCESS" else "FAILED"}" } ?: ""
            debugLog("Auth Step: $step$emailPart$successPart")
        }
    }
    
    /**
     * Sign up a new user with email and password
     */
    override suspend fun signUp(email: String, password: String): Result<AuthResult> {
        return withContext(Dispatchers.IO) {
            try {
                logAuthenticationStep("Starting sign up", email)
                
                // Check if Supabase is configured
                if (!SupabaseClient.isConfigured()) {
                    logAuthenticationStep("Sign up failed - Supabase not configured", email, false)
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }
                
                // Clear any existing session first
                try {
                    client.auth.signOut()
                    debugLog("Cleared existing session before sign up")
                } catch (e: Exception) {
                    debugLog("No existing session to clear", e)
                }
                
                // Attempt sign up with retry logic for network errors
                logAuthenticationStep("Attempting Supabase sign up", email)
                val authResult = AuthErrorHandler.executeWithRetry(
                    maxAttempts = authConfig.getEffectiveRetryAttempts(),
                    initialDelay = authConfig.getEffectiveRetryDelay()
                ) {
                    client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }
                
                // Sign up was successful - Supabase creates user even if email verification is needed
                debugLog("Sign up request completed successfully")
                
                // Try to get the user, but don't fail if not immediately available
                val supabaseUser = client.auth.currentUserOrNull()
                
                // Create user object from sign up result or current user
                val user = if (supabaseUser != null && supabaseUser.id.isNotEmpty()) {
                    User(
                        id = supabaseUser.id,
                        email = supabaseUser.email ?: email,
                        emailConfirmed = supabaseUser.emailConfirmedAt != null,
                        createdAt = supabaseUser.createdAt?.toString()
                    )
                } else {
                    // Even if we can't get current user, the sign up was successful
                    // This happens when email verification is required
                    User(
                        id = "pending", // Temporary ID until verification
                        email = email,
                        emailConfirmed = false,
                        createdAt = System.currentTimeMillis().toString()
                    )
                }
                
                debugLog("User created successfully: ${user.id}")
                
                // Check if email verification should be bypassed in development mode
                val needsVerification = if (authConfig.shouldBypassEmailVerification()) {
                    debugLog("Email verification bypassed in development mode")
                    false
                } else {
                    // If we have a user and they're not confirmed, or if we don't have a user (pending verification)
                    supabaseUser?.emailConfirmedAt == null
                }
                
                // Log verification attempt
                AuthErrorHandler.logVerificationAttempt(email, !needsVerification)
                logAuthenticationStep("Sign up completed successfully", email, true)
                
                val message = if (needsVerification) {
                    "Please check your email and click the verification link to activate your account."
                } else if (authConfig.shouldBypassEmailVerification()) {
                    "Account created successfully. Email verification bypassed in development mode."
                } else {
                    "Account created successfully!"
                }
                
                Result.success(AuthResult(
                    user = user,
                    needsEmailVerification = needsVerification,
                    message = message
                ))
            } catch (e: Exception) {
                logAuthenticationStep("Sign up failed with exception", email, false)
                debugLog("Sign up failed", e)
                
                // Make sure to clear any partial session on error
                try {
                    client.auth.signOut()
                } catch (signOutError: Exception) {
                    debugLog("Failed to clear session after error", signOutError)
                }
                
                val authError = AuthErrorHandler.handleAuthError(e)
                val errorMessage = AuthErrorHandler.getErrorMessage(authError)
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    /**
     * Sign in with email and password
     */
    override suspend fun signIn(email: String, password: String): Result<AuthResult> {
        return withContext(Dispatchers.IO) {
            try {
                logAuthenticationStep("Starting sign in", email)
                
                // Check if Supabase is configured
                if (!SupabaseClient.isConfigured()) {
                    logAuthenticationStep("Sign in failed - Supabase not configured", email, false)
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }
                
                // Clear any existing session first
                try {
                    client.auth.signOut()
                    debugLog("Cleared existing session before sign in")
                } catch (e: Exception) {
                    debugLog("No existing session to clear", e)
                }
                
                // Attempt sign in with retry logic for network errors
                logAuthenticationStep("Attempting Supabase sign in", email)
                val authResult = AuthErrorHandler.executeWithRetry(
                    maxAttempts = authConfig.getEffectiveRetryAttempts(),
                    initialDelay = authConfig.getEffectiveRetryDelay()
                ) {
                    client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }
                
                // Verify the user is actually authenticated
                val supabaseUser = client.auth.currentUserOrNull()
                if (supabaseUser != null && supabaseUser.id.isNotEmpty()) {
                    val user = User(
                        id = supabaseUser.id,
                        email = supabaseUser.email ?: email,
                        emailConfirmed = supabaseUser.emailConfirmedAt != null,
                        createdAt = supabaseUser.createdAt?.toString()
                    )
                    
                    debugLog("User authenticated successfully: ${user.id}")
                    
                    // Check if email verification should be bypassed in development mode
                    val emailVerified = supabaseUser.emailConfirmedAt != null || authConfig.shouldBypassEmailVerification()
                    
                    if (!emailVerified && !authConfig.shouldBypassEmailVerification()) {
                        // Log verification attempt
                        AuthErrorHandler.logVerificationAttempt(email, false, "Email not verified")
                        logAuthenticationStep("Sign in requires email verification", email, false)
                        
                        // Email not verified - return result indicating verification needed
                        Result.success(AuthResult(
                            user = user,
                            needsEmailVerification = true,
                            message = "Please verify your email address to continue"
                        ))
                    } else {
                        // Log successful verification
                        AuthErrorHandler.logVerificationAttempt(email, true)
                        logAuthenticationStep("Sign in completed successfully", email, true)
                        
                        val message = if (authConfig.shouldBypassEmailVerification() && supabaseUser.emailConfirmedAt == null) {
                            "Signed in successfully. Email verification bypassed in development mode."
                        } else {
                            null
                        }
                        
                        // Email verified or bypassed - successful authentication
                        Result.success(AuthResult(
                            user = user,
                            needsEmailVerification = false,
                            message = message
                        ))
                    }
                } else {
                    logAuthenticationStep("Sign in failed - invalid credentials", email, false)
                    Result.failure(Exception("Authentication failed - invalid credentials"))
                }
            } catch (e: Exception) {
                logAuthenticationStep("Sign in failed with exception", email, false)
                debugLog("Sign in failed", e)
                
                // Make sure to clear any partial session on error
                try {
                    client.auth.signOut()
                } catch (signOutError: Exception) {
                    debugLog("Failed to clear session after error", signOutError)
                }
                
                val authError = AuthErrorHandler.handleAuthError(e)
                val errorMessage = AuthErrorHandler.getErrorMessage(authError)
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    /**
     * Sign out the current user
     */
    override suspend fun signOut(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.signOut()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Resend verification email to the specified email address
     */
    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                logAuthenticationStep("Starting resend verification email", email)
                
                // Check if email verification is bypassed in development mode
                if (authConfig.shouldBypassEmailVerification()) {
                    debugLog("Resend verification email bypassed in development mode")
                    logAuthenticationStep("Resend verification email bypassed (dev mode)", email, true)
                    return@withContext Result.success(Unit)
                }
                
                // Check if Supabase is configured
                if (!SupabaseClient.isConfigured()) {
                    logAuthenticationStep("Resend verification failed - Supabase not configured", email, false)
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }
                
                // Execute with retry logic for network errors
                AuthErrorHandler.executeWithRetry(
                    maxAttempts = authConfig.getEffectiveRetryAttempts(),
                    initialDelay = authConfig.getEffectiveRetryDelay()
                ) {
                    logAuthenticationStep("Calling Supabase resend", email)
                    client.auth.resendEmail(OtpType.Email.SIGNUP, email)
                    Unit
                }
                
                // Log resend attempt
                AuthErrorHandler.logResendVerificationAttempt(email, true)
                logAuthenticationStep("Resend verification email completed", email, true)
                
                Result.success(Unit)
            } catch (e: Exception) {
                logAuthenticationStep("Resend verification email failed", email, false)
                debugLog("Failed to resend verification email", e)
                
                // Log failed resend attempt
                AuthErrorHandler.logResendVerificationAttempt(email, false, e.message)
                
                val authError = AuthErrorHandler.handleAuthError(e)
                val errorMessage = AuthErrorHandler.getErrorMessage(authError)
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    /**
     * Check if email is verified for the given email address
     */
    override suspend fun checkEmailVerified(email: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logAuthenticationStep("Checking email verification status", email)
                
                // Check if email verification is bypassed in development mode
                if (authConfig.shouldBypassEmailVerification()) {
                    debugLog("Email verification check bypassed in development mode - returning true")
                    logAuthenticationStep("Email verification check bypassed (dev mode)", email, true)
                    return@withContext Result.success(true)
                }
                
                // Check if Supabase is configured
                if (!SupabaseClient.isConfigured()) {
                    logAuthenticationStep("Email verification check failed - Supabase not configured", email, false)
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }
                
                // Get current user and check verification status with retry logic
                val isVerified = AuthErrorHandler.executeWithRetry(
                    maxAttempts = authConfig.getEffectiveRetryAttempts(),
                    initialDelay = authConfig.getEffectiveRetryDelay()
                ) {
                    val user = client.auth.currentUserOrNull()
                    if (user != null && user.email == email) {
                        val verified = user.emailConfirmedAt != null
                        debugLog("Email verification status for $email: $verified")
                        verified
                    } else {
                        debugLog("No current user or email mismatch for verification check")
                        false
                    }
                }
                
                // Log verification check
                AuthErrorHandler.logVerificationAttempt(email, isVerified)
                logAuthenticationStep("Email verification check completed", email, isVerified)
                
                Result.success(isVerified)
            } catch (e: Exception) {
                logAuthenticationStep("Email verification check failed", email, false)
                debugLog("Failed to check email verification status", e)
                
                // Log failed verification check
                AuthErrorHandler.logVerificationAttempt(email, false, e.message)
                
                val authError = AuthErrorHandler.handleAuthError(e)
                val errorMessage = AuthErrorHandler.getErrorMessage(authError)
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    /**
     * Get current user
     */
    override fun getCurrentUser(): User? {
        // Check if Supabase is configured
        if (!SupabaseClient.isConfigured()) {
            return null
        }

        val user = client.auth.currentUserOrNull()
        return if (user != null && user.id.isNotEmpty()) {
            User(
                id = user.id,
                email = user.email ?: "",
                emailConfirmed = user.emailConfirmedAt != null,
                createdAt = user.createdAt?.toString()
            )
        } else {
            null
        }
    }
    
    /**
     * Get current user ID
     */
    override fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
    
    /**
     * Update user password
     */
    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.updateUser {
                    password = newPassword
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update user email
     */
    override suspend fun updateEmail(newEmail: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.updateUser {
                    email = newEmail
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

/**
 * User data class
 */
data class User(
    val id: String,
    val email: String,
    val emailConfirmed: Boolean = false,
    val createdAt: String? = null
)

/**
 * Enhanced authentication result with verification status
 */
data class AuthResult(
    val user: User?,
    val needsEmailVerification: Boolean = false,
    val message: String? = null
)

/**
 * Authentication error types for specific error classification
 */
enum class AuthError {
    EMAIL_NOT_VERIFIED,
    INVALID_CREDENTIALS,
    NETWORK_ERROR,
    SUPABASE_NOT_CONFIGURED,
    UNKNOWN_ERROR
}
