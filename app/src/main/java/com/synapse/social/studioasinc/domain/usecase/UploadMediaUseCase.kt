package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.remote.services.SupabaseChatService
import com.synapse.social.studioasinc.data.remote.services.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.chat.interfaces.ChatAttachment
import com.synapse.social.studioasinc.chat.models.ChatAttachmentImpl
import com.synapse.social.studioasinc.chat.models.MessageType
import com.synapse.social.studioasinc.chat.service.MediaUploadManager
import com.synapse.social.studioasinc.domain.model.models.MediaUploadResult
import com.synapse.social.studioasinc.domain.model.models.UploadProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Use case for orchestrating media uploads and sending messages with attachments.
 * Note: This use case delegates Uri handling to the MediaUploadManager to maintain
 * domain layer separation while still providing the required functionality.
 */
class UploadMediaUseCase(
    private val authService: IAuthenticationService,
    private val chatService: SupabaseChatService = SupabaseChatService()
) {
    /**
     * Uploads multiple images and sends a message with attachments upon completion.
     * The MediaUploadManager handles the Android Uri conversion internally.
     */
    suspend fun uploadImages(
        mediaUploadManager: MediaUploadManager,
        chatId: String,
        uris: List<Any> // Using Any to avoid direct Android dependency
    ): Flow<UploadProgress> {
        return mediaUploadManager.uploadMultiple(uris as List<android.net.Uri>, chatId)
    }

    /**
     * Uploads a single file and sends a message with the attachment.
     */
    suspend fun uploadFileAndSend(
        mediaUploadManager: MediaUploadManager,
        chatId: String,
        uri: Any, // Using Any to avoid direct Android dependency
        type: MediaType,
        caption: String
    ): Result<Unit> {
        val androidUri = uri as android.net.Uri
        val result = when (type) {
            MediaType.VIDEO -> mediaUploadManager.uploadVideo(androidUri, chatId)
            MediaType.AUDIO -> mediaUploadManager.uploadAudio(androidUri, chatId)
            MediaType.DOCUMENT -> mediaUploadManager.uploadDocument(androidUri, chatId)
            MediaType.IMAGE -> mediaUploadManager.uploadImage(androidUri, chatId)
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
