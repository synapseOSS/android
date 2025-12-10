package com.synapse.social.studioasinc.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Interface for chat attachments
 */
interface ChatAttachment {
    val id: String
    val url: String
    val type: String  // image, video, audio, document
    val fileName: String?
    val fileSize: Long?
    val thumbnailUrl: String?
}

/**
 * Implementation of ChatAttachment with enhanced media metadata
 */
@Serializable
data class ChatAttachmentImpl(
    override val id: String,
    override val url: String,
    override val type: String,
    @SerialName("file_name")
    override val fileName: String? = null,
    @SerialName("file_size")
    override val fileSize: Long? = null,
    @SerialName("thumbnail_url")
    override val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Long? = null,  // milliseconds for video/audio
    @SerialName("mime_type")
    val mimeType: String? = null
) : ChatAttachment
