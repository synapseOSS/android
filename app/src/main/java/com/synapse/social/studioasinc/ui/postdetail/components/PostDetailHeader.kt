package com.synapse.social.studioasinc.ui.postdetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.ui.components.post.PostHeader

// We can likely reuse the existing PostHeader, but let's wrap it if needed or use directly.
// The existing PostHeader takes a User object. PostDetailState has Author which might be different or map to it.
// Let's check com.synapse.social.studioasinc.domain.model.PostDetail Author type.
// Actually PostDetail.author is likely com.synapse.social.studioasinc.domain.model.User or similar.
// And PostHeader expects com.synapse.social.studioasinc.domain.model.User.
// We might need to map it.

@Composable
fun PostDetailHeader(
    user: User,
    timestamp: String,
    onUserClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PostHeader(
        user = user,
        timestamp = timestamp,
        onUserClick = onUserClick,
        onOptionsClick = onOptionsClick,
        modifier = modifier
    )
}
