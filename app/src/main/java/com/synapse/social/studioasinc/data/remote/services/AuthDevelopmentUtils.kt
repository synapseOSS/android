package com.synapse.social.studioasinc.data.remote.services

import android.content.Context
import android.util.Log
import com.synapse.social.studioasinc.BuildConfig

/**
 * Development utilities for authentication configuration and testing
 */
object AuthDevelopmentUtils {
    
    private const val TAG = "AuthDevUtils"
    
    /**
     * Check if the app is running in development mode
     */
    fun isDevelopmentBuild(): Boolean {
        return BuildConfig.DEBUG || BuildConfig.APPLICATION_ID.contains(".debug")
    }
    
    /**
     * Log authentication configuration for debugging
     */
    fun logAuthConfig(context: Context) {
        if (!isDevelopmentBuild()) return
        
        val config = AuthConfig.create(context)
        Log.d(TAG, "=== Authentication Configuration ===")
        Log.d(TAG, "Development Mode: ${config.developmentMode}")
        Log.d(TAG, "Email Verification Required: ${config.requireEmailVerification}")
        Log.d(TAG, "Email Verification Bypass: ${config.shouldBypassEmailVerification()}")
        Log.d(TAG, "Debug Logging: ${config.enableDebugLogging}")
        Log.d(TAG, "Resend Cooldown: ${config.resendCooldownSeconds}s")
        Log.d(TAG, "Max Resend Attempts: ${config.maxResendAttempts}")
        Log.d(TAG, "Auto Retry Attempts: ${config.autoRetryAttempts}")
        Log.d(TAG, "Retry Delay: ${config.retryDelayMs}ms")
        Log.d(TAG, "Build Variant: ${if (BuildConfig.DEBUG) "debug" else "release"}")
        Log.d(TAG, "Application ID: ${BuildConfig.APPLICATION_ID}")
        Log.d(TAG, "===================================")
    }
    
    /**
     * Create a test authentication service with development configuration
     */
    fun createTestAuthService(context: Context): SupabaseAuthenticationService {
        val service = SupabaseAuthenticationService(context)
        
        if (isDevelopmentBuild()) {
            // Enable development mode for testing
            service.enableDevelopmentMode()
            Log.d(TAG, "Test authentication service created with development mode enabled")
        }
        
        return service
    }
    
    /**
     * Quick setup for development mode
     */
    fun setupDevelopmentMode(context: Context): AuthConfig {
        if (!isDevelopmentBuild()) {
            Log.w(TAG, "Development mode setup called in non-development build")
            return AuthConfig.create(context)
        }
        
        val config = AuthConfig.enableDevelopmentMode(context)
        Log.i(TAG, "Development mode setup completed")
        logAuthConfig(context)
        
        return config
    }
    
    /**
     * Quick setup for production mode
     */
    fun setupProductionMode(context: Context): AuthConfig {
        val config = AuthConfig.disableDevelopmentMode(context)
        Log.i(TAG, "Production mode setup completed")
        
        if (isDevelopmentBuild()) {
            logAuthConfig(context)
        }
        
        return config
    }
    
    /**
     * Test authentication configuration with different settings
     */
    fun testAuthConfiguration(context: Context) {
        if (!isDevelopmentBuild()) {
            Log.w(TAG, "Auth configuration test called in non-development build")
            return
        }
        
        Log.d(TAG, "=== Testing Authentication Configuration ===")
        
        // Test default configuration
        val defaultConfig = AuthConfig.create(context)
        Log.d(TAG, "Default config: $defaultConfig")
        
        // Test development configuration
        val devConfig = AuthConfig.enableDevelopmentMode(context)
        Log.d(TAG, "Development config: $devConfig")
        
        // Test production configuration
        val prodConfig = AuthConfig.disableDevelopmentMode(context)
        Log.d(TAG, "Production config: $prodConfig")
        
        // Restore default
        AuthConfig.save(context, defaultConfig)
        Log.d(TAG, "Configuration restored to default")
        
        Log.d(TAG, "=== Authentication Configuration Test Complete ===")
    }
    
    /**
     * Get development configuration recommendations
     */
    fun getDevelopmentRecommendations(context: Context): List<String> {
        val config = AuthConfig.create(context)
        val recommendations = mutableListOf<String>()
        
        if (!isDevelopmentBuild()) {
            recommendations.add("Switch to debug build variant for development features")
            return recommendations
        }
        
        if (!config.developmentMode) {
            recommendations.add("Enable development mode to bypass email verification")
        }
        
        if (config.requireEmailVerification && config.developmentMode) {
            recommendations.add("Disable email verification requirement for faster testing")
        }
        
        if (!config.enableDebugLogging) {
            recommendations.add("Enable debug logging to see authentication flow details")
        }
        
        if (config.resendCooldownSeconds > 30) {
            recommendations.add("Reduce resend cooldown to 30 seconds for faster testing")
        }
        
        if (config.autoRetryAttempts < 3) {
            recommendations.add("Increase auto retry attempts to handle network issues")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Configuration is optimized for development")
        }
        
        return recommendations
    }
    
    /**
     * Apply recommended development settings
     */
    fun applyDevelopmentRecommendations(context: Context): AuthConfig {
        if (!isDevelopmentBuild()) {
            Log.w(TAG, "Development recommendations cannot be applied in non-development build")
            return AuthConfig.create(context)
        }
        
        val optimizedConfig = AuthConfig(
            requireEmailVerification = false,
            resendCooldownSeconds = 30,
            maxResendAttempts = 10,
            enableDebugLogging = true,
            developmentMode = true,
            autoRetryAttempts = 5,
            retryDelayMs = 500L
        )
        
        AuthConfig.save(context, optimizedConfig)
        Log.i(TAG, "Development recommendations applied")
        logAuthConfig(context)
        
        return optimizedConfig
    }
    
    /**
     * Validate authentication configuration
     */
    fun validateConfiguration(context: Context): List<String> {
        val config = AuthConfig.create(context)
        val issues = mutableListOf<String>()
        
        // Check for configuration conflicts
        if (config.developmentMode && config.requireEmailVerification) {
            issues.add("Development mode enabled but email verification still required")
        }
        
        if (!config.developmentMode && !config.requireEmailVerification) {
            issues.add("Production mode but email verification disabled - security risk")
        }
        
        if (config.resendCooldownSeconds < 30) {
            issues.add("Resend cooldown too short - may cause rate limiting")
        }
        
        if (config.maxResendAttempts > 10) {
            issues.add("Max resend attempts too high - may cause abuse")
        }
        
        if (config.autoRetryAttempts > 10) {
            issues.add("Auto retry attempts too high - may cause delays")
        }
        
        if (config.retryDelayMs < 500) {
            issues.add("Retry delay too short - may overwhelm server")
        }
        
        return issues
    }
}
