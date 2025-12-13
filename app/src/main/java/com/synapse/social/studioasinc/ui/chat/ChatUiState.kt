package com.synapse.social.studioasinc.ui.chat

import com.synapse.social.studioasinc.data.model.UserProfile

/**
 * UI State definitions for Direct Chat Compose screen
 */

// =============================================
// MAIN SCREEN STATE
// =============================================

/**
 * Main UI state for the chat screen
 */
data class ChatUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val otherUser: ChatUserInfo? = null,
    val connectionState: RealtimeConnectionState = RealtimeConnectionState.Connected,
    val typingUsers: List<String> = emptyList(),
    val replyTo: MessageUiModel? = null,
    val isMultiSelectMode: Boolean = false,
    val selectedMessageIds: Set<String> = emptySet(),
    val hasMoreMessages: Boolean = true,
    val scrollToMessageId: String? = null,
    val inputText: String = "",
    val isRecordingVoice: Boolean = false,
    val attachments: List<AttachmentUiModel> = emptyList()
)

/**
 * Model representing a chat available for forwarding, including display name and avatar.
 */
data class ChatForwardUiModel(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val isGroup: Boolean
)

/**
 * Simplified user info for chat header
 */
data class ChatUserInfo(
    val id: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val isVerified: Boolean = false
)

/**
 * Realtime connection states
 */
enum class RealtimeConnectionState {
    Connected,
    Connecting,
    Disconnected,
    Reconnecting
}

// =============================================
// MESSAGE UI MODEL
// =============================================

/**
 * UI-ready message model for Compose consumption
 * Maps from domain Message model with computed display properties
 */
data class MessageUiModel(
    val id: String,
    val content: String,
    val messageType: MessageType,
    val senderId: String,
    val senderName: String?,
    val senderAvatarUrl: String?,
    val timestamp: Long,
    val formattedTime: String,
    val isFromCurrentUser: Boolean,
    val deliveryStatus: DeliveryStatus,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val replyTo: ReplyPreviewData? = null,
    val forwardedFrom: ForwardedData? = null,
    val attachments: List<AttachmentUiModel>? = null,
    val linkPreview: LinkPreviewData? = null,
    val voiceData: VoiceMessageData? = null,
    val position: MessagePosition = MessagePosition.Single,
    val isAnimating: Boolean = false,
    val isSelected: Boolean = false,
    val showDateHeader: Boolean = false,
    val dateHeaderText: String? = null
)

/**
 * Message position within a group (for corner radius styling)
 */
enum class MessagePosition {
    Single,  // Not part of a group
    First,   // First message in a group
    Middle,  // Middle message in a group
    Last     // Last message in a group
}

/**
 * Message delivery status for read receipts
 */
enum class DeliveryStatus {
    Sending,
    Sent,
    Delivered,
    Read,
    Failed
}

/**
 * Message content types
 */
enum class MessageType {
    Text,
    Image,
    Video,
    Voice,
    File,
    LinkPreview,
    Deleted
}

// =============================================
// NESTED DATA CLASSES
// =============================================

/**
 * Reply preview data shown above message
 */
data class ReplyPreviewData(
    val messageId: String,
    val senderName: String,
    val content: String,
    val thumbnailUrl: String? = null,
    val messageType: MessageType = MessageType.Text
)

/**
 * Forwarded message indicator data
 */
data class ForwardedData(
    val fromChatId: String,
    val fromMessageId: String
)

/**
 * Attachment data for media messages
 */
data class AttachmentUiModel(
    val id: String,
    val url: String,
    val type: AttachmentType,
    val thumbnailUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Long? = null,  // For video/audio
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f
)

/**
 * Attachment types
 */
enum class AttachmentType {
    Image,
    Video,
    Audio,
    Document,
    Unknown
}

/**
 * Link preview data
 */
data class LinkPreviewData(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val domain: String
)

/**
 * Voice message specific data
 */
data class VoiceMessageData(
    val url: String,
    val duration: Long,  // in milliseconds
    val waveform: List<Float>? = null,  // Normalized 0-1 values
    val isPlaying: Boolean = false,
    val playbackPosition: Long = 0
)

// =============================================
// CHAT EVENTS & EFFECTS
// =============================================

/**
 * One-time effects that should be handled by the UI
 */
sealed class ChatEffect {
    data class ScrollToMessage(val messageId: String, val highlight: Boolean = true) : ChatEffect()
    data class ShowError(val message: String) : ChatEffect()
    data class ShowSnackbar(val message: String) : ChatEffect()
    data object ScrollToBottom : ChatEffect()
    data class OpenMediaViewer(val attachments: List<AttachmentUiModel>, val startIndex: Int) : ChatEffect()
    data class OpenUserProfile(val userId: String) : ChatEffect()
    data class StartVoiceRecording(val maxDuration: Long) : ChatEffect()
    data object StopVoiceRecording : ChatEffect()
    data class PlayVoiceMessage(val messageId: String, val url: String) : ChatEffect()
    data object PauseVoiceMessage : ChatEffect()
    data class ShowMessageActions(val message: MessageUiModel) : ChatEffect()
    data class CopyToClipboard(val text: String) : ChatEffect()
    data object NavigateBack : ChatEffect()
}

/**
 * User actions/intents for the chat screen
 */
sealed class ChatIntent {
    // Message actions
    data class SendMessage(val content: String) : ChatIntent()
    data class SendMediaMessage(val attachments: List<AttachmentUiModel>, val caption: String?) : ChatIntent()
    data class SendVoiceMessage(val audioPath: String, val duration: Long) : ChatIntent()
    data class DeleteMessage(val messageId: String, val forEveryone: Boolean) : ChatIntent()
    data class EditMessage(val messageId: String, val newContent: String) : ChatIntent()
    data class ForwardMessage(val messageId: String, val toChatIds: List<String>) : ChatIntent()
    data class ReactToMessage(val messageId: String, val reaction: String) : ChatIntent()
    
    // Reply
    data class SetReplyTo(val message: MessageUiModel) : ChatIntent()
    data object ClearReply : ChatIntent()
    
    // Selection
    data class ToggleMessageSelection(val messageId: String) : ChatIntent()
    data object EnterMultiSelectMode : ChatIntent()
    data object ExitMultiSelectMode : ChatIntent()
    data object DeleteSelectedMessages : ChatIntent()
    data object ForwardSelectedMessages : ChatIntent()
    data object CopySelectedMessages : ChatIntent()  // Copies all selected text messages
    data class CopyToClipboard(val content: String) : ChatIntent()
    
    // Input
    data class UpdateInputText(val text: String) : ChatIntent()
    data class AddAttachment(val attachment: AttachmentUiModel) : ChatIntent()
    data class RemoveAttachment(val attachmentId: String) : ChatIntent()
    data object ClearAttachments : ChatIntent()
    
    // Voice
    data object StartVoiceRecording : ChatIntent()
    data object StopVoiceRecording : ChatIntent()
    data object CancelVoiceRecording : ChatIntent()
    
    // Navigation & scroll
    data class ScrollToMessage(val messageId: String) : ChatIntent()
    data object ScrollToBottom : ChatIntent()
    data object LoadMoreMessages : ChatIntent()
    data class MarkMessagesAsRead(val messageIds: List<String>) : ChatIntent()
    
    // Typing
    data object StartTyping : ChatIntent()
    data object StopTyping : ChatIntent()
    
    // Retry
    data class RetryFailedMessage(val messageId: String) : ChatIntent()
    
    // Error
    data object ClearError : ChatIntent()
}
