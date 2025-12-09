package com.synapse.social.studioasinc.ui.chat.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Chat-specific theme extensions for Material 3 Expressive design
 */

// =============================================
// CORNER RADIUS FOR MESSAGE GROUPING
// =============================================

/**
 * Corner radius configuration for message bubbles based on grouping position
 */
data class MessageBubbleCorners(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp
) {
    fun toShape() = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart
    )
}

object ChatBubbleCorners {
    private val FULL_RADIUS = 20.dp
    private val SMALL_RADIUS = 6.dp
    
    // Single message (not grouped)
    val Single = MessageBubbleCorners(FULL_RADIUS, FULL_RADIUS, FULL_RADIUS, FULL_RADIUS)
    
    // Sent messages (right-aligned) - tail on right side
    val SentFirst = MessageBubbleCorners(FULL_RADIUS, FULL_RADIUS, SMALL_RADIUS, FULL_RADIUS)
    val SentMiddle = MessageBubbleCorners(FULL_RADIUS, SMALL_RADIUS, SMALL_RADIUS, FULL_RADIUS)
    val SentLast = MessageBubbleCorners(FULL_RADIUS, SMALL_RADIUS, FULL_RADIUS, FULL_RADIUS)
    
    // Received messages (left-aligned) - tail on left side
    val ReceivedFirst = MessageBubbleCorners(FULL_RADIUS, FULL_RADIUS, FULL_RADIUS, SMALL_RADIUS)
    val ReceivedMiddle = MessageBubbleCorners(SMALL_RADIUS, FULL_RADIUS, FULL_RADIUS, SMALL_RADIUS)
    val ReceivedLast = MessageBubbleCorners(SMALL_RADIUS, FULL_RADIUS, FULL_RADIUS, FULL_RADIUS)
}

// =============================================
// MESSAGE SPACING
// =============================================

object ChatSpacing {
    val MessageGrouped = 2.dp        // Between grouped messages
    val MessageUngrouped = 12.dp     // Between different senders
    val MessageHorizontalPadding = 8.dp
    val BubblePaddingHorizontal = 12.dp
    val BubblePaddingVertical = 8.dp
    val ReplyBarHeight = 52.dp
    val InputBarMinHeight = 56.dp
    val AvatarSize = 32.dp
    val DateHeaderVerticalPadding = 16.dp
}

// =============================================
// ANIMATION SPECS
// =============================================

object ChatAnimations {
    // Message entrance animation
    val MessageEntranceSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    // Send button morph animation
    val ButtonMorphSpec = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessMediumLow
    )
    
    // Typing dots animation
    val TypingDotDuration = 400
    val TypingDotDelayPerDot = 150
    
    // Reply bar slide animation
    val ReplyBarDuration = 200
    
    // Scroll to bottom FAB animation
    val FabScaleDuration = 150
    
    // Message selection animation
    val SelectionScaleDuration = 100
    
    // Swipe to reply threshold
    val SwipeToReplyThreshold = 80.dp
}

// =============================================
// CHAT THEME COLORS PROVIDER
// =============================================

data class ChatThemeColors(
    // Sent bubble
    val sentBubbleGradient: Brush,
    val sentBubbleText: Color,
    val sentBubbleSecondaryText: Color,
    
    // Received bubble
    val receivedBubble: Color,
    val receivedBubbleText: Color,
    val receivedBubbleSecondaryText: Color,
    
    // Status
    val statusSending: Color,
    val statusSent: Color,
    val statusDelivered: Color,
    val statusRead: Color,
    val statusFailed: Color,
    
    // Input bar
    val inputBarBackground: Color,
    val inputFieldBackground: Color,
    val inputText: Color,
    val inputPlaceholder: Color,
    
    // Accents
    val replyAccent: Color,
    val replyBackground: Color,
    val sendButtonActive: Color,
    val sendButtonInactive: Color,
    
    // Background
    val chatBackground: Color,
    val dateHeaderBackground: Color,
    val dateHeaderText: Color,
    
    // Typing
    val typingDot: Color,
    val typingBackground: Color
)

private val LightChatColors = ChatThemeColors(
    sentBubbleGradient = Brush.linearGradient(
        colors = listOf(ChatColors.SentBubbleStart, ChatColors.SentBubbleEnd)
    ),
    sentBubbleText = ChatColors.SentBubbleText,
    sentBubbleSecondaryText = ChatColors.SentBubbleSecondaryText,
    
    receivedBubble = ChatColors.ReceivedBubbleLight,
    receivedBubbleText = ChatColors.ReceivedBubbleText,
    receivedBubbleSecondaryText = ChatColors.ReceivedBubbleSecondaryText,
    
    statusSending = ChatColors.StatusSending,
    statusSent = ChatColors.StatusSent,
    statusDelivered = ChatColors.StatusDelivered,
    statusRead = ChatColors.StatusRead,
    statusFailed = ChatColors.StatusFailed,
    
    inputBarBackground = ChatColors.InputBarBackground,
    inputFieldBackground = ChatColors.InputFieldBackground,
    inputText = ChatColors.InputText,
    inputPlaceholder = ChatColors.InputPlaceholder,
    
    replyAccent = ChatColors.ReplyAccent,
    replyBackground = ChatColors.ReplyAccentLight,
    sendButtonActive = ChatColors.SendButtonActive,
    sendButtonInactive = ChatColors.SendButtonInactive,
    
    chatBackground = ChatColors.ChatBackgroundLight,
    dateHeaderBackground = ChatColors.DateHeaderBackground,
    dateHeaderText = ChatColors.DateHeaderText,
    
    typingDot = ChatColors.TypingDot,
    typingBackground = ChatColors.TypingBackground
)

private val DarkChatColors = ChatThemeColors(
    sentBubbleGradient = Brush.linearGradient(
        colors = listOf(ChatColors.SentBubbleStart, ChatColors.SentBubbleEnd)
    ),
    sentBubbleText = ChatColors.SentBubbleText,
    sentBubbleSecondaryText = ChatColors.SentBubbleSecondaryText,
    
    receivedBubble = ChatColors.ReceivedBubbleDark,
    receivedBubbleText = ChatColors.ReceivedBubbleTextDark,
    receivedBubbleSecondaryText = ChatColors.ReceivedBubbleSecondaryTextDark,
    
    statusSending = ChatColors.StatusSending,
    statusSent = ChatColors.StatusSent,
    statusDelivered = ChatColors.StatusDelivered,
    statusRead = ChatColors.StatusRead,
    statusFailed = ChatColors.StatusFailed,
    
    inputBarBackground = ChatColors.InputBarBackgroundDark,
    inputFieldBackground = ChatColors.InputFieldBackgroundDark,
    inputText = ChatColors.InputTextDark,
    inputPlaceholder = ChatColors.InputPlaceholder,
    
    replyAccent = ChatColors.ReplyAccent,
    replyBackground = ChatColors.ReplyAccent.copy(alpha = 0.2f),
    sendButtonActive = ChatColors.SendButtonActive,
    sendButtonInactive = ChatColors.SendButtonInactive,
    
    chatBackground = ChatColors.ChatBackgroundDark,
    dateHeaderBackground = ChatColors.DateHeaderBackgroundDark,
    dateHeaderText = ChatColors.DateHeaderTextDark,
    
    typingDot = ChatColors.TypingDotLight,
    typingBackground = ChatColors.TypingBackgroundDark
)

val LocalChatColors = staticCompositionLocalOf { LightChatColors }

/**
 * Provides chat-specific theme colors based on system theme
 */
@Composable
fun ChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val chatColors = if (darkTheme) DarkChatColors else LightChatColors
    
    CompositionLocalProvider(LocalChatColors provides chatColors) {
        content()
    }
}

/**
 * Access chat theme colors from composables
 */
object ChatTheme {
    val colors: ChatThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalChatColors.current
}
