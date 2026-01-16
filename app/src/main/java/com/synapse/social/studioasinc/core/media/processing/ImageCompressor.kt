package com.synapse.social.studioasinc.core.media.processing

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * ImageCompressor handles image compression with size and quality optimization.
 * Maintains aspect ratio while reducing file size to meet target requirements.
 */
class ImageCompressor(private val context: Context) {
    
    companion object {
        private const val MAX_WIDTH = 1920
        private const val MAX_HEIGHT = 1080
        private const val MAX_FILE_SIZE_MB = 2
        private const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024L // 2MB
        private const val DEFAULT_COMPRESSION_QUALITY = 85
        private const val MIN_COMPRESSION_QUALITY = 50
        private const val QUALITY_STEP = 5
    }
    
    /**
     * Compresses an image from URI to meet size and quality requirements.
     * 
     * @param uri The URI of the image to compress
     * @return Result containing the compressed image file or error
     */
    suspend fun compress(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Decode the image
            val decodedBitmap = decodeImage(uri, MAX_WIDTH, MAX_HEIGHT)
                ?: return@withContext Result.failure(IOException("Failed to decode bitmap from $uri"))
            
            // Apply EXIF orientation
            // ImageDecoder (API 28+) handles orientation automatically during decode
            val orientedBitmap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                try {
                    applyExifOrientation(uri, decodedBitmap)
                } catch (e: Exception) {
                    // If EXIF processing fails, use original bitmap
                    decodedBitmap
                }
            } else {
                decodedBitmap
            }
            
            // Scale bitmap to target dimensions if needed
            val scaledBitmap = scaleToTargetSize(orientedBitmap, MAX_WIDTH, MAX_HEIGHT)
            
            // Clean up intermediate bitmap if different
            if (scaledBitmap != orientedBitmap && scaledBitmap != decodedBitmap) {
                orientedBitmap.recycle()
            }
            if (orientedBitmap != decodedBitmap) {
                decodedBitmap.recycle()
            }
            
            // Validate bitmap safety before compression
            if (!isBitmapSafeToProcess(scaledBitmap)) {
                scaledBitmap.recycle()
                return@withContext Result.failure(IOException("Bitmap too large to process safely"))
            }
            
            // Compress iteratively to meet file size target
            val compressedFile = compressIteratively(scaledBitmap, MAX_FILE_SIZE_BYTES)
            
            // Clean up bitmap
            scaledBitmap.recycle()
            
            Result.success(compressedFile)
            
        } catch (e: OutOfMemoryError) {
            Result.failure(IOException("Out of memory while compressing image", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Compresses an image to a specific target size.
     * 
     * @param uri The URI of the image to compress
     * @param maxSizeBytes Maximum file size in bytes
     * @return Result containing the compressed image file or error
     */
    suspend fun compressToSize(uri: Uri, maxSizeBytes: Long): Result<File> = withContext(Dispatchers.IO) {
        try {
            val decodedBitmap = decodeImage(uri, MAX_WIDTH, MAX_HEIGHT)
                ?: return@withContext Result.failure(IOException("Failed to decode bitmap from $uri"))
            
            val orientedBitmap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                try {
                    applyExifOrientation(uri, decodedBitmap)
                } catch (e: Exception) {
                    decodedBitmap
                }
            } else {
                decodedBitmap
            }
            
            val scaledBitmap = scaleToTargetSize(orientedBitmap, MAX_WIDTH, MAX_HEIGHT)
            
            if (scaledBitmap != orientedBitmap && scaledBitmap != decodedBitmap) {
                orientedBitmap.recycle()
            }
            if (orientedBitmap != decodedBitmap) {
                decodedBitmap.recycle()
            }
            
            // Validate bitmap safety before compression
            if (!isBitmapSafeToProcess(scaledBitmap)) {
                scaledBitmap.recycle()
                return@withContext Result.failure(IOException("Bitmap too large to process safely"))
            }
            
            val compressedFile = compressIteratively(scaledBitmap, maxSizeBytes)
            scaledBitmap.recycle()
            
            Result.success(compressedFile)
            
        } catch (e: OutOfMemoryError) {
            Result.failure(IOException("Out of memory while compressing image", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decodes an image from a URI, handling API level differences and large images.
     */
    private fun decodeImage(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeImageApi28(uri, reqWidth, reqHeight)
        } else {
            decodeImageLegacy(uri, reqWidth, reqHeight)
        }
    }

    /**
     * Decodes image using ImageDecoder (API 28+).
     */
    private fun decodeImageApi28(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, source ->
                val size = info.size
                var sampleSize = 1
                if (size.width > reqWidth || size.height > reqHeight) {
                    val halfWidth = size.width / 2
                    val halfHeight = size.height / 2
                    while ((halfWidth / sampleSize) >= reqWidth && (halfHeight / sampleSize) >= reqHeight) {
                        sampleSize *= 2
                    }
                }

                // For very large images (>4k), ensure we don't start with too little downsampling
                var effectiveWidth = size.width / sampleSize
                var effectiveHeight = size.height / sampleSize

                while (effectiveWidth > 4096 || effectiveHeight > 4096) {
                     sampleSize *= 2
                     effectiveWidth = size.width / sampleSize
                     effectiveHeight = size.height / sampleSize
                }

                decoder.setTargetSampleSize(sampleSize)

                // Check if image has transparency to decide config
                val mimeType = info.mimeType
                if (mimeType == "image/png" || mimeType == "image/webp") {
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // Or ALLOCATOR_DEFAULT which might pick hardware
                    // ImageDecoder handles alpha automatically
                } else {
                    decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }

                // Handle Mutable requirement if needed, but here we just need a bitmap to process
                decoder.isMutableRequired = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes image using BitmapFactory (Legacy).
     */
    private fun decodeImageLegacy(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
                ?: return null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var sampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // Retry logic for OOM
            while (true) {
                val newInputStream = context.contentResolver.openInputStream(uri)
                    ?: return null

                try {
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        // Check mime type for transparency
                        val mimeType = options.outMimeType
                        if (mimeType == "image/png" || mimeType == "image/webp") {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        } else {
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                    }

                    val bitmap = BitmapFactory.decodeStream(newInputStream, null, decodeOptions)
                    newInputStream.close()
                    return bitmap
                } catch (e: OutOfMemoryError) {
                    newInputStream.close()
                    sampleSize *= 2
                    if (sampleSize > 64) { // Safety break
                        return null
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        } finally {
            inputStream?.close()
        }
    }
    
    /**
     * Calculates the optimal inSampleSize for efficient bitmap decoding.
     * 
     * @param options BitmapFactory.Options containing image dimensions
     * @param reqWidth Required width
     * @param reqHeight Required height
     * @return Optimal sample size (power of 2)
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        // Additional check for extremely large images to avoid OOM
        // If the resulting bitmap would still be very large (e.g. > 4000x4000 effectively), bump sample size
        // Use a loop to ensure it is reduced enough
        var effectiveWidth = width / inSampleSize
        var effectiveHeight = height / inSampleSize

        while (effectiveWidth > 4096 || effectiveHeight > 4096) {
             inSampleSize *= 2
             effectiveWidth = width / inSampleSize
             effectiveHeight = height / inSampleSize
        }
        
        return inSampleSize
    }
    
    /**
     * Applies EXIF orientation to the bitmap to ensure correct display.
     * 
     * @param uri The URI of the original image
     * @param bitmap The bitmap to orient
     * @return Oriented bitmap
     */
    private suspend fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext bitmap
            
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(-90f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return@withContext bitmap
            }
            
            try {
                val orientedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                if (orientedBitmap != bitmap) {
                    bitmap.recycle()
                }
                orientedBitmap
            } catch (e: OutOfMemoryError) {
                // If we can't create the oriented bitmap, return the original
                bitmap
            }
        } catch (e: Exception) {
            // If EXIF processing fails, return original bitmap
            bitmap
        }
    }
    
    /**
     * Scales bitmap to fit within target dimensions while maintaining aspect ratio.
     * 
     * @param bitmap The bitmap to scale
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return Scaled bitmap
     */
    private fun scaleToTargetSize(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val aspectRatio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        
        if (width > height) {
            targetWidth = maxWidth
            targetHeight = (maxWidth / aspectRatio).toInt()
        } else {
            targetHeight = maxHeight
            targetWidth = (maxHeight * aspectRatio).toInt()
        }
        
        return try {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            if (scaledBitmap != bitmap) {
                bitmap.recycle()
            }
            scaledBitmap
        } catch (e: OutOfMemoryError) {
            // If scaling fails due to memory, return original bitmap
            bitmap
        }
    }
    
    /**
     * Compresses bitmap iteratively to meet target file size.
     * Uses binary search for optimal quality reduction.
     * 
     * @param bitmap The bitmap to compress
     * @param targetSizeBytes Target file size in bytes
     * @return Compressed image file
     */
    private suspend fun compressIteratively(bitmap: Bitmap, targetSizeBytes: Long): File = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("compressed_image_", ".jpg", context.cacheDir)
        
        var minQuality = MIN_COMPRESSION_QUALITY
        var maxQuality = 95 // Start with a reasonable max quality
        var bestQuality = minQuality
        var bestData: ByteArray? = null

        // Initial check with max quality
        val initialStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, maxQuality, initialStream)
        val initialData = initialStream.toByteArray()
        initialStream.close()
        
        if (initialData.size.toLong() <= targetSizeBytes) {
            bestData = initialData
        } else {
            // Binary search for the best quality
            var attempts = 0
            val maxAttempts = 10
            
            while (minQuality <= maxQuality && attempts < maxAttempts) {
                val midQuality = (minQuality + maxQuality) / 2
                val outputStream = ByteArrayOutputStream()

                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, midQuality, outputStream)
                    val compressedData = outputStream.toByteArray()
                    val currentSize = compressedData.size.toLong()

                    if (currentSize <= targetSizeBytes) {
                        bestQuality = midQuality
                        bestData = compressedData
                        minQuality = midQuality + 1 // Try to get better quality
                    } else {
                        maxQuality = midQuality - 1 // Needs more compression
                    }
                } catch (e: OutOfMemoryError) {
                    // If OOM, treat as file too big
                    maxQuality = midQuality - 1
                } finally {
                    outputStream.close()
                }
                attempts++
            }
        }
        
        // If we found a suitable quality
        if (bestData != null) {
             try {
                FileOutputStream(tempFile).use { fileOutputStream ->
                    fileOutputStream.write(bestData)
                }
            } catch (e: IOException) {
                tempFile.delete()
                throw e
            }
            return@withContext tempFile
        }

        // Fallback: if even min quality didn't work (which shouldn't happen with correct binary search initialization but to be safe)
        // or if we never found a size < target (bestData is null)
        // We just compress with MIN_COMPRESSION_QUALITY and return whatever we got.

        val fallbackStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, MIN_COMPRESSION_QUALITY, fallbackStream)
        val fallbackData = fallbackStream.toByteArray()
        fallbackStream.close()
        
        try {
            FileOutputStream(tempFile).use { fileOutputStream ->
                fileOutputStream.write(fallbackData)
            }
        } catch (e: IOException) {
            tempFile.delete()
            throw e
        }
        
        tempFile
    }
    
    /**
     * Calculates optimal compression quality based on bitmap size and target file size.
     * Uses advanced estimation considering JPEG compression characteristics.
     * 
     * @param bitmap The bitmap to analyze
     * @param targetSizeBytes Target file size in bytes
     * @return Optimal quality percentage
     */
    private fun calculateOptimalQuality(bitmap: Bitmap, targetSizeBytes: Long): Int {
        // Estimate compressed size based on image complexity and target
        val pixelCount = bitmap.width * bitmap.height
        val estimatedBytesPerPixel = when {
            pixelCount > 2_000_000 -> 0.8f  // Large images compress better
            pixelCount > 1_000_000 -> 1.0f  // Medium images
            else -> 1.2f                    // Small images have overhead
        }
        
        val estimatedCompressedSize = (pixelCount * estimatedBytesPerPixel).toLong()
        val compressionRatio = targetSizeBytes.toFloat() / estimatedCompressedSize
        
        return when {
            compressionRatio >= 1.0f -> 95  // No compression needed
            compressionRatio >= 0.7f -> DEFAULT_COMPRESSION_QUALITY  // Light compression
            compressionRatio >= 0.4f -> 75  // Medium compression
            compressionRatio >= 0.2f -> 65  // Heavy compression
            compressionRatio >= 0.1f -> 55  // Very heavy compression
            else -> MIN_COMPRESSION_QUALITY   // Maximum compression
        }.coerceIn(MIN_COMPRESSION_QUALITY, 95)
    }
    
    /**
     * Estimates the final file size for a given quality setting.
     * Used for optimization and progress estimation.
     * 
     * @param bitmap The bitmap to analyze
     * @param quality JPEG quality (0-100)
     * @return Estimated file size in bytes
     */
    private fun estimateCompressedSize(bitmap: Bitmap, quality: Int): Long {
        val pixelCount = bitmap.width * bitmap.height
        val qualityFactor = quality / 100f
        
        // Base compression ratio varies with quality
        val baseCompressionRatio = when {
            quality >= 90 -> 0.15f
            quality >= 80 -> 0.10f
            quality >= 70 -> 0.08f
            quality >= 60 -> 0.06f
            quality >= 50 -> 0.05f
            else -> 0.04f
        }
        
        return (pixelCount * baseCompressionRatio * qualityFactor).toLong()
    }
    
    /**
     * Validates if the bitmap can be safely processed without causing OOM.
     * 
     * @param bitmap The bitmap to validate
     * @return true if safe to process, false otherwise
     */
    private fun isBitmapSafeToProcess(bitmap: Bitmap): Boolean {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val availableMemory = maxMemory - usedMemory
        
        // Estimate memory needed for bitmap operations (original + compressed + temp)
        val bitmapMemory = bitmap.byteCount * 3L
        
        return bitmapMemory < availableMemory * 0.5 // Use only 50% of available memory
    }
}
