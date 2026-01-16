package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

/**
 * Account-related dialog composables.
 * 
 * This file contains all dialog components for account management:
 * - ChangeEmailDialog: For updating email address
 * - ChangePasswordDialog: For updating password with strength indicator
 * - DeleteAccountDialog: For account deletion confirmation
 * 
 * All dialogs use AlertDialog with 28dp corner radius and 12dp input corners.
 * 
 * Requirements: 2.3, 2.4, 2.6
 */

/**
 * Dialog for changing email address.
 * 
 * Displays an OutlinedTextField for new email with validation and a password
 * field for verification. Uses 12dp corner radius for inputs.
 * 
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback when email change is confirmed (newEmail, password)
 * @param isLoading Whether the operation is in progress
 * @param error Optional error message to display
 * 
 * Requirements: 2.3
 */
@Composable
fun ChangeEmailDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Change Email",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter your new email address and current password to verify the change.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // New Email Field
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("New Email") },
                    placeholder = { Text("email@example.com") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = error != null && error.contains("email", ignoreCase = true)
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Current Password") },
                    placeholder = { Text("Enter your password") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    visualTransformation = if (passwordVisible) VisualTransformation.None 
                                         else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newEmail.isNotBlank() && password.isNotBlank()) {
                                onConfirm(newEmail, password)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(
                                    if (passwordVisible) R.drawable.ic_visibility_off 
                                    else R.drawable.ic_visibility_off
                                ),
                                contentDescription = if (passwordVisible) "Hide password" 
                                                   else "Show password"
                            )
                        }
                    },
                    isError = error != null && error.contains("password", ignoreCase = true)
                )

                // Error message
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { onConfirm(newEmail, password) },
                enabled = !isLoading && newEmail.isNotBlank() && password.isNotBlank(),
                shape = SettingsShapes.itemShape
            ) {
                if (isLoading) {
                    ExpressiveLoadingIndicator(
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Change Email")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        },
        shape = SettingsShapes.cardShape
    )
}

/**
 * Dialog for changing password.
 * 
 * Displays secure text fields for current password, new password, and confirmation.
 * Includes a password strength indicator that updates in real-time.
 * 
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback when password change is confirmed (current, new, confirm)
 * @param isLoading Whether the operation is in progress
 * @param error Optional error message to display
 * @param calculatePasswordStrength Function to calculate password strength (0-4)
 * 
 * Requirements: 2.4
 */
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    calculatePasswordStrength: (String) -> Int = { 0 }
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val passwordStrength = remember(newPassword) { calculatePasswordStrength(newPassword) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter your current password and choose a new password. Your new password must be at least 8 characters long.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Current Password Field
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None 
                                         else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                painter = painterResource(
                                    if (currentPasswordVisible) R.drawable.ic_visibility_off 
                                    else R.drawable.ic_visibility_off
                                ),
                                contentDescription = if (currentPasswordVisible) "Hide password" 
                                                   else "Show password"
                            )
                        }
                    }
                )

                // New Password Field
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None 
                                         else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                painter = painterResource(
                                    if (newPasswordVisible) R.drawable.ic_visibility_off 
                                    else R.drawable.ic_visibility_off
                                ),
                                contentDescription = if (newPasswordVisible) "Hide password" 
                                                   else "Show password"
                            )
                        }
                    },
                    isError = newPassword.isNotEmpty() && newPassword.length < 8
                )

                // Password Strength Indicator
                if (newPassword.isNotEmpty()) {
                    PasswordStrengthIndicator(strength = passwordStrength)
                }

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None 
                                         else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (currentPassword.isNotBlank() && newPassword.isNotBlank() && 
                                confirmPassword.isNotBlank()) {
                                onConfirm(currentPassword, newPassword, confirmPassword)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                painter = painterResource(
                                    if (confirmPasswordVisible) R.drawable.ic_visibility_off 
                                    else R.drawable.ic_visibility_off
                                ),
                                contentDescription = if (confirmPasswordVisible) "Hide password" 
                                                   else "Show password"
                            )
                        }
                    },
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword
                )

                // Error message
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { onConfirm(currentPassword, newPassword, confirmPassword) },
                enabled = !isLoading && currentPassword.isNotBlank() && 
                         newPassword.length >= 8 && confirmPassword == newPassword,
                shape = SettingsShapes.itemShape
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Change Password")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        },
        shape = SettingsShapes.cardShape
    )
}

/**
 * Password strength indicator component.
 * 
 * Displays a visual indicator of password strength with color-coded bars.
 * 
 * @param strength Password strength level (0-4)
 */
@Composable
private fun PasswordStrengthIndicator(strength: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .then(
                            if (index < strength) {
                                Modifier.background(
                                    color = when (strength) {
                                        1 -> MaterialTheme.colorScheme.error
                                        2 -> MaterialTheme.colorScheme.tertiary
                                        3 -> MaterialTheme.colorScheme.primary
                                        4 -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = SettingsShapes.chipShape
                                )
                            } else {
                                Modifier.background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = SettingsShapes.chipShape
                                )
                            }
                        )
                )
            }
        }
        
        Text(
            text = when (strength) {
                0 -> "Enter a password"
                1 -> "Weak password"
                2 -> "Fair password"
                3 -> "Good password"
                4 -> "Strong password"
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = when (strength) {
                0 -> MaterialTheme.colorScheme.onSurfaceVariant
                1 -> MaterialTheme.colorScheme.error
                2 -> MaterialTheme.colorScheme.tertiary
                3, 4 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Dialog for account deletion confirmation.
 * 
 * Displays a warning icon and requires the user to type an exact confirmation
 * phrase to proceed with account deletion. Uses 28dp corner radius.
 * 
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback when deletion is confirmed (confirmationText)
 * @param isLoading Whether the operation is in progress
 * @param error Optional error message to display
 * 
 * Requirements: 2.6
 */
@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var confirmationText by remember { mutableStateOf("") }
    val requiredText = AccountSettingsViewModel.DELETE_ACCOUNT_CONFIRMATION

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Delete Account",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "This action cannot be undone. All your data, including posts, messages, and profile information will be permanently deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "To confirm, please type:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = SettingsShapes.inputShape
                ) {
                    Text(
                        text = requiredText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    label = { Text("Confirmation") },
                    placeholder = { Text("Type the phrase above") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (confirmationText == requiredText) {
                                onConfirm(confirmationText)
                            }
                        }
                    ),
                    isError = confirmationText.isNotEmpty() && confirmationText != requiredText,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.error,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )

                // Error message
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(confirmationText) },
                enabled = !isLoading && confirmationText == requiredText,
                shape = SettingsShapes.itemShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                if (isLoading) {
                    ExpressiveLoadingIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Delete Account")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        },
        shape = SettingsShapes.cardShape
    )
}
