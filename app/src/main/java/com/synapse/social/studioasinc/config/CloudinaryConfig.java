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

package com.synapse.social.studioasinc.config;

/**
 * Configuration constants for Cloudinary image/video service.
 * Centralizes all cloud-related URLs and parameters for easy maintenance.
 */
public class CloudinaryConfig {
    
    // Base Cloudinary configuration
    public static final String CLOUD_NAME = "demo"; // TODO: Replace with actual cloud name
    public static final String BASE_URL = "https://res.cloudinary.com/" + CLOUD_NAME + "/image/upload/";
    public static final String VIDEO_BASE_URL = "https://res.cloudinary.com/" + CLOUD_NAME + "/video/upload/";
    
    // Common image transformations
    public static final class ImageTransformations {
        // Gallery thumbnails (optimized for actual view size)
        public static final String GALLERY_THUMBNAIL = "w_400,h_400,c_fill,q_auto,f_auto";
        
        // Reply message preview (120dp = ~120px on mdpi)
        public static final String REPLY_PREVIEW = "w_120,h_120,c_fill,q_auto,f_auto";
        
        // Carousel images (200dp = ~200px on mdpi, but we'll optimize for density)
        public static final String CAROUSEL_IMAGE_SMALL = "w_200,h_200,c_fill,q_auto,f_auto";
        public static final String CAROUSEL_IMAGE_MEDIUM = "w_400,h_400,c_fill,q_auto,f_auto";
        public static final String CAROUSEL_IMAGE_LARGE = "w_600,h_600,c_fill,q_auto,f_auto";
        
        // Full resolution for gallery
        public static final String FULL_RESOLUTION = "q_auto,f_auto";
        
        // Profile pictures
        public static final String PROFILE_SMALL = "w_100,h_100,c_fill,g_face,q_auto,f_auto";
        public static final String PROFILE_MEDIUM = "w_200,h_200,c_fill,g_face,q_auto,f_auto";
        
        // Post images
        public static final String POST_THUMBNAIL = "w_300,h_300,c_fill,q_auto,f_auto";
        public static final String POST_FULL = "w_800,h_800,c_limit,q_auto,f_auto";
    }
    
    // Video transformations
    public static final class VideoTransformations {
        public static final String VIDEO_THUMBNAIL = "w_400,h_400,c_fill,so_auto";
        public static final String VIDEO_PREVIEW = "w_200,h_200,c_fill,so_auto";
    }
    
    /**
     * Builds a complete Cloudinary URL for an image with the specified transformation.
     * 
     * @param publicId The Cloudinary public ID of the image
     * @param transformation The transformation string (e.g., "w_400,h_400,c_fill")
     * @return Complete Cloudinary URL
     */
    public static String buildImageUrl(String publicId, String transformation) {
        if (publicId == null || publicId.isEmpty()) {
            return "";
        }
        
        if (transformation == null || transformation.isEmpty()) {
            return BASE_URL + publicId;
        }
        
        return BASE_URL + transformation + "/" + publicId;
    }
    
    /**
     * Builds a complete Cloudinary URL for a video with the specified transformation.
     * 
     * @param publicId The Cloudinary public ID of the video
     * @param transformation The transformation string
     * @return Complete Cloudinary URL
     */
    public static String buildVideoUrl(String publicId, String transformation) {
        if (publicId == null || publicId.isEmpty()) {
            return "";
        }
        
        if (transformation == null || transformation.isEmpty()) {
            return VIDEO_BASE_URL + publicId;
        }
        
        return VIDEO_BASE_URL + transformation + "/" + publicId;
    }
    
    /**
     * Builds a URL for gallery thumbnail based on attachment type.
     * 
     * @param publicId The Cloudinary public ID
     * @param isVideo Whether this is a video attachment
     * @return Complete Cloudinary URL for gallery thumbnail
     */
    public static String buildGalleryThumbnailUrl(String publicId, boolean isVideo) {
        if (isVideo) {
            return buildVideoUrl(publicId, VideoTransformations.VIDEO_THUMBNAIL);
        } else {
            return buildImageUrl(publicId, ImageTransformations.GALLERY_THUMBNAIL);
        }
    }
    
    /**
     * Builds a URL for carousel display with optimal size based on device density.
     * 
     * @param publicId The Cloudinary public ID
     * @param densityDpi The device density DPI
     * @return Complete Cloudinary URL for carousel
     */
    public static String buildCarouselImageUrl(String publicId, int densityDpi) {
        String transformation;
        if (densityDpi >= 480) { // xxhdpi and above
            transformation = ImageTransformations.CAROUSEL_IMAGE_LARGE;
        } else if (densityDpi >= 320) { // xhdpi and hdpi
            transformation = ImageTransformations.CAROUSEL_IMAGE_MEDIUM;
        } else { // mdpi and below
            transformation = ImageTransformations.CAROUSEL_IMAGE_SMALL;
        }
        return buildImageUrl(publicId, transformation);
    }
    
    /**
     * Builds a URL for carousel display with default medium size.
     * 
     * @param publicId The Cloudinary public ID
     * @return Complete Cloudinary URL for carousel
     */
    public static String buildCarouselImageUrl(String publicId) {
        return buildImageUrl(publicId, ImageTransformations.CAROUSEL_IMAGE_MEDIUM);
    }
    
    /**
     * Builds a URL with custom dimensions for optimal performance.
     * 
     * @param publicId The Cloudinary public ID
     * @param widthPx Width in pixels
     * @param heightPx Height in pixels
     * @return Complete Cloudinary URL with custom dimensions
     */
    public static String buildOptimizedImageUrl(String publicId, int widthPx, int heightPx) {
        String transformation = String.format("w_%d,h_%d,c_fill,q_auto,f_auto", widthPx, heightPx);
        return buildImageUrl(publicId, transformation);
    }
    
    /**
     * Builds a URL for reply message preview.
     * 
     * @param publicId The Cloudinary public ID
     * @return Complete Cloudinary URL for reply preview
     */
    public static String buildReplyPreviewUrl(String publicId) {
        return buildImageUrl(publicId, ImageTransformations.REPLY_PREVIEW);
    }
}