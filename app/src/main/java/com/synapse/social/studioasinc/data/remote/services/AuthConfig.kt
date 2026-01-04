package com.synapse.social.studioasinc.data.remote.services

import android.content.Context
import android.util.Log
import com.synapse.social.studioasinc.BuildConfig

/**
 * Authentication configuration data class for authentication settings
 */
data class AuthConfig(
    val requireEmailVerification: Boolean = true,
    val resendCooldownSeconds: Int = 60,
    val maxResendAttempts: Int = 5,
    val enableDebugLogging: Boolean = false,
    val developmentMode: Boolean = false,
    val autoRetryAttempts: Int = 3,
    val retryDelayMs: Long = 1000L
) {
    companion object {
        private const val TAG = "AuthConfig"
        private const val PREFS_NAME = "auth_config"
        private const val KEY_REQUIRE_EMAIL_VERIFICATION = "require_email_verification"
        private const val KEY_RESEND_COOLDOWN_SECONDS = "resend_cooldown_seconds"
        private const val KEY_MAX_RESEND_ATTEMPTS = "max_resend_attempts"
        private const val KEY_ENABLE_DEBUG_LOGGING = "enable_debug_logging"
        private const val KEY_DEVELOPMENT_MODE = "development_mode"
        private const val KEY_AUTO_RETRY_ATTEMPTS = "auto_retry_attempts"
        private const val KEY_RETRY_DELAY_MS = "retry_delay_ms"
        
        /**
         * Create AuthConfig from build variants, properties, and runtime settings
         */
        fun create(context: Context): AuthConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // Determine if we're in development mode based on build variant and BuildConfig
            val isDevelopmentMode = try {
                BuildConfig.AUTH_DEVELOPMENT_MODE
            } catch (e: Exception) {
                BuildConfig.DEBUG
            } || BuildConfig.APPLICATION_ID.contains(".debug") ||
                System.getProperty("auth.development.mode")?.toBoolean() == true ||
                prefs.getBoolean(KEY_DEVELOPMENT_MODE, BuildConfig.DEBUG)
            
            // Enable debug logging from BuildConfig or runtime settings
            val enableDebugLogging = try {
                BuildConfig.AUTH_ENABLE_DEBUG_LOGGING
            } catch (e: Exception) {
                BuildConfig.DEBUG
            } || System.getProperty("auth.debug.logging")?.toBoolean() == true ||
                prefs.getBoolean(KEY_ENABLE_DEBUG_LOGGING, BuildConfig.DEBUG)
            
            // Email verification configuration from BuildConfig or runtime settings
            val requireEmailVerification = try {
                // Use BuildConfig value if available
                val buildConfigValue = BuildConfig.AUTH_REQUIRE_EMAIL_VERIFICATION
                // Allow runtime override
                System.getProperty("auth.require.email.verification")?.toBoolean() 
                    ?: prefs.getBoolean(KEY_REQUIRE_EMAIL_VERIFICATION, buildConfigValue)
            } catch (e: Exception) {
                // Fallback logic if BuildConfig fields are not available
                if (isDevelopmentMode) {
                    System.getProperty("auth.require.email.verification")?.toBoolean() 
                        ?: prefs.getBoolean(KEY_REQUIRE_EMAIL_VERIFICATION, false)
                } else {
                    System.getProperty("auth.require.email.verification")?.toBoolean() 
                        ?: prefs.getBoolean(KEY_REQUIRE_EMAIL_VERIFICATION, true)
                }
            }
            
            // Get other configuration values from BuildConfig, system properties, or preferences
            val resendCooldownSeconds = try {
                System.getProperty("auth.resend.cooldown.seconds")?.toIntOrNull()
                    ?: prefs.getInt(KEY_RESEND_COOLDOWN_SECONDS, BuildConfig.AUTH_RESEND_COOLDOWN_SECONDS)
            } catch (e: Exception) {
                System.getProperty("auth.resend.cooldown.seconds")?.toIntOrNull()
                    ?: prefs.getInt(KEY_RESEND_COOLDOWN_SECONDS, 60)
            }
            
            val maxResendAttempts = try {
                System.getProperty("auth.max.resend.attempts")?.toIntOrNull()
                    ?: prefs.getInt(KEY_MAX_RESEND_ATTEMPTS, BuildConfig.AUTH_MAX_RESEND_ATTEMPTS)
            } catch (e: Exception) {
                System.getProperty("auth.max.resend.attempts")?.toIntOrNull()
                    ?: prefs.getInt(KEY_MAX_RESEND_ATTEMPTS, 5)
            }
            
            val autoRetryAttempts = try {
                System.getProperty("auth.auto.retry.attempts")?.toIntOrNull()
                    ?: prefs.getInt(KEY_AUTO_RETRY_ATTEMPTS, BuildConfig.AUTH_AUTO_RETRY_ATTEMPTS)
            } catch (e: Exception) {
                System.getProperty("auth.auto.retry.attempts")?.toIntOrNull()
                    ?: prefs.getInt(KEY_AUTO_RETRY_ATTEMPTS, 3)
            }
            
            val retryDelayMs = try {
                System.getProperty("auth.retry.delay.ms")?.toLongOrNull()
                    ?: prefs.getLong(KEY_RETRY_DELAY_MS, BuildConfig.AUTH_RETRY_DELAY_MS)
            } catch (e: Exception) {
                System.getProperty("auth.retry.delay.ms")?.toLongOrNull()
                    ?: prefs.getLong(KEY_RETRY_DELAY_MS, 1000L)
            }
            
            val config = AuthConfig(
                requireEmailVerification = requireEmailVerification,
                resendCooldownSeconds = resendCooldownSeconds,
                maxResendAttempts = maxResendAttempts,
                enableDebugLogging = enableDebugLogging,
                developmentMode = isDevelopmentMode,
                autoRetryAttempts = autoRetryAttempts,
                retryDelayMs = retryDelayMs
            )
            
            if (enableDebugLogging) {
                Log.d(TAG, "AuthConfig created: $config")
                Log.d(TAG, "Build variant: ${if (BuildConfig.DEBUG) "debug" else "release"}")
                Log.d(TAG, "Application ID: ${BuildConfig.APPLICATION_ID}")
            }
            
            return config
        }
        
        /**
         * Save configuration to preferences for runtime changes
         */
        fun save(context: Context, config: AuthConfig) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean(KEY_REQUIRE_EMAIL_VERIFICATION, config.requireEmailVerification)
                putInt(KEY_RESEND_COOLDOWN_SECONDS, config.resendCooldownSeconds)
                putInt(KEY_MAX_RESEND_ATTEMPTS, config.maxResendAttempts)
                putBoolean(KEY_ENABLE_DEBUG_LOGGING, config.enableDebugLogging)
                putBoolean(KEY_DEVELOPMENT_MODE, config.developmentMode)
                putInt(KEY_AUTO_RETRY_ATTEMPTS, config.autoRetryAttempts)
                putLong(KEY_RETRY_DELAY_MS, config.retryDelayMs)
                apply()
            }
            
            if (config.enableDebugLogging) {
                Log.d(TAG, "AuthConfig saved: $config")
            }
        }
        
        /**
         * Reset configuration to defaults
         */
        fun reset(context: Context): AuthConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            
            val config = create(context)
            Log.d(TAG, "AuthConfig reset to defaults: $config")
            return config
        }
        
        /**
         * Enable development mode bypass for email verification
         */
        fun enableDevelopmentMode(context: Context): AuthConfig {
            val currentConfig = create(context)
            val devConfig = currentConfig.copy(
                developmentMode = true,
                requireEmailVerification = false,
                enableDebugLogging = true
            )
            save(context, devConfig)
            
            Log.d(TAG, "Development mode enabled: $devConfig")
            return devConfig
        }
        
        /**
         * Disable development mode and restore production settings
         */
        fun disableDevelopmentMode(context: Context): AuthConfig {
            val currentConfig = create(context)
            val prodConfig = currentConfig.copy(
                developmentMode = false,
                requireEmailVerification = true,
                enableDebugLogging = BuildConfig.DEBUG
            )
            save(context, prodConfig)
            
            Log.d(TAG, "Development mode disabled: $prodConfig")
            return prodConfig
        }
    }
    
    /**
     * Check if email verification should be bypassed
     */
    fun shouldBypassEmailVerification(): Boolean {
        return developmentMode && !requireEmailVerification
    }
    
    /**
     * Check if debug logging is enabled
     */
    fun isDebugLoggingEnabled(): Boolean {
        return enableDebugLogging || BuildConfig.DEBUG
    }
    
    /**
     * Get effective retry attempts (minimum 1, maximum 10)
     */
    fun getEffectiveRetryAttempts(): Int {
        return autoRetryAttempts.coerceIn(1, 10)
    }
    
    /**
     * Get effective retry delay (minimum 500ms, maximum 10000ms)
     */
    fun getEffectiveRetryDelay(): Long {
        return retryDelayMs.coerceIn(500L, 10000L)
    }
    
    /**
     * Get effective resend cooldown (minimum 30 seconds, maximum 300 seconds)
     */
    fun getEffectiveResendCooldown(): Int {
        return resendCooldownSeconds.coerceIn(30, 300)
    }
    
    /**
     * Get effective max resend attempts (minimum 3, maximum 10)
     */
    fun getEffectiveMaxResendAttempts(): Int {
        return maxResendAttempts.coerceIn(3, 10)
    }
}
