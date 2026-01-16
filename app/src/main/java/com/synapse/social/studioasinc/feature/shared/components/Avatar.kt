package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R

@Composable
fun CircularAvatar(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null
) {
    val imageModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.ic_profile_placeholder),
            error = painterResource(id = R.drawable.ic_profile_placeholder)
        )
    } else {
        // Fallback or placeholder if imageUrl is null
         androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.ic_profile_placeholder),
            contentDescription = contentDescription,
            modifier = imageModifier,
            contentScale = ContentScale.Crop
        )
    }
}
