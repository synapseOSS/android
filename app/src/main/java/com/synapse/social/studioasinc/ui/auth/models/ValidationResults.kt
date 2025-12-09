package com.synapse.social.studioasinc.ui.auth.models

/**
 * Data class representing the result of email validation.
 * @param isValid Whether the email format is valid
 * @param error Error message if validation failed, null if valid
 */
data class EmailValidationResult(
    val isValid: Boolean,
    val error: String? = null
)

/**
 * Data class representing the result of password validation.
 * @param isValid Whether the password meets requirements
 * @param strength The calculated password strength level
 * @param error Error message if validation failed, null if valid
 */
data class PasswordValidationResult(
    val isValid: Boolean,
    val strength: PasswordStrength,
    val error: String? = null
)

/**
 * Data class representing the result of username validation.
 * @param isValid Whether the username meets requirements
 * @param error Error message if validation failed, null if valid
 */
data class UsernameValidationResult(
    val isValid: Boolean,
    val error: String? = null
)

/**
 * Data class representing the result of form validation.
 * @param isValid Whether all form fields are valid
 * @param errors Map of field names to error messages for invalid fields
 */
data class FormValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String> = emptyMap()
)
