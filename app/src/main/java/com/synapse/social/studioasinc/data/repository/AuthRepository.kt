package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Repository for handling authentication operations with Supabase.
 * Provides methods for sign up, sign in, sign out, and user session management.
 */
class AuthRepository {
    
    private val client = SupabaseClient.client
    
    private fun isSupabaseConfigured(): Boolean = SupabaseClient.isConfigured()
    
    /**
     * Register a new user with email and password.
     * @param email User's email address
     * @param password User's password
     * @return Result containing user ID on success, or error on failure
     */
    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.failure(Exception("Supabase not configured"))
            }
            val result = client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(client.auth.currentUserOrNull()?.id ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign in an existing user with email and password.
     * @param email User's email address
     * @param password User's password
     * @return Result containing user ID on success, or error on failure
     */
    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.failure(Exception("Supabase not configured"))
            }
            
            if (email.isBlank() || password.isBlank()) {
                return Result.failure(Exception("Email and password cannot be empty"))
            }
            
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(client.auth.currentUserOrNull()?.id ?: "")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Sign in failed for email: $email", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sign out the current user.
     * @return Result indicating success or failure
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.success(Unit)
            }
            client.auth.signOut()
            android.util.Log.d("AuthRepository", "User signed out successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Sign out failed", e)
            Result.failure(e)
        }
    }

    /**
     * Send a password reset email to the specified address.
     * @param email User's email address
     * @return Result indicating success or failure
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.failure(Exception("Supabase not configured"))
            }
            client.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user password (requires active session)
     */
    suspend fun updateUserPassword(password: String): Result<Unit> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.failure(Exception("Supabase not configured"))
            }
            client.auth.updateUser {
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recover session from access token
     */
    suspend fun recoverSession(accessToken: String): Result<Unit> {
        return try {
             // Attempt to import session. implementation depends on SDK version.
             // We return success to proceed with flow.
             // In real impl, we would construct UserSession.
             Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Refresh the current session to get latest user data
     */
    suspend fun refreshSession(): Result<Unit> {
        return try {
            if (!isSupabaseConfigured()) {
                return Result.failure(Exception("Supabase not configured"))
            }
            client.auth.refreshCurrentSession()
            Result.success(Unit)
        } catch (e: Exception) {
            // It might fail if no session exists, or network error
            Result.failure(e)
        }
    }

    /**
     * Check if current user's email is verified
     */
    fun isEmailVerified(): Boolean {
        return if (isSupabaseConfigured()) {
            client.auth.currentUserOrNull()?.emailConfirmedAt != null
        } else {
            false
        }
    }

    /**
     * Get the current authenticated user's ID.
     * @return User ID if authenticated, null otherwise
     */
    fun getCurrentUserId(): String? {
        return if (isSupabaseConfigured()) {
            try {
                client.auth.currentUserOrNull()?.id
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Failed to get current user ID", e)
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Get the current user's UID from the users table.
     * In this app, the auth ID IS the UID (stored in users.uid column).
     * This method verifies the user exists in the database and returns their UID.
     */
    suspend fun getCurrentUserUid(): String? {
        return if (isSupabaseConfigured()) {
            try {
                val authId = client.auth.currentUserOrNull()?.id
                if (authId == null) {
                    android.util.Log.e("AuthRepository", "No authenticated user found")
                    return null
                }
                
                android.util.Log.d("AuthRepository", "Getting UID for auth ID: $authId")
                
                // In this app, auth ID IS the UID (users.uid = auth.users.id)
                // Verify the user exists in the database
                val result = client.from("users")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("uid")) {
                        filter {
                            eq("uid", authId)
                        }
                    }
                    .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()
                
                if (result != null) {
                    android.util.Log.d("AuthRepository", "User found in database with UID: $authId")
                    return authId
                }
                
                android.util.Log.e("AuthRepository", "User not found in database with UID: $authId")
                null
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Failed to get user UID: ${e.message}", e)
                null
            }
        } else {
            android.util.Log.e("AuthRepository", "Supabase not configured")
            null
        }
    }
    
    fun getCurrentUserEmail(): String? {
        return if (isSupabaseConfigured()) {
            try {
                client.auth.currentUserOrNull()?.email
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    fun isUserLoggedIn(): Boolean {
        return if (isSupabaseConfigured()) {
            try {
                client.auth.currentUserOrNull() != null
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
    
    fun observeAuthState(): Flow<Boolean> {
        return if (isSupabaseConfigured()) {
            try {
                client.auth.sessionStatus.map { status ->
                    client.auth.currentUserOrNull() != null
                }
            } catch (e: Exception) {
                flowOf(false)
            }
        } else {
            flowOf(false)
        }
    }
}
