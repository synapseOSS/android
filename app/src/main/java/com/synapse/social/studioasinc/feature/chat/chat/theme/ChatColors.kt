package com.synapse.social.studioasinc.ui.chat.theme

import androidx.compose.ui.graphics.Color

/**
 * Chat-specific expressive color palette for Material 3
 * Provides vibrant, premium colors for the chat experience
 */
object ChatColors {
    // =============================================
    // SENT MESSAGE BUBBLES - Vibrant gradient colors
    // =============================================
    val SentBubbleStart = Color(0xFF6B4EFF)      // Deep purple
    val SentBubbleEnd = Color(0xFF8B6CFF)        // Lighter purple
    val SentBubbleText = Color.White
    val SentBubbleSecondaryText = Color(0xFFE8E0FF)  // Light purple for timestamps
    
    // =============================================
    // RECEIVED MESSAGE BUBBLES - Soft neutral tones
    // =============================================
    val ReceivedBubbleLight = Color(0xFFF0F2F5)  // Light gray
    val ReceivedBubbleDark = Color(0xFF2D2D2D)   // Dark gray
    val ReceivedBubbleText = Color(0xFF1A1A1A)
    val ReceivedBubbleTextDark = Color(0xFFE6E1E5)
    val ReceivedBubbleSecondaryText = Color(0xFF6B7280)
    val ReceivedBubbleSecondaryTextDark = Color(0xFF9CA3AF)
    
    // =============================================
    // MESSAGE STATUS COLORS - Read receipts
    // =============================================
    val StatusSending = Color(0xFFBDBDBD)        // Gray - sending
    val StatusSent = Color(0xFF9E9E9E)           // Darker gray - sent
    val StatusDelivered = Color(0xFF64B5F6)      // Light blue - delivered
    val StatusRead = Color(0xFF34B7F1)           // Bright blue - read (WhatsApp style)
    val StatusFailed = Color(0xFFE53935)         // Red - failed
    
    // =============================================
    // TYPING INDICATOR
    // =============================================
    val TypingDot = Color(0xFF9E9E9E)
    val TypingDotLight = Color(0xFFBDBDBD)
    val TypingBackground = Color(0xFFE8E8E8)
    val TypingBackgroundDark = Color(0xFF3D3D3D)
    
    // =============================================
    // ACCENT COLORS
    // =============================================
    val ReplyAccent = Color(0xFF6B4EFF)          // Purple bar on reply
    val ReplyAccentLight = Color(0xFFEDE7FF)     // Light purple background

    val ForwardedAccent = Color(0xFF757575)      // Gray for forwarded
    val EditedAccent = Color(0xFF9E9E9E)         // Gray italic for edited
    
    // =============================================
    // INPUT BAR
    // =============================================
    val InputBarBackground = Color(0xFFF8F9FA)
    val InputBarBackgroundDark = Color(0xFF1E1E1E)
    val InputFieldBackground = Color.White
    val InputFieldBackgroundDark = Color(0xFF2D2D2D)
    val InputPlaceholder = Color(0xFF9CA3AF)
    val InputText = Color(0xFF1F2937)
    val InputTextDark = Color(0xFFF3F4F6)
    
    // =============================================
    // SEND/VOICE BUTTON
    // =============================================
    val SendButtonActive = Color(0xFF6B4EFF)     // Purple when text present
    val SendButtonInactive = Color(0xFF9CA3AF)   // Gray when empty
    val VoiceButtonRecording = Color(0xFFE53935) // Red when recording
    
    // =============================================
    // ATTACHMENT COLORS
    // =============================================
    val AttachmentImage = Color(0xFF4CAF50)      // Green
    val AttachmentVideo = Color(0xFF2196F3)      // Blue
    val AttachmentFile = Color(0xFFFF9800)       // Orange
    val AttachmentAudio = Color(0xFF9C27B0)      // Purple
    
    // =============================================
    // SELECTION & MULTI-SELECT
    // =============================================
    val SelectionOverlay = Color(0x336B4EFF)     // Semi-transparent purple
    val SelectionCheckmark = Color(0xFF6B4EFF)   // Purple checkmark
    
    // =============================================
    // CONNECTION STATUS
    // =============================================
    val ConnectionConnected = Color(0xFF4CAF50)  // Green
    val ConnectionConnecting = Color(0xFFFF9800) // Orange
    val ConnectionDisconnected = Color(0xFFE53935) // Red
    
    // =============================================
    // CHAT BACKGROUND
    // =============================================
    val ChatBackgroundLight = Color(0xFFF5F5F5)
    val ChatBackgroundDark = Color(0xFF121212)
    val ChatPatternLight = Color(0xFFE8E8E8)
    val ChatPatternDark = Color(0xFF1E1E1E)
    
    // =============================================
    // VOICE MESSAGE
    // =============================================
    val VoiceWaveformActive = Color(0xFF6B4EFF)
    val VoiceWaveformInactive = Color(0xFFBDBDBD)
    val VoicePlayButton = Color(0xFF6B4EFF)
    
    // =============================================
    // DATE HEADER
    // =============================================
    val DateHeaderBackground = Color(0xFFE0E0E0)
    val DateHeaderBackgroundDark = Color(0xFF424242)
    val DateHeaderText = Color(0xFF616161)
    val DateHeaderTextDark = Color(0xFFBDBDBD)
}
