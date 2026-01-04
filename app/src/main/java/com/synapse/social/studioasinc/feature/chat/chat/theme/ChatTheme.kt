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
import com.synapse.social.studioasinc.domain.model.ChatThemePreset

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
    val MessageHorizontalPadding = 4.dp  // Reduced from 8dp to 4dp
    val BubblePaddingHorizontal = 12.dp
    val BubblePaddingVertical = 8.dp
    val ReplyBarHeight = 52.dp
    val InputBarMinHeight = 56.dp
    val AvatarSize = 24.dp  // Reduced from 32dp to 24dp
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
    
    // =============================================
    // MESSAGE SEND ANIMATION SPEC
    // =============================================
    
    // Input clearing animation
    val SendInputClearDuration = 100       // Input field text fade + height shrink
    
    // Message bubble entrance
    val SendBubbleEnterDuration = 150      // Fade-in + scale-up
    val SendBubbleTranslationY = 4.dp      // Vertical float distance from bottom
    
    // Scroll coordination
    val SendScrollDelay = 50               // Delay before scroll starts (let bubble animate first)
    val SendScrollDuration = 200           // Smooth scroll duration
    
    // Send button
    val SendButtonClickDuration = 50       // Ripple/overlay flash
    val SendIconCrossfadeDuration = 100    // Icon transition (was 300, now per spec)
    
    // Custom easing per spec: cubic-bezier(0.25, 0.46, 0.45, 0.94)
    // This is a smooth ease-out curve for premium feel
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

private fun getLightChatColors(preset: ChatThemePreset): ChatThemeColors {
    val (primaryColor, secondaryColor) = when (preset) {
        ChatThemePreset.DEFAULT -> ChatColors.SentBubbleStart to ChatColors.SentBubbleEnd
        ChatThemePreset.OCEAN -> Color(0xFF1976D2) to Color(0xFF2196F3)
        ChatThemePreset.FOREST -> Color(0xFF388E3C) to Color(0xFF4CAF50)
        ChatThemePreset.SUNSET -> Color(0xFFF57C00) to Color(0xFFFF9800)
        ChatThemePreset.MONOCHROME -> Color(0xFF424242) to Color(0xFF616161)
    }

    return ChatThemeColors(
        sentBubbleGradient = Brush.linearGradient(
            colors = listOf(primaryColor, secondaryColor)
        ),
        sentBubbleText = ChatColors.SentBubbleText,
        sentBubbleSecondaryText = ChatColors.SentBubbleSecondaryText,

        receivedBubble = ChatColors.ReceivedBubbleLight,
        receivedBubbleText = ChatColors.ReceivedBubbleText,
        receivedBubbleSecondaryText = ChatColors.ReceivedBubbleSecondaryText,

        statusSending = ChatColors.StatusSending,
        statusSent = ChatColors.StatusSent,
        statusDelivered = secondaryColor,
        statusRead = secondaryColor,
        statusFailed = ChatColors.StatusFailed,

        inputBarBackground = ChatColors.InputBarBackground,
        inputFieldBackground = ChatColors.InputFieldBackground,
        inputText = ChatColors.InputText,
        inputPlaceholder = ChatColors.InputPlaceholder,

        replyAccent = primaryColor,
        replyBackground = primaryColor.copy(alpha = 0.1f),
        sendButtonActive = primaryColor,
        sendButtonInactive = ChatColors.SendButtonInactive,

        chatBackground = ChatColors.ChatBackgroundLight,
        dateHeaderBackground = ChatColors.DateHeaderBackground,
        dateHeaderText = ChatColors.DateHeaderText,

        typingDot = ChatColors.TypingDot,
        typingBackground = ChatColors.TypingBackground
    )
}

private fun getDarkChatColors(preset: ChatThemePreset): ChatThemeColors {
     val (primaryColor, secondaryColor) = when (preset) {
        ChatThemePreset.DEFAULT -> ChatColors.SentBubbleStart to ChatColors.SentBubbleEnd
        ChatThemePreset.OCEAN -> Color(0xFF1565C0) to Color(0xFF1E88E5)
        ChatThemePreset.FOREST -> Color(0xFF2E7D32) to Color(0xFF43A047)
        ChatThemePreset.SUNSET -> Color(0xFFEF6C00) to Color(0xFFF57C00)
        ChatThemePreset.MONOCHROME -> Color(0xFF212121) to Color(0xFF424242)
    }

    return ChatThemeColors(
        sentBubbleGradient = Brush.linearGradient(
            colors = listOf(primaryColor, secondaryColor)
        ),
        sentBubbleText = ChatColors.SentBubbleText,
        sentBubbleSecondaryText = ChatColors.SentBubbleSecondaryText,

        receivedBubble = ChatColors.ReceivedBubbleDark,
        receivedBubbleText = ChatColors.ReceivedBubbleTextDark,
        receivedBubbleSecondaryText = ChatColors.ReceivedBubbleSecondaryTextDark,

        statusSending = ChatColors.StatusSending,
        statusSent = ChatColors.StatusSent,
        statusDelivered = secondaryColor,
        statusRead = secondaryColor,
        statusFailed = ChatColors.StatusFailed,

        inputBarBackground = ChatColors.InputBarBackgroundDark,
        inputFieldBackground = ChatColors.InputFieldBackgroundDark,
        inputText = ChatColors.InputTextDark,
        inputPlaceholder = ChatColors.InputPlaceholder,

        replyAccent = primaryColor,
        replyBackground = primaryColor.copy(alpha = 0.2f),
        sendButtonActive = primaryColor,
        sendButtonInactive = ChatColors.SendButtonInactive,

        chatBackground = ChatColors.ChatBackgroundDark,
        dateHeaderBackground = ChatColors.DateHeaderBackgroundDark,
        dateHeaderText = ChatColors.DateHeaderTextDark,

        typingDot = ChatColors.TypingDotLight,
        typingBackground = ChatColors.TypingBackgroundDark
    )
}

val LocalChatColors = staticCompositionLocalOf { getLightChatColors(ChatThemePreset.DEFAULT) }

/**
 * Provides chat-specific theme colors based on system theme
 */
@Composable
fun ChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    preset: ChatThemePreset = ChatThemePreset.DEFAULT,
    content: @Composable () -> Unit
) {
    val chatColors = if (darkTheme) getDarkChatColors(preset) else getLightChatColors(preset)
    
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

    /**
     * Get the primary color for a given theme preset (useful for previews).
     */
    fun getPreviewColor(preset: ChatThemePreset): Color {
        return when (preset) {
            ChatThemePreset.DEFAULT -> ChatColors.SentBubbleStart
            ChatThemePreset.OCEAN -> Color(0xFF1976D2)
            ChatThemePreset.FOREST -> Color(0xFF388E3C)
            ChatThemePreset.SUNSET -> Color(0xFFF57C00)
            ChatThemePreset.MONOCHROME -> Color(0xFF424242)
        }
    }
}
