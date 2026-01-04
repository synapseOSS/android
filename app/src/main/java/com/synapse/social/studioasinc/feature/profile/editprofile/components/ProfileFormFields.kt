package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.presentation.editprofile.UsernameValidation
import com.synapse.social.studioasinc.ui.settings.SettingsColors
import com.synapse.social.studioasinc.ui.settings.SettingsShapes

@Composable
fun ProfileFormFields(
    username: String,
    onUsernameChange: (String) -> Unit,
    usernameValidation: UsernameValidation,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    nicknameError: String?,
    bio: String,
    onBiographyChange: (String) -> Unit,
    bioError: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SettingsColors.cardBackground,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Username Field
            UsernameField(
                value = username,
                onValueChange = onUsernameChange,
                validation = usernameValidation
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nickname Field
            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text("Display Name") },
                placeholder = { Text("Enter display name (optional)") },
                leadingIcon = {
                    Icon(painter = painterResource(R.drawable.ic_person), contentDescription = null)
                },
                supportingText = {
                    if (nicknameError != null) {
                        Text(nicknameError, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Optional - shown instead of username")
                    }
                },
                isError = nicknameError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = SettingsShapes.inputShape,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Biography Field
            OutlinedTextField(
                value = bio,
                onValueChange = onBiographyChange,
                label = { Text("Bio") },
                placeholder = { Text("Tell us about yourself (optional)") },
                supportingText = {
                    val currentLength = bio.length
                    Text(
                        text = if (bioError != null) bioError else "$currentLength/250",
                        color = if (bioError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                isError = bioError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = SettingsShapes.inputShape,
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text
                )
            )
        }
    }
}

@Composable
fun UsernameField(
    value: String,
    onValueChange: (String) -> Unit,
    validation: UsernameValidation
) {
    val isError = validation is UsernameValidation.Error
    val errorMessage = if (validation is UsernameValidation.Error) validation.message else null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Username") },
        placeholder = { Text("Enter username") },
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_person), contentDescription = null)
        },
        trailingIcon = {
            when (validation) {
                is UsernameValidation.Checking -> {
                    ExpressiveLoadingIndicator(
                        modifier = Modifier.size(20.dp)
                    )
                }
                is UsernameValidation.Valid -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = "Valid",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                is UsernameValidation.Error -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_error),
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        supportingText = {
            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            } else {
                Text("@username format")
            }
        },
        isError = isError,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.inputShape,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = KeyboardType.Ascii
        ),
        singleLine = true
    )
}
