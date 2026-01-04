package com.synapse.social.studioasinc.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.domain.model.Post
import kotlin.math.roundToInt

@Composable
fun PollDisplay(
    post: Post,
    userPollVote: Int?,
    onOptionSelected: (Int) -> Unit
) {
    val options = post.pollOptions ?: return
    val totalVotes = options.sumOf { it.votes }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        if (!post.pollQuestion.isNullOrEmpty()) {
            Text(
                text = post.pollQuestion,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        options.forEachIndexed { index, option ->
            PollOptionItem(
                option = option,
                totalVotes = totalVotes,
                isSelected = userPollVote == index,
                onClick = { onOptionSelected(index) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = pluralStringResource(id = R.plurals.poll_votes_count, count = totalVotes, totalVotes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun PollOptionItem(
    option: PollOption,
    totalVotes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val percentage = if (totalVotes > 0) (option.votes.toFloat() / totalVotes) else 0f
    val percentageText = if (totalVotes > 0) "${(percentage * 100).roundToInt()}%" else ""

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Progress bar background
        if (percentage > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                     Icon(
                        painter = painterResource(id = R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp).padding(end = 8.dp)
                    )
                }
                Text(
                    text = option.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else null
                )
            }

            if (percentageText.isNotEmpty()) {
                Text(
                    text = percentageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else null
                )
            }
        }
    }
}
