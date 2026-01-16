package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.core.util.ImageLoader
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.settings.SettingsColors
import com.synapse.social.studioasinc.ui.settings.SettingsShapes

@Composable
fun ProfileImageSection(
    coverUrl: String?,
    avatarUrl: String?,
    avatarUploadState: com.synapse.social.studioasinc.presentation.editprofile.UploadState,
    coverUploadState: com.synapse.social.studioasinc.presentation.editprofile.UploadState,
    onCoverClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onRetryAvatarUpload: () -> Unit,
    onRetryCoverUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clickable(onClick = onCoverClick)
                ) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = if (coverUrl != null && coverUrl.isNotBlank()) {
                            ImageLoader.buildImageRequest(context, coverUrl)
                        } else {
                            R.drawable.user_null_cover_photo
                        },
                        contentDescription = "Cover photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Upload state overlay for cover
                    when (coverUploadState) {
                        is com.synapse.social.studioasinc.presentation.editprofile.UploadState.Uploading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        is com.synapse.social.studioasinc.presentation.editprofile.UploadState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_error),
                                        contentDescription = "Upload failed",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    if (coverUploadState.canRetry) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        androidx.compose.material3.TextButton(
                                            onClick = onRetryCoverUpload,
                                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit),
                                    contentDescription = "Edit cover photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 152.dp)
                        .clickable(onClick = onAvatarClick)
                ) {
                    Surface(
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (avatarUrl != null && avatarUrl.isNotBlank()) {
                            android.util.Log.d("ProfileImageSection", "Loading avatar from URL: $avatarUrl")
                            AsyncImage(
                                model = ImageLoader.buildImageRequest(LocalContext.current, avatarUrl),
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_person),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    // Upload state overlay for avatar
                    when (avatarUploadState) {
                        is com.synapse.social.studioasinc.presentation.editprofile.UploadState.Uploading -> {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        is com.synapse.social.studioasinc.presentation.editprofile.UploadState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_error),
                                        contentDescription = "Upload failed",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (avatarUploadState.canRetry) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        androidx.compose.material3.TextButton(
                                            onClick = onRetryAvatarUpload,
                                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Retry", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            // No overlay for success or idle state
                        }
                    }
                }
            }
        }
    }
}
