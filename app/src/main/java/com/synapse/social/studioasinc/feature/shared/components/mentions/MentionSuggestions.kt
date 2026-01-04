package com.synapse.social.studioasinc.ui.components.mentions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.domain.model.SearchResult

@Composable
fun MentionSuggestions(
    suggestions: List<SearchResult.User>,
    onUserSelected: (SearchResult.User) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(suggestions) { user ->
                MentionUserItem(user = user, onClick = { onUserSelected(user) })
            }
        }
    }
}

@Composable
fun MentionUserItem(
    user: SearchResult.User,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(user.nickname ?: user.username) },
        supportingContent = { Text("@${user.username}") },
        leadingContent = {
            AsyncImage(
                model = user.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
