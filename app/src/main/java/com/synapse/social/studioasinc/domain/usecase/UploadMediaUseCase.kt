package com.synapse.social.studioasinc.domain.usecase

import android.net.Uri
import com.synapse.social.studioasinc.data.remote.services.SupabaseChatService
import com.synapse.social.studioasinc.data.remote.services.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.chat.interfaces.ChatAttachment
import com.synapse.social.studioasinc.chat.models.ChatAttachmentImpl
import com.synapse.social.studioasinc.chat.models.MessageType
import com.synapse.social.studioasinc.chat.service.MediaUploadManager
import com.synapse.social.studioasinc.model.models.MediaUploadResult
import com.synapse.social.studioasinc.model.models.UploadProgress
import com.synapse.social.studioasinc.model.models.UploadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Use case for orchestrating media uploads and sending messages with attachments.
 */
class UploadMediaUseCase(
    private val authService: IAuthenticationService,
    // Using SupabaseChatService directly because ChatRepository wraps it but doesn't expose all functionality
    // especially regarding attachment messages in a structured way compatible with current ViewModel logic
    private val chatService: SupabaseChatService = SupabaseChatService()
) {
    /**
     * Uploads multiple images and sends a message with attachments upon completion.
     * Note: This currently only tracks progress. The actual message sending logic
     * needs to be coordinated because MediaUploadManager.uploadMultiple returns a Flow of progress,
     * not the final results.
     *
     * Ideally, MediaUploadManager should return results in its flow.
     */
    suspend fun uploadImages(
        mediaUploadManager: MediaUploadManager,
        chatId: String,
        uris: List<Uri>
    ): Flow<UploadProgress> {
        return mediaUploadManager.uploadMultiple(uris, chatId)
    }

    /**
     * Uploads a single file (Video, Audio, Document) and sends a message with the attachment.
     */
    suspend fun uploadFileAndSend(
        mediaUploadManager: MediaUploadManager,
        chatId: String,
        uri: Uri,
        type: MediaType,
        caption: String
    ): Result<Unit> {
        val result = when (type) {
            MediaType.VIDEO -> mediaUploadManager.uploadVideo(uri, chatId)
            MediaType.AUDIO -> mediaUploadManager.uploadAudio(uri, chatId)
            MediaType.DOCUMENT -> mediaUploadManager.uploadDocument(uri, chatId)
            MediaType.IMAGE -> mediaUploadManager.uploadImage(uri, chatId)
        }

        return result.fold(
            onSuccess = { uploadResult ->
                sendMessageWithAttachment(chatId, uploadResult, caption)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    /**
     * Sends a message with the uploaded attachment.
     */
    suspend fun sendMessageWithAttachment(
        chatId: String,
        uploadResult: MediaUploadResult,
        caption: String
    ): Result<Unit> {
        val userId = authService.getCurrentUserId() ?: return Result.failure(IllegalStateException("User not authenticated"))

        val attachment = ChatAttachmentImpl(
            id = UUID.randomUUID().toString(),
            url = uploadResult.url,
            type = getAttachmentType(uploadResult.mimeType),
            fileName = uploadResult.fileName,
            fileSize = uploadResult.fileSize,
            thumbnailUrl = uploadResult.thumbnailUrl,
            width = uploadResult.width,
            height = uploadResult.height,
            duration = uploadResult.duration,
            mimeType = uploadResult.mimeType
        )

        val result = chatService.sendMessage(
            chatId = chatId,
            senderId = userId,
            content = caption.ifEmpty { "" },
            messageType = MessageType.ATTACHMENT,
            replyToId = null,
            attachments = listOf(attachment)
        )

        return result.map { }
    }

    private fun getAttachmentType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "image"
            mimeType.startsWith("video/") -> "video"
            mimeType.startsWith("audio/") -> "audio"
            else -> "document"
        }
    }

    enum class MediaType {
        IMAGE, VIDEO, AUDIO, DOCUMENT
    }
}
