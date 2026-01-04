package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

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
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Cannot open input stream for URI: $uri"))
            
            // Get original image dimensions and EXIF data
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            
            if (originalWidth <= 0 || originalHeight <= 0) {
                return@withContext Result.failure(IOException("Invalid image dimensions"))
            }
            
            // Calculate optimal sample size
            val sampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT)
            
            // Decode the image with sample size
            val newInputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Cannot reopen input stream for URI: $uri"))
            
            val decodedBitmap = try {
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
                }
                BitmapFactory.decodeStream(newInputStream, null, decodeOptions)
            } catch (e: OutOfMemoryError) {
                // Retry with higher sample size if OOM occurs
                val higherSampleSize = sampleSize * 2
                val retryOptions = BitmapFactory.Options().apply {
                    inSampleSize = higherSampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                newInputStream.close()
                val retryInputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IOException("Cannot reopen input stream after OOM"))
                BitmapFactory.decodeStream(retryInputStream, null, retryOptions)
            } finally {
                newInputStream.close()
            }
            
            if (decodedBitmap == null) {
                return@withContext Result.failure(IOException("Failed to decode bitmap"))
            }
            
            // Apply EXIF orientation
            val orientedBitmap = try {
                applyExifOrientation(uri, decodedBitmap)
            } catch (e: Exception) {
                // If EXIF processing fails, use original bitmap
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
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Cannot open input stream for URI: $uri"))
            
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            val sampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT)
            
            val newInputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Cannot reopen input stream for URI: $uri"))
            
            val decodedBitmap = try {
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeStream(newInputStream, null, decodeOptions)
            } catch (e: OutOfMemoryError) {
                val higherSampleSize = sampleSize * 2
                val retryOptions = BitmapFactory.Options().apply {
                    inSampleSize = higherSampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                newInputStream.close()
                val retryInputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IOException("Cannot reopen input stream after OOM"))
                BitmapFactory.decodeStream(retryInputStream, null, retryOptions)
            } finally {
                newInputStream.close()
            }
            
            if (decodedBitmap == null) {
                return@withContext Result.failure(IOException("Failed to decode bitmap"))
            }
            
            val orientedBitmap = try {
                applyExifOrientation(uri, decodedBitmap)
            } catch (e: Exception) {
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
     * Uses adaptive quality reduction and size estimation for optimal results.
     * 
     * @param bitmap The bitmap to compress
     * @param targetSizeBytes Target file size in bytes
     * @return Compressed image file
     */
    private suspend fun compressIteratively(bitmap: Bitmap, targetSizeBytes: Long): File = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("compressed_image_", ".jpg", context.cacheDir)
        
        var quality = calculateOptimalQuality(bitmap, targetSizeBytes)
        var compressedData: ByteArray? = null
        var previousSize = Long.MAX_VALUE
        var attempts = 0
        val maxAttempts = 10 // Prevent infinite loops
        
        do {
            val outputStream = ByteArrayOutputStream()
            
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                compressedData = outputStream.toByteArray()
            } catch (e: OutOfMemoryError) {
                // If compression fails due to memory, try with lower quality
                quality = maxOf(MIN_COMPRESSION_QUALITY, quality - (QUALITY_STEP * 2))
                if (quality <= MIN_COMPRESSION_QUALITY) {
                    // Last resort: use minimum quality
                    bitmap.compress(Bitmap.CompressFormat.JPEG, MIN_COMPRESSION_QUALITY, outputStream)
                    compressedData = outputStream.toByteArray()
                } else {
                    outputStream.close()
                    continue
                }
            } finally {
                outputStream.close()
            }
            
            // If we still don't have compressed data, break to avoid null pointer
            if (compressedData == null) {
                break
            }
            
            val currentSize = compressedData.size.toLong()
            
            // Check if we've reached target size or minimum quality
            if (currentSize <= targetSizeBytes || quality <= MIN_COMPRESSION_QUALITY) {
                break
            }
            
            // Adaptive quality reduction based on size ratio
            val sizeRatio = currentSize.toFloat() / targetSizeBytes
            val qualityReduction = when {
                sizeRatio > 2.0f -> QUALITY_STEP * 2  // Large reduction for very oversized files
                sizeRatio > 1.5f -> QUALITY_STEP + 2  // Medium reduction
                else -> QUALITY_STEP                   // Standard reduction
            }
            
            quality -= qualityReduction
            
            // Prevent infinite loops if size isn't decreasing
            if (currentSize >= previousSize) {
                quality -= QUALITY_STEP
            }
            previousSize = currentSize
            attempts++
            
        } while (quality > MIN_COMPRESSION_QUALITY && attempts < maxAttempts)
        
        // Ensure we have compressed data before writing
        if (compressedData == null) {
            // Fallback: compress with minimum quality
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, MIN_COMPRESSION_QUALITY, outputStream)
            compressedData = outputStream.toByteArray()
            outputStream.close()
        }
        
        // Write final compressed data to file
        try {
            FileOutputStream(tempFile).use { fileOutputStream ->
                fileOutputStream.write(compressedData)
            }
        } catch (e: IOException) {
            // Clean up temp file if write fails
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
