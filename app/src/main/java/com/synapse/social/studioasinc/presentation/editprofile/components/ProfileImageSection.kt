package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.settings.SettingsColors
import com.synapse.social.studioasinc.ui.settings.SettingsShapes

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ProfileImageSection(
    coverUrl: String?,
    avatarUrl: String?,
    onCoverClick: () -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = 2.dp
    ) {
        Box(
             modifier = Modifier.fillMaxWidth().padding(bottom = 56.dp)
        ) {
             Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(onClick = onCoverClick)
            ) {
                 if (coverUrl != null) {
                    GlideImage(
                        model = coverUrl,
                        contentDescription = "Cover photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                     GlideImage(
                        model = R.drawable.user_null_cover_photo,
                        contentDescription = "Cover photo placeholder",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

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
                     if (avatarUrl != null) {
                        GlideImage(
                            model = avatarUrl,
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

                 Box(
                     modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                     contentAlignment = Alignment.Center
                 ) {
                     Icon(
                         painter = painterResource(R.drawable.ic_edit),
                         contentDescription = "Edit avatar",
                         tint = MaterialTheme.colorScheme.onPrimary,
                         modifier = Modifier.size(16.dp)
                     )
                 }
            }
        }
    }
}
