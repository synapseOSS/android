package com.synapse.social.studioasinc.backend.interfaces

/**
 * A generic interface representing the result of an authentication operation.
 * This abstracts the underlying BaaS provider's specific AuthResult class.
 */
interface IAuthResult {
    fun isSuccessful(): Boolean
    fun getUser(): IUser?
}

/**
 * A generic interface representing a user from the BaaS provider.
 * This abstracts the underlying BaaS provider's specific User class.
 */
interface IUser {
    fun getUid(): String
}
