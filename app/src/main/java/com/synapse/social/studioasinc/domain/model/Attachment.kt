/**
 * CONFIDENTIAL AND PROPRIETARY
 *
 * This source code is the sole property of StudioAs Inc. Synapse. (Ashik).
 * Any reproduction, modification, distribution, or exploitation in any form
 * without explicit written permission from the owner is strictly prohibited.
 *
 * Copyright (c) 2025 StudioAs Inc. Synapse. (Ashik)
 * All rights reserved.
 */

package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.Serializable

/**
 * Data class representing a message attachment.
 * Pure domain model without Android framework dependencies.
 */
@Serializable
data class Attachment(
    var publicId: String? = null,
    var url: String? = null,
    var width: Int = 0,
    var height: Int = 0,
    var type: String? = null,
    var mimeType: String? = null,
    var size: Long = 0
) {

    constructor(publicId: String?, url: String?, width: Int, height: Int, type: String?) : this(
        publicId = publicId,
        url = url,
        width = width,
        height = height,
        type = type,
        mimeType = null,
        size = 0
    )

    /**
     * Checks if this attachment is a video based on the publicId containing "|video"
     * or the type being "video".
     *
     * @return true if this is a video attachment
     */
    fun isVideo(): Boolean {
        return "video" == type || (publicId != null && publicId!!.contains("|video"))
    }

    /**
     * Checks if this attachment is an image.
     *
     * @return true if this is an image attachment
     */
    fun isImage(): Boolean {
        return "image" == type || !isVideo()
    }

    /**
     * Gets the aspect ratio of the attachment.
     *
     * @return the aspect ratio (width/height), or 1.0 if dimensions are invalid
     */
    fun getAspectRatio(): Float {
        if (height > 0 && width > 0) {
            return width.toFloat() / height
        }
        return 1.0f
    }

    /**
     * Checks if the attachment has a portrait orientation (taller than wide).
     *
     * @return true if height > width
     */
    fun isPortrait(): Boolean {
        return height > width && width > 0
    }

    /**
     * Checks if the attachment has a landscape orientation (wider than tall).
     *
     * @return true if width > height
     */
    fun isLandscape(): Boolean {
        return width > height && height > 0
    }
}
