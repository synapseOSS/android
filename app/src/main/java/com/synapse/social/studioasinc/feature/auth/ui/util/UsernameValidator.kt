package com.synapse.social.studioasinc.feature.auth.ui.util

object UsernameValidator {
    private const val MIN_LENGTH = 3
    private const val MAX_LENGTH = 20
    private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]+$")

    fun validate(username: String): ValidationResult {
        return when {
            username.isEmpty() -> ValidationResult.Error("Username is required")
            username.length < MIN_LENGTH ->
                ValidationResult.Error("Username must be at least $MIN_LENGTH characters")
            username.length > MAX_LENGTH ->
                ValidationResult.Error("Username must be at most $MAX_LENGTH characters")
            !username.matches(USERNAME_REGEX) ->
                ValidationResult.Error("Username can only contain letters, numbers, and underscores")
            else -> ValidationResult.Valid
        }
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
