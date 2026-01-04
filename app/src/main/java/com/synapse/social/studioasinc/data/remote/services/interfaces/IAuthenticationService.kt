package com.synapse.social.studioasinc.data.remote.services.interfaces

import com.synapse.social.studioasinc.data.remote.services.User
import com.synapse.social.studioasinc.data.remote.services.AuthResult

/**
 * Authentication Service Interface
 */
interface IAuthenticationService {
    suspend fun signUp(email: String, password: String): Result<AuthResult>
    suspend fun signIn(email: String, password: String): Result<AuthResult>
    suspend fun signOut(): Result<Unit>
    suspend fun resendVerificationEmail(email: String): Result<Unit>
    suspend fun checkEmailVerified(email: String): Result<Boolean>
    fun getCurrentUser(): User?
    fun getCurrentUserId(): String?
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun updateEmail(newEmail: String): Result<Unit>
}
