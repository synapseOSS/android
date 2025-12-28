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

package com.synapse.social.studioasinc.util

import com.synapse.social.studioasinc.model.Attachment
import java.util.ArrayList
import java.util.HashMap

/**
 * Utility class for converting between HashMap-based attachments and type-safe Attachment objects.
 * Provides migration path from legacy HashMap format to new Parcelable format.
 */
object AttachmentUtils {

    /**
     * Converts a HashMap representation to an Attachment object.
     *
     * @param attachmentMap The HashMap containing attachment data
     * @return Attachment object or null if conversion fails
     */
    fun fromHashMap(attachmentMap: HashMap<String, Any?>?): Attachment? {
        if (attachmentMap == null) {
            return null
        }

        try {
            val attachment = Attachment()

            // Extract publicId
            val publicIdObj = attachmentMap["publicId"]
            if (publicIdObj != null) {
                attachment.publicId = publicIdObj.toString()
            }

            // Extract URL
            val urlObj = attachmentMap["url"]
            if (urlObj != null) {
                attachment.url = urlObj.toString()
            }

            // Extract dimensions
            val widthObj = attachmentMap["width"]
            if (widthObj is Number) {
                attachment.width = widthObj.toInt()
            }

            val heightObj = attachmentMap["height"]
            if (heightObj is Number) {
                attachment.height = heightObj.toInt()
            }

            // Extract type
            val typeObj = attachmentMap["type"]
            if (typeObj != null) {
                attachment.type = typeObj.toString()
            } else {
                // Infer type from publicId if not explicitly set
                val publicId = attachment.publicId
                if (publicId != null && publicId.contains("|video")) {
                    attachment.type = "video"
                } else {
                    attachment.type = "image"
                }
            }

            // Extract mimeType
            val mimeTypeObj = attachmentMap["mimeType"]
            if (mimeTypeObj != null) {
                attachment.mimeType = mimeTypeObj.toString()
            }

            // Extract size
            val sizeObj = attachmentMap["size"]
            if (sizeObj is Number) {
                attachment.size = sizeObj.toLong()
            }

            return attachment

        } catch (e: Exception) {
            // Log error but don't crash
            android.util.Log.e("AttachmentUtils", "Error converting HashMap to Attachment: " + e.message)
            return null
        }
    }

    /**
     * Converts an Attachment object to a HashMap representation.
     * Used for backward compatibility with existing code.
     *
     * @param attachment The Attachment object
     * @return HashMap representation
     */
    fun toHashMap(attachment: Attachment?): HashMap<String, Any>? {
        if (attachment == null) {
            return null
        }

        val map = HashMap<String, Any>()

        attachment.publicId?.let { map["publicId"] = it }
        attachment.url?.let { map["url"] = it }

        map["width"] = attachment.width
        map["height"] = attachment.height

        attachment.type?.let { map["type"] = it }
        attachment.mimeType?.let { map["mimeType"] = it }

        map["size"] = attachment.size

        return map
    }

    /**
     * Converts a list of HashMap attachments to a list of Attachment objects.
     *
     * @param attachmentMaps List of HashMap representations
     * @return List of Attachment objects
     */
    fun fromHashMapList(attachmentMaps: ArrayList<HashMap<String, Any?>>?): ArrayList<Attachment> {
        if (attachmentMaps == null) {
            return ArrayList()
        }

        val attachments = ArrayList<Attachment>()

        for (map in attachmentMaps) {
            val attachment = fromHashMap(map)
            if (attachment != null) {
                attachments.add(attachment)
            }
        }

        return attachments
    }

    /**
     * Converts a list of Attachment objects to a list of HashMap representations.
     *
     * @param attachments List of Attachment objects
     * @return List of HashMap representations
     */
    fun toHashMapList(attachments: ArrayList<Attachment>?): ArrayList<HashMap<String, Any>> {
        if (attachments == null) {
            return ArrayList()
        }

        val maps = ArrayList<HashMap<String, Any>>()

        for (attachment in attachments) {
            val map = toHashMap(attachment)
            if (map != null) {
                maps.add(map)
            }
        }

        return maps
    }
}
