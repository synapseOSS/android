package com.synapse.social.studioasinc.core.util

import android.content.Context
import androidx.biometric.BiometricManager

/**
 * Interface for checking biometric capabilities.
 * Allows for easier testing by mocking this interface.
 */
interface BiometricChecker {
    fun canAuthenticate(context: Context): Int
}

/**
 * Default implementation of BiometricChecker using Android's BiometricManager.
 */
class BiometricCheckerImpl : BiometricChecker {
    override fun canAuthenticate(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    }
}
