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

package com.synapse.social.studioasinc.core.config

/**
 * Configuration constants for Cloudinary image/video service.
 * Centralizes all cloud-related URLs and parameters for easy maintenance.
 */
object CloudinaryConfig {

    // Base Cloudinary configuration
    const val CLOUD_NAME = "synapse-social" // Updated with actual cloud name
    const val BASE_URL = "https://res.cloudinary.com/$CLOUD_NAME/image/upload/"
    const val VIDEO_BASE_URL = "https://res.cloudinary.com/$CLOUD_NAME/video/upload/"

    // Common image transformations
    object ImageTransformations {
        // Gallery thumbnails (optimized for actual view size)
        const val GALLERY_THUMBNAIL = "w_400,h_400,c_fill,q_auto,f_auto"

        // Reply message preview (120dp = ~120px on mdpi)
        const val REPLY_PREVIEW = "w_120,h_120,c_fill,q_auto,f_auto"

        // Carousel images (200dp = ~200px on mdpi, but we'll optimize for density)
        const val CAROUSEL_IMAGE_SMALL = "w_200,h_200,c_fill,q_auto,f_auto"
        const val CAROUSEL_IMAGE_MEDIUM = "w_400,h_400,c_fill,q_auto,f_auto"
        const val CAROUSEL_IMAGE_LARGE = "w_600,h_600,c_fill,q_auto,f_auto"

        // Full resolution for gallery
        const val FULL_RESOLUTION = "q_auto,f_auto"

        // Profile pictures
        const val PROFILE_SMALL = "w_100,h_100,c_fill,g_face,q_auto,f_auto"
        const val PROFILE_MEDIUM = "w_200,h_200,c_fill,g_face,q_auto,f_auto"

        // Post images
        const val POST_THUMBNAIL = "w_300,h_300,c_fill,q_auto,f_auto"
        const val POST_FULL = "w_800,h_800,c_limit,q_auto,f_auto"
    }

    // Video transformations
    object VideoTransformations {
        const val VIDEO_THUMBNAIL = "w_400,h_400,c_fill,so_auto"
        const val VIDEO_PREVIEW = "w_200,h_200,c_fill,so_auto"
    }

    /**
     * Builds a complete Cloudinary URL for an image with the specified transformation.
     *
     * @param publicId The Cloudinary public ID of the image
     * @param transformation The transformation string (e.g., "w_400,h_400,c_fill")
     * @return Complete Cloudinary URL
     */
    fun buildImageUrl(publicId: String?, transformation: String?): String {
        if (publicId.isNullOrEmpty()) {
            return ""
        }

        if (transformation.isNullOrEmpty()) {
            return BASE_URL + publicId
        }

        return "$BASE_URL$transformation/$publicId"
    }

    /**
     * Builds a complete Cloudinary URL for a video with the specified transformation.
     *
     * @param publicId The Cloudinary public ID of the video
     * @param transformation The transformation string
     * @return Complete Cloudinary URL
     */
    fun buildVideoUrl(publicId: String?, transformation: String?): String {
        if (publicId.isNullOrEmpty()) {
            return ""
        }

        if (transformation.isNullOrEmpty()) {
            return VIDEO_BASE_URL + publicId
        }

        return "$VIDEO_BASE_URL$transformation/$publicId"
    }

    /**
     * Builds a URL for gallery thumbnail based on attachment type.
     *
     * @param publicId The Cloudinary public ID
     * @param isVideo Whether this is a video attachment
     * @return Complete Cloudinary URL for gallery thumbnail
     */
    fun buildGalleryThumbnailUrl(publicId: String?, isVideo: Boolean): String {
        return if (isVideo) {
            buildVideoUrl(publicId, VideoTransformations.VIDEO_THUMBNAIL)
        } else {
            buildImageUrl(publicId, ImageTransformations.GALLERY_THUMBNAIL)
        }
    }

    /**
     * Builds a URL for carousel display with optimal size based on device density.
     *
     * @param publicId The Cloudinary public ID
     * @param densityDpi The device density DPI
     * @return Complete Cloudinary URL for carousel
     */
    fun buildCarouselImageUrl(publicId: String?, densityDpi: Int): String {
        val transformation = when {
            densityDpi >= 480 -> ImageTransformations.CAROUSEL_IMAGE_LARGE // xxhdpi and above
            densityDpi >= 320 -> ImageTransformations.CAROUSEL_IMAGE_MEDIUM // xhdpi and hdpi
            else -> ImageTransformations.CAROUSEL_IMAGE_SMALL // mdpi and below
        }
        return buildImageUrl(publicId, transformation)
    }

    /**
     * Builds a URL for carousel display with default medium size.
     *
     * @param publicId The Cloudinary public ID
     * @return Complete Cloudinary URL for carousel
     */
    fun buildCarouselImageUrl(publicId: String?): String {
        return buildImageUrl(publicId, ImageTransformations.CAROUSEL_IMAGE_MEDIUM)
    }

    /**
     * Builds a URL with custom dimensions for optimal performance.
     *
     * @param publicId The Cloudinary public ID
     * @param widthPx Width in pixels
     * @param heightPx Height in pixels
     * @return Complete Cloudinary URL with custom dimensions
     */
    fun buildOptimizedImageUrl(publicId: String?, widthPx: Int, heightPx: Int): String {
        val transformation = "w_$widthPx,h_$heightPx,c_fill,q_auto,f_auto"
        return buildImageUrl(publicId, transformation)
    }

    /**
     * Builds a URL for reply message preview.
     *
     * @param publicId The Cloudinary public ID
     * @return Complete Cloudinary URL for reply preview
     */
    fun buildReplyPreviewUrl(publicId: String?): String {
        return buildImageUrl(publicId, ImageTransformations.REPLY_PREVIEW)
    }
}
