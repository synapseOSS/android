package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun StatItem(
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp) // MD3: Increased touch target
    ) {
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.titleLarge, // MD3: Larger title
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp)) // MD3: 4dp spacing
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium, // MD3: Body medium for labels
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatsRow(
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    onPostsClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(
            label = "Posts",
            count = postsCount,
            onClick = onPostsClick,
            modifier = Modifier.weight(1f)
        )
        StatItem(
            label = "Followers", 
            count = followersCount,
            onClick = onFollowersClick,
            modifier = Modifier.weight(1f)
        )
        StatItem(
            label = "Following",
            count = followingCount,
            onClick = onFollowingClick,
            modifier = Modifier.weight(1f)
        )
    }
}

fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000_000 -> {
            val formatted = count / 1_000_000_000.0
            if (formatted == formatted.toLong().toDouble()) {
                "${formatted.toLong()}B"
            } else {
                String.format("%.1fB", formatted)
            }
        }
        count >= 1_000_000 -> {
            val formatted = count / 1_000_000.0
            if (formatted == formatted.toLong().toDouble()) {
                "${formatted.toLong()}M"
            } else {
                String.format("%.1fM", formatted)
            }
        }
        count >= 1_000 -> {
            val formatted = count / 1_000.0
            String.format("%.1fK", formatted)
        }
        else -> count.toString()
    }
}

@Preview(showBackground = true)
@Composable
private fun StatItemPreview() {
    MaterialTheme {
        StatItem(
            label = "Followers",
            count = 1,
            onClick = {}
        )
    }
}
