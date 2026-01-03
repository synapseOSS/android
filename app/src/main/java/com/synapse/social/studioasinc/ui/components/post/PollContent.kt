package com.synapse.social.studioasinc.ui.components.post

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Data class representing a poll option for UI rendering.
 * Used in both PostCard (Feed) and PostDetailScreen.
 */
data class PollOption(
    val id: String,
    val text: String,
    val voteCount: Int,
    val isSelected: Boolean
)

/**
 * Modern animated poll component with smooth animations and visual feedback.
 * Works in both Home Feed and Post Detail screens.
 */
@Composable
fun PollContent(
    question: String,
    options: List<PollOption>,
    totalVotes: Int,
    hasVoted: Boolean = options.any { it.isSelected },
    onVote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Poll Question
        if (question.isNotBlank()) {
            Text(
                text = question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        
        // Poll Options
        options.forEach { option ->
            AnimatedPollOptionItem(
                option = option,
                totalVotes = totalVotes,
                hasVoted = hasVoted,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onVote(option.id)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        
        // Vote Count Summary
        Text(
            text = if (totalVotes == 1) "$totalVotes vote" else "$totalVotes votes",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Animated poll option with progress bar, selection state, and smooth transitions.
 */
@Composable
fun AnimatedPollOptionItem(
    option: PollOption,
    totalVotes: Int,
    hasVoted: Boolean,
    onClick: () -> Unit
) {
    val percentage = if (totalVotes > 0) option.voteCount.toFloat() / totalVotes else 0f
    val percentageText = if (hasVoted && totalVotes > 0) "${(percentage * 100).roundToInt()}%" else ""
    
    // Animation states
    var showProgress by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (showProgress && hasVoted) percentage else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "progress"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (option.isSelected) 2.5.dp else 1.dp,
        animationSpec = tween(durationMillis = 300),
        label = "border"
    )
    
    val borderColor by animateColorAsState(
        targetValue = when {
            option.isSelected -> MaterialTheme.colorScheme.primary
            hasVoted -> MaterialTheme.colorScheme.outlineVariant
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(durationMillis = 300),
        label = "borderColor"
    )
    
    val scaleState by animateFloatAsState(
        targetValue = if (option.isSelected) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    val checkIconScale by animateFloatAsState(
        targetValue = if (option.isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "checkScale"
    )
    
    // Trigger progress animation when component appears or vote state changes
    LaunchedEffect(hasVoted) {
        showProgress = hasVoted
    }
    
    // Gradient colors for progress bar
    val progressGradient = Brush.horizontalGradient(
        colors = listOf(
            if (option.isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
            if (option.isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scaleState)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                enabled = !hasVoted || option.isSelected,
                onClick = onClick
            )
    ) {
        // Animated Progress Background
        if (hasVoted) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .background(progressGradient)
            )
        }
        
        // Content Row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left section: Check icon + Text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Animated Check Icon
                if (option.isSelected) {
                    Box(
                        modifier = Modifier
                            .scale(checkIconScale)
                            .padding(end = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                Text(
                    text = option.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (option.isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            
            // Right section: Vote count and Percentage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasVoted && option.voteCount > 0) {
                    Text(
                        text = "${option.voteCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (percentageText.isNotEmpty()) {
                    Text(
                        text = percentageText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (option.isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (option.isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/**
 * Legacy PollOptionItem for backward compatibility.
 * Use AnimatedPollOptionItem for new implementations.
 */
@Composable
fun PollOptionItem(
    option: PollOption,
    totalVotes: Int,
    onClick: () -> Unit
) {
    AnimatedPollOptionItem(
        option = option,
        totalVotes = totalVotes,
        hasVoted = totalVotes > 0,
        onClick = onClick
    )
}
