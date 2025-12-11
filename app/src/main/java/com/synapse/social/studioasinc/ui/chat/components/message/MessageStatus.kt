package com.synapse.social.studioasinc.ui.chat.components.message

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.chat.DeliveryStatus
import com.synapse.social.studioasinc.ui.chat.theme.ChatColors

/**
 * Message delivery status icon with animated color transitions
 * 
 * Status icons:
 * - Sending: Clock icon (gray)
 * - Sent: Single check (gray)
 * - Delivered: Double check (blue-gray)
 * - Read: Double check (blue)
 * - Failed: Error icon (red)
 */
@Composable
fun MessageStatusIcon(
    status: DeliveryStatus,
    modifier: Modifier = Modifier
) {
    val (icon, tintColor) = getStatusIconAndColor(status)
    
    // Animate color transitions for smooth read receipt updates
    val animatedColor by animateColorAsState(
        targetValue = tintColor,
        animationSpec = tween(300),
        label = "statusColor"
    )
    
    Icon(
        imageVector = icon,
        contentDescription = getStatusDescription(status),
        modifier = modifier.size(16.dp),
        tint = animatedColor
    )
}

/**
 * Get the icon and color for each delivery status
 */
private fun getStatusIconAndColor(status: DeliveryStatus): Pair<ImageVector, Color> {
    return when (status) {
        DeliveryStatus.Sending -> Icons.Filled.AccessTime to ChatColors.StatusSending
        DeliveryStatus.Sent -> Icons.Filled.Check to ChatColors.StatusSent
        DeliveryStatus.Delivered -> Icons.Filled.DoneAll to ChatColors.StatusDelivered
        DeliveryStatus.Read -> Icons.Filled.DoneAll to ChatColors.StatusRead
        DeliveryStatus.Failed -> Icons.Filled.Error to ChatColors.StatusFailed
    }
}

/**
 * Get accessibility description for status
 */
@Composable
private fun getStatusDescription(status: DeliveryStatus): String {
    return when (status) {
        DeliveryStatus.Sending -> stringResource(id = R.string.msg_status_sending)
        DeliveryStatus.Sent -> stringResource(id = R.string.msg_status_sent)
        DeliveryStatus.Delivered -> stringResource(id = R.string.msg_status_delivered)
        DeliveryStatus.Read -> stringResource(id = R.string.msg_status_read)
        DeliveryStatus.Failed -> stringResource(id = R.string.msg_status_failed)
    }
}

// =============================================
// PREVIEWS
// =============================================

@Preview(showBackground = true)
@Composable
private fun MessageStatusPreview() {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        MessageStatusIcon(status = DeliveryStatus.Sending)
        MessageStatusIcon(status = DeliveryStatus.Sent)
        MessageStatusIcon(status = DeliveryStatus.Delivered)
        MessageStatusIcon(status = DeliveryStatus.Read)
        MessageStatusIcon(status = DeliveryStatus.Failed)
    }
}


