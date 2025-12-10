package com.synapse.social.studioasinc.ui.components.post

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Assuming a simple Poll model structure for UI
data class PollOption(
    val id: String,
    val text: String,
    val voteCount: Int,
    val isSelected: Boolean
)

@Composable
fun PollContent(
    question: String,
    options: List<PollOption>,
    totalVotes: Int,
    onVote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        options.forEach { option ->
            PollOptionItem(
                option = option,
                totalVotes = totalVotes,
                onClick = { onVote(option.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = "$totalVotes votes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PollOptionItem(
    option: PollOption,
    totalVotes: Int,
    onClick: () -> Unit
) {
    val progress = if (totalVotes > 0) option.voteCount.toFloat() / totalVotes else 0f

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option.text,
                    modifier = Modifier.weight(1f)
                )
                if (totalVotes > 0) { // Only show percentage if voted
                    Text(
                        text = "${(progress * 100).toInt()}%"
                    )
                }
            }
             if (totalVotes > 0) {
                 Spacer(modifier = Modifier.height(4.dp))
                 LinearProgressIndicator(
                     progress = { progress },
                     modifier = Modifier.fillMaxWidth(),
                 )
             }
        }
    }
}
