package com.synapse.social.studioasinc.ui.chat.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors
import com.synapse.social.studioasinc.ui.chat.theme.ChatSpacing

/**
 * Sticky Date Header for grouping messages by day
 */
@Composable
fun DateHeader(
    date: String,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) ChatColors.DateHeaderBackgroundDark else ChatColors.DateHeaderBackground
    val textColor = if (isDarkTheme) ChatColors.DateHeaderTextDark else ChatColors.DateHeaderText

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ChatSpacing.DateHeaderVerticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = backgroundColor.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

@Preview
@Composable
private fun DateHeaderPreview() {
    DateHeader(date = "Today, Oct 24")
}
