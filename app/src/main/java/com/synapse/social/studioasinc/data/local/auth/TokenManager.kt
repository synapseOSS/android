package com.synapse.social.studioasinc.data.local.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class StoredTokens(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val userEmail: String,
    val expiryTime: Long
)

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth_tokens", Context.MODE_PRIVATE)
    
    suspend fun storeTokens(
        accessToken: String,
        refreshToken: String,
        userId: String,
        userEmail: String,
        expiresIn: Int
    ) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000L)
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putString("user_id", userId)
            .putString("user_email", userEmail)
            .putLong("expiry_time", expiryTime)
            .apply()
    }
    
    suspend fun getStoredTokens(): StoredTokens? {
        val accessToken = prefs.getString("access_token", null) ?: return null
        val refreshToken = prefs.getString("refresh_token", null) ?: return null
        val userId = prefs.getString("user_id", null) ?: return null
        val userEmail = prefs.getString("user_email", null) ?: return null
        val expiryTime = prefs.getLong("expiry_time", 0)
        
        return StoredTokens(accessToken, refreshToken, userId, userEmail, expiryTime)
    }
    
    suspend fun areTokensValid(): Boolean {
        val expiryTime = prefs.getLong("expiry_time", 0)
        return System.currentTimeMillis() < expiryTime
    }
    
    suspend fun clearTokens() {
        prefs.edit().clear().apply()
    }
    
    suspend fun getAccessToken(): String? = prefs.getString("access_token", null)
    
    suspend fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
}
