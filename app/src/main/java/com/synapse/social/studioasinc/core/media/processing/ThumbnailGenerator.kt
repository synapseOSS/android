package com.synapse.social.studioasinc.core.media.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * ThumbnailGenerator creates optimized thumbnail images for media files.
 * Supports image and video thumbnail generation with efficient caching.
 */
class ThumbnailGenerator(private val context: Context) {
    
    companion object {
        private const val THUMBNAIL_SIZE = 200
        private const val THUMBNAIL_QUALITY = 85
        private const val VIDEO_FRAME_TIME_US = 1000000L // 1 second in microseconds
        private const val CACHE_DIR_NAME = "thumbnails"
        private const val MAX_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
    }
    
    // In-memory cache for recently generated thumbnails
    private val thumbnailCache = ConcurrentHashMap<String, File>()
    
    // Cache directory for persistent thumbnail storage
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Generates a thumbnail for an image file.
     * Uses center crop scaling to maintain aspect ratio within square bounds.
     * 
     * @param uri The URI of the image to generate thumbnail for
     * @return Result containing the thumbnail file or error
     */
    suspend fun generateImageThumbnail(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateCacheKey(uri, "image")
            
            // Check cache first
            getCachedThumbnail(cacheKey)?.let { cachedFile ->
                return@withContext Result.success(cachedFile)
            }
            
            // Validate URI and file accessibility
            if (!isValidMediaUri(uri)) {
                return@withContext Result.failure(IOException("Invalid or inaccessible media URI: $uri"))
            }
            
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Cannot open input stream for URI: $uri"))
            
            // Get image dimensions efficiently with corruption detection
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            val dimensionsResult = try {
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                
                val originalWidth = options.outWidth
                val originalHeight = options.outHeight
                
                // Validate image dimensions and format
                if (originalWidth <= 0 || originalHeight <= 0) {
                    return@withContext Result.failure(IOException("Corrupted image: Invalid dimensions ($originalWidth x $originalHeight)"))
                }
                
                // Check for reasonable dimensions (prevent extremely large images)
                if (originalWidth > 10000 || originalHeight > 10000) {
                    return@withContext Result.failure(IOException("Image too large: ${originalWidth}x${originalHeight}"))
                }
                
                // Validate MIME type
                val mimeType = options.outMimeType
                if (mimeType == null || !isValidImageMimeType(mimeType)) {
                    return@withContext Result.failure(IOException("Unsupported or corrupted image format: $mimeType"))
                }
                
                Pair(originalWidth, originalHeight)
            } catch (e: Exception) {
                inputStream.close()
                return@withContext Result.failure(IOException("Corrupted image file: ${e.message}", e))
            }
            
            val (originalWidth, originalHeight) = dimensionsResult
            
            // Calculate optimal sample size for thumbnail
            val sampleSize = calculateThumbnailSampleSize(originalWidth, originalHeight)
            
            // Decode with sample size for efficiency
            val newInputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Cannot reopen input stream"))
            
            val bitmap = try {
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory for thumbnails
                    inTempStorage = ByteArray(16 * 1024) // 16KB temp storage for better performance
                }
                BitmapFactory.decodeStream(newInputStream, null, decodeOptions)
            } catch (e: OutOfMemoryError) {
                // Retry with higher sample size if OOM occurs
                val higherSampleSize = sampleSize * 2
                val retryOptions = BitmapFactory.Options().apply {
                    inSampleSize = higherSampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                    inTempStorage = ByteArray(16 * 1024)
                }
                newInputStream.close()
                val retryInputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IOException("Cannot reopen input stream after OOM"))
                BitmapFactory.decodeStream(retryInputStream, null, retryOptions)
            } catch (e: Exception) {
                newInputStream.close()
                return@withContext Result.failure(IOException("Failed to decode corrupted image: ${e.message}", e))
            } finally {
                try {
                    newInputStream.close()
                } catch (e: Exception) {
                    // Ignore close errors
                }
            }
            
            if (bitmap == null) {
                return@withContext Result.failure(IOException("Failed to decode bitmap for thumbnail - possibly corrupted"))
            }
            
            // Validate decoded bitmap
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                bitmap.recycle()
                return@withContext Result.failure(IOException("Decoded bitmap is invalid or corrupted"))
            }
            
            // Create center-cropped thumbnail
            val thumbnail = createCenterCroppedThumbnail(bitmap, THUMBNAIL_SIZE)
            bitmap.recycle()
            
            // Validate thumbnail before saving
            if (thumbnail.isRecycled || thumbnail.width != THUMBNAIL_SIZE || thumbnail.height != THUMBNAIL_SIZE) {
                thumbnail.recycle()
                return@withContext Result.failure(IOException("Failed to create valid thumbnail"))
            }
            
            // Save thumbnail to cache
            val thumbnailFile = saveThumbnailToCache(thumbnail, cacheKey)
            thumbnail.recycle()
            
            // Add to in-memory cache
            thumbnailCache[cacheKey] = thumbnailFile
            
            Result.success(thumbnailFile)
            
        } catch (e: OutOfMemoryError) {
            Result.failure(IOException("Out of memory while generating image thumbnail", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generates a thumbnail for a video file using MediaMetadataRetriever.
     * Extracts a frame from the video and creates a center-cropped thumbnail.
     * 
     * @param uri The URI of the video to generate thumbnail for
     * @return Result containing the thumbnail file or error
     */
    suspend fun generateVideoThumbnail(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateCacheKey(uri, "video")
            
            // Check cache first
            getCachedThumbnail(cacheKey)?.let { cachedFile ->
                return@withContext Result.success(cachedFile)
            }
            
            // Validate URI and file accessibility
            if (!isValidMediaUri(uri)) {
                return@withContext Result.failure(IOException("Invalid or inaccessible video URI: $uri"))
            }
            
            val retriever = MediaMetadataRetriever()
            
            try {
                retriever.setDataSource(context, uri)
                
                // Validate video metadata to detect corruption
                val duration = try {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                } catch (e: Exception) {
                    null
                }
                
                val width = try {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                } catch (e: Exception) {
                    null
                }
                
                val height = try {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                } catch (e: Exception) {
                    null
                }
                
                // Validate video properties
                if (duration == null || duration <= 0) {
                    return@withContext Result.failure(IOException("Corrupted video: Invalid or missing duration"))
                }
                
                if (width == null || height == null || width <= 0 || height <= 0) {
                    return@withContext Result.failure(IOException("Corrupted video: Invalid dimensions ($width x $height)"))
                }
                
                // Check for reasonable dimensions
                if (width > 10000 || height > 10000) {
                    return@withContext Result.failure(IOException("Video resolution too large: ${width}x${height}"))
                }
                
                // Extract frame with fallback strategy for corrupted videos
                val bitmap = try {
                    // Try to extract frame at 1 second
                    retriever.getFrameAtTime(
                        VIDEO_FRAME_TIME_US,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                } catch (e: Exception) {
                    try {
                        // Fallback: try first frame
                        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } catch (e2: Exception) {
                        try {
                            // Last resort: try any frame
                            retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST)
                        } catch (e3: Exception) {
                            null
                        }
                    }
                }
                
                if (bitmap == null) {
                    return@withContext Result.failure(IOException("Failed to extract any frame from video - possibly corrupted"))
                }
                
                // Validate extracted bitmap
                if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                    bitmap.recycle()
                    return@withContext Result.failure(IOException("Extracted video frame is invalid"))
                }
                
                // Create center-cropped thumbnail
                val thumbnail = createCenterCroppedThumbnail(bitmap, THUMBNAIL_SIZE)
                bitmap.recycle()
                
                // Validate thumbnail before saving
                if (thumbnail.isRecycled || thumbnail.width != THUMBNAIL_SIZE || thumbnail.height != THUMBNAIL_SIZE) {
                    thumbnail.recycle()
                    return@withContext Result.failure(IOException("Failed to create valid video thumbnail"))
                }
                
                // Save thumbnail to cache
                val thumbnailFile = saveThumbnailToCache(thumbnail, cacheKey)
                thumbnail.recycle()
                
                // Add to in-memory cache
                thumbnailCache[cacheKey] = thumbnailFile
                
                Result.success(thumbnailFile)
                
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }
            
        } catch (e: OutOfMemoryError) {
            Result.failure(IOException("Out of memory while generating video thumbnail", e))
        } catch (e: Exception) {
            Result.failure(IOException("Failed to generate video thumbnail: ${e.message}", e))
        }
    }
    
    /**
     * Extracts a video frame at a specific timestamp.
     * Useful for generating custom thumbnails or previews.
     * 
     * @param uri The URI of the video
     * @param timeMs The timestamp in milliseconds
     * @return Result containing the extracted frame bitmap or error
     */
    suspend fun extractVideoFrame(uri: Uri, timeMs: Long): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            
            try {
                retriever.setDataSource(context, uri)
                
                val timeUs = timeMs * 1000L // Convert to microseconds
                val bitmap = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                
                if (bitmap == null) {
                    return@withContext Result.failure(IOException("Failed to extract video frame at ${timeMs}ms"))
                }
                
                Result.success(bitmap)
                
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }
            
        } catch (e: OutOfMemoryError) {
            Result.failure(IOException("Out of memory while extracting video frame", e))
        } catch (e: Exception) {
            Result.failure(IOException("Failed to extract video frame: ${e.message}", e))
        }
    }
    
    /**
     * Calculates optimal sample size for thumbnail generation.
     * Ensures efficient memory usage while maintaining quality.
     * 
     * @param width Original image width
     * @param height Original image height
     * @return Optimal sample size (power of 2)
     */
    private fun calculateThumbnailSampleSize(width: Int, height: Int): Int {
        var inSampleSize = 1
        val targetSize = THUMBNAIL_SIZE * 2 // Decode at 2x target size for better quality
        
        if (width > targetSize || height > targetSize) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            
            // Calculate the largest inSampleSize value that keeps dimensions above target
            while ((halfWidth / inSampleSize) >= targetSize && (halfHeight / inSampleSize) >= targetSize) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Creates a center-cropped square thumbnail from a bitmap.
     * Maintains aspect ratio by cropping to fit square dimensions.
     * 
     * @param source The source bitmap
     * @param size The target thumbnail size (width and height)
     * @return Center-cropped thumbnail bitmap
     */
    private fun createCenterCroppedThumbnail(source: Bitmap, size: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height
        
        // Calculate crop dimensions to maintain aspect ratio
        val cropSize = minOf(sourceWidth, sourceHeight)
        val cropX = (sourceWidth - cropSize) / 2
        val cropY = (sourceHeight - cropSize) / 2
        
        // Create cropped bitmap
        val croppedBitmap = try {
            Bitmap.createBitmap(source, cropX, cropY, cropSize, cropSize)
        } catch (e: OutOfMemoryError) {
            // If cropping fails, scale the original bitmap directly
            return Bitmap.createScaledBitmap(source, size, size, true)
        }
        
        // Scale to target size
        val thumbnail = if (cropSize != size) {
            try {
                val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, size, size, true)
                if (scaledBitmap != croppedBitmap) {
                    croppedBitmap.recycle()
                }
                scaledBitmap
            } catch (e: OutOfMemoryError) {
                croppedBitmap // Return cropped bitmap if scaling fails
            }
        } else {
            croppedBitmap
        }
        
        return thumbnail
    }
    
    /**
     * Saves a thumbnail bitmap to the cache directory.
     * 
     * @param thumbnail The thumbnail bitmap to save
     * @param cacheKey The cache key for the thumbnail
     * @return The saved thumbnail file
     */
    private suspend fun saveThumbnailToCache(thumbnail: Bitmap, cacheKey: String): File = withContext(Dispatchers.IO) {
        val thumbnailFile = File(cacheDir, "$cacheKey.jpg")
        
        try {
            FileOutputStream(thumbnailFile).use { outputStream ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, outputStream)
            }
        } catch (e: IOException) {
            // Clean up file if write fails
            if (thumbnailFile.exists()) {
                thumbnailFile.delete()
            }
            throw e
        }
        
        thumbnailFile
    }
    
    /**
     * Generates a cache key for a media URI and type.
     * 
     * @param uri The media URI
     * @param type The media type (image, video)
     * @return A unique cache key
     */
    private fun generateCacheKey(uri: Uri, type: String): String {
        return "${type}_${uri.toString().hashCode()}_${THUMBNAIL_SIZE}"
    }
    
    /**
     * Retrieves a cached thumbnail if it exists and is valid.
     * 
     * @param cacheKey The cache key
     * @return The cached thumbnail file or null if not found/invalid
     */
    private fun getCachedThumbnail(cacheKey: String): File? {
        // Check in-memory cache first
        thumbnailCache[cacheKey]?.let { cachedFile ->
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return cachedFile
            } else {
                // Remove invalid entry from cache
                thumbnailCache.remove(cacheKey)
            }
        }
        
        // Check disk cache
        val thumbnailFile = File(cacheDir, "$cacheKey.jpg")
        if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
            // Add to in-memory cache for faster access
            thumbnailCache[cacheKey] = thumbnailFile
            return thumbnailFile
        }
        
        return null
    }
    
    /**
     * Clears the thumbnail cache to free up storage space.
     * Removes both in-memory and disk cache entries.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            // Clear in-memory cache
            thumbnailCache.clear()
            
            // Clear disk cache
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        // Ignore individual file deletion errors
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cache clearing errors
        }
    }
    
    /**
     * Performs cache maintenance by removing old or excess files.
     * Should be called periodically to manage cache size.
     */
    suspend fun performCacheMaintenance() = withContext(Dispatchers.IO) {
        try {
            if (!cacheDir.exists()) return@withContext
            
            val files = cacheDir.listFiles() ?: return@withContext
            val totalSize = files.sumOf { it.length() }
            
            // If cache size exceeds limit, remove oldest files
            if (totalSize > MAX_CACHE_SIZE) {
                val sortedFiles = files.sortedBy { it.lastModified() }
                var currentSize = totalSize
                
                for (file in sortedFiles) {
                    if (currentSize <= MAX_CACHE_SIZE * 0.8) break // Keep 80% of max size
                    
                    try {
                        currentSize -= file.length()
                        file.delete()
                        
                        // Remove from in-memory cache if present
                        val fileName = file.nameWithoutExtension
                        thumbnailCache.remove(fileName)
                    } catch (e: Exception) {
                        // Ignore individual file deletion errors
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cache maintenance errors
        }
    }
    
    /**
     * Validates if a media URI is accessible and valid.
     * Helps detect corrupted or inaccessible files early.
     * 
     * @param uri The URI to validate
     * @return true if URI is valid and accessible, false otherwise
     */
    private fun isValidMediaUri(uri: Uri): Boolean {
        return try {
            when (uri.scheme) {
                "content" -> {
                    // Check if content resolver can access the URI
                    context.contentResolver.openInputStream(uri)?.use { 
                        it.read() // Try to read at least one byte
                    }
                    true
                }
                "file" -> {
                    // Check if file exists and is readable
                    val file = File(uri.path ?: return false)
                    file.exists() && file.canRead() && file.length() > 0
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates if an image MIME type is supported.
     * Helps filter out unsupported or corrupted image formats.
     * 
     * @param mimeType The MIME type to validate
     * @return true if MIME type is supported, false otherwise
     */
    private fun isValidImageMimeType(mimeType: String): Boolean {
        return when (mimeType.lowercase()) {
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp" -> true
            else -> false
        }
    }
}
