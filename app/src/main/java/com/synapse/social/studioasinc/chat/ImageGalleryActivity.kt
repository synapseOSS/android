package com.synapse.social.studioasinc.chat

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
// import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.github.chrisbanes.photoview.PhotoView
import com.synapse.social.studioasinc.BaseActivity
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.backend.SupabaseStorageService
import com.synapse.social.studioasinc.chat.service.MediaDownloadManager
import com.synapse.social.studioasinc.databinding.ActivityImageGalleryBinding
import com.synapse.social.studioasinc.databinding.ItemGalleryImageBinding
import com.synapse.social.studioasinc.util.MediaCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * ImageGalleryActivity
 * Full-screen image viewer with pinch-to-zoom, swipe navigation, and download functionality.
 * 
 * Features:
 * - ViewPager2 for smooth image swiping
 * - PhotoView for pinch-to-zoom (max 3x)
 * - Image metadata display (name, size, dimensions)
 * - Download to device storage
 * - Progressive image loading (thumbnail → full resolution)
 * - Preloading of adjacent images
 */
class ImageGalleryActivity : BaseActivity() {
    
    companion object {
        private const val TAG = "ImageGalleryActivity"
        private const val EXTRA_IMAGE_URLS = "extra_image_urls"
        private const val EXTRA_THUMBNAIL_URLS = "extra_thumbnail_urls"
        private const val EXTRA_IMAGE_NAMES = "extra_image_names"
        private const val EXTRA_IMAGE_SIZES = "extra_image_sizes"
        private const val EXTRA_IMAGE_DIMENSIONS = "extra_image_dimensions"
        private const val EXTRA_INITIAL_POSITION = "extra_initial_position"
        private const val REQUEST_WRITE_STORAGE = 1001
        
        /**
         * Create intent to launch ImageGalleryActivity.
         * 
         * @param context Context
         * @param imageUrls List of full-resolution image URLs
         * @param thumbnailUrls List of thumbnail URLs (optional)
         * @param imageNames List of image file names (optional)
         * @param imageSizes List of image file sizes in bytes (optional)
         * @param imageDimensions List of image dimensions as "widthxheight" (optional)
         * @param initialPosition Initial position to display
         */
        fun createIntent(
            context: Context,
            imageUrls: List<String>,
            thumbnailUrls: List<String>? = null,
            imageNames: List<String>? = null,
            imageSizes: List<Long>? = null,
            imageDimensions: List<String>? = null,
            initialPosition: Int = 0
        ): Intent {
            return Intent(context, ImageGalleryActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_IMAGE_URLS, ArrayList(imageUrls))
                thumbnailUrls?.let { putStringArrayListExtra(EXTRA_THUMBNAIL_URLS, ArrayList(it)) }
                imageNames?.let { putStringArrayListExtra(EXTRA_IMAGE_NAMES, ArrayList(it)) }
                imageSizes?.let { putExtra(EXTRA_IMAGE_SIZES, it.toLongArray()) }
                imageDimensions?.let { putStringArrayListExtra(EXTRA_IMAGE_DIMENSIONS, ArrayList(it)) }
                putExtra(EXTRA_INITIAL_POSITION, initialPosition)
            }
        }
    }
    
    private lateinit var binding: ActivityImageGalleryBinding
    private lateinit var imageUrls: List<String>
    private var thumbnailUrls: List<String>? = null
    private var imageNames: List<String>? = null
    private var imageSizes: List<Long>? = null
    private var imageDimensions: List<String>? = null
    private var currentPosition: Int = 0
    
    private lateinit var mediaDownloadManager: MediaDownloadManager
    private var pendingDownloadPosition: Int? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Extract intent extras
        imageUrls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS) ?: emptyList()
        thumbnailUrls = intent.getStringArrayListExtra(EXTRA_THUMBNAIL_URLS)
        imageNames = intent.getStringArrayListExtra(EXTRA_IMAGE_NAMES)
        imageSizes = intent.getLongArrayExtra(EXTRA_IMAGE_SIZES)?.toList()
        imageDimensions = intent.getStringArrayListExtra(EXTRA_IMAGE_DIMENSIONS)
        currentPosition = intent.getIntExtra(EXTRA_INITIAL_POSITION, 0)
        
        if (imageUrls.isEmpty()) {
            Log.e(TAG, "No image URLs provided")
            finish()
            return
        }
        
        // Initialize MediaDownloadManager
        val storageService = SupabaseStorageService()
        val mediaCache = MediaCache(this)
        mediaDownloadManager = MediaDownloadManager(this, storageService, mediaCache, lifecycleScope)
        
        setupToolbar()
        setupViewPager()
        setupDownloadButton()
        
        // Update metadata for initial position
        updateImageMetadata(currentPosition)
        
        // Preload adjacent images
        preloadAdjacentImages(currentPosition)
    }
    
    /**
     * Setup toolbar with close button and metadata display.
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    /**
     * Setup ViewPager2 with image adapter and page change listener.
     */
    private fun setupViewPager() {
        val adapter = ImageGalleryAdapter(
            imageUrls = imageUrls,
            thumbnailUrls = thumbnailUrls,
            mediaDownloadManager = mediaDownloadManager
        )
        
        binding.viewPagerImages.adapter = adapter
        binding.viewPagerImages.setCurrentItem(currentPosition, false)
        
        // Listen for page changes
        binding.viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                updateImageMetadata(position)
                preloadAdjacentImages(position)
            }
        })
    }
    
    /**
     * Setup download button click listener.
     */
    private fun setupDownloadButton() {
        binding.fabDownload.setOnClickListener {
            downloadCurrentImage()
        }
    }
    
    /**
     * Update image metadata display (name, size, dimensions).
     */
    private fun updateImageMetadata(position: Int) {
        // Update image name
        val imageName = imageNames?.getOrNull(position) ?: "Image ${position + 1}"
        binding.textImageName.text = imageName
        
        // Update image size and dimensions
        val sizeText = imageSizes?.getOrNull(position)?.let { formatFileSize(it) }
        val dimensionsText = imageDimensions?.getOrNull(position)
        
        val metadataText = when {
            sizeText != null && dimensionsText != null -> "$sizeText • $dimensionsText"
            sizeText != null -> sizeText
            dimensionsText != null -> dimensionsText
            else -> "${position + 1} / ${imageUrls.size}"
        }
        
        binding.textImageSize.text = metadataText
    }
    
    /**
     * Preload adjacent images for smooth swiping.
     * Preloads the next 3 images from current position.
     */
    private fun preloadAdjacentImages(position: Int) {
        mediaDownloadManager.preloadGalleryImages(imageUrls, position)
    }
    
    /**
     * Download current image to device storage.
     */
    private fun downloadCurrentImage() {
        // Check storage permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingDownloadPosition = currentPosition
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_STORAGE
                )
                return
            }
        }
        
        performDownload(currentPosition)
    }
    
    /**
     * Perform the actual download operation.
     */
    private fun performDownload(position: Int) {
        val imageUrl = imageUrls.getOrNull(position) ?: return
        val imageName = imageNames?.getOrNull(position) ?: "image_${System.currentTimeMillis()}.jpg"
        
        // Show loading
        showLoading(true, getString(R.string.downloading_image))
        
        lifecycleScope.launch {
            try {
                // Download image using Glide to get bitmap
                val bitmap = withContext(Dispatchers.IO) {
                    Glide.with(this@ImageGalleryActivity)
                        .asBitmap()
                        .load(imageUrl)
                        .submit()
                        .get()
                }
                
                // Save to gallery
                val saved = saveImageToGallery(bitmap, imageName)
                
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (saved) {
                        Toast.makeText(
                            this@ImageGalleryActivity,
                            getString(R.string.image_downloaded),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ImageGalleryActivity,
                            getString(R.string.failed_to_download_image),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download image", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@ImageGalleryActivity,
                        getString(R.string.failed_to_download_image),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    /**
     * Save bitmap to device gallery.
     */
    private suspend fun saveImageToGallery(bitmap: Bitmap, fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val outputStream: OutputStream?
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use MediaStore for Android 10+
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Synapse")
                    }
                    
                    val uri = contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    
                    outputStream = uri?.let { contentResolver.openOutputStream(it) }
                } else {
                    // Use legacy storage for Android 9 and below
                    val imagesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    ).toString() + "/Synapse"
                    
                    val dir = File(imagesDir)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    
                    val imageFile = File(dir, fileName)
                    outputStream = FileOutputStream(imageFile)
                }
                
                outputStream?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                }
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save image to gallery", e)
                false
            }
        }
    }
    
    /**
     * Show/hide loading indicator.
     */
    private fun showLoading(show: Boolean, message: String? = null) {
        binding.loadingContainer.visibility = if (show) View.VISIBLE else View.GONE
        message?.let { binding.textLoadingStatus.text = it }
    }
    
    /**
     * Format file size in human-readable format.
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingDownloadPosition?.let { performDownload(it) }
                pendingDownloadPosition = null
            } else {
                Toast.makeText(
                    this,
                    "Storage permission is required to download images",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel any ongoing preload operations
        mediaDownloadManager.cancelPreloading()
    }
    
    /**
     * RecyclerView adapter for ViewPager2.
     */
    private class ImageGalleryAdapter(
        private val imageUrls: List<String>,
        private val thumbnailUrls: List<String>?,
        private val mediaDownloadManager: MediaDownloadManager
    ) : RecyclerView.Adapter<ImageGalleryAdapter.ImageViewHolder>() {
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ImageViewHolder {
            val binding = ItemGalleryImageBinding.inflate(
                android.view.LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ImageViewHolder(binding, mediaDownloadManager)
        }
        
        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val imageUrl = imageUrls[position]
            val thumbnailUrl = thumbnailUrls?.getOrNull(position)
            holder.bind(imageUrl, thumbnailUrl)
        }
        
        override fun getItemCount(): Int = imageUrls.size
        
        /**
         * ViewHolder for gallery images with progressive loading.
         */
        class ImageViewHolder(
            private val binding: ItemGalleryImageBinding,
            private val mediaDownloadManager: MediaDownloadManager
        ) : RecyclerView.ViewHolder(binding.root) {
            
            private var currentImageUrl: String? = null
            
            fun bind(imageUrl: String, thumbnailUrl: String?) {
                currentImageUrl = imageUrl
                
                // Reset views
                binding.photoView.setImageDrawable(null)
                binding.imageThumbnail.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                binding.errorContainer.visibility = View.GONE
                
                // Setup zoom gestures
                setupZoomGestures(binding.photoView)
                
                // Load image with progressive loading
                loadImageProgressively(imageUrl, thumbnailUrl)
                
                // Setup retry button
                binding.buttonRetry.setOnClickListener {
                    loadImageProgressively(imageUrl, thumbnailUrl)
                }
            }
            
            /**
             * Setup pinch-to-zoom gestures with max 3x zoom.
             */
            private fun setupZoomGestures(photoView: PhotoView) {
                photoView.maximumScale = 3.0f
                photoView.mediumScale = 2.0f
                photoView.minimumScale = 1.0f
            }
            
            /**
             * Load image progressively: thumbnail first, then full resolution.
             */
            private fun loadImageProgressively(imageUrl: String, thumbnailUrl: String?) {
                binding.progressBar.visibility = View.VISIBLE
                binding.errorContainer.visibility = View.GONE
                
                // Load thumbnail first if available
                if (thumbnailUrl != null) {
                    loadThumbnail(thumbnailUrl, imageUrl)
                } else {
                    loadFullImage(imageUrl)
                }
            }
            
            /**
             * Load low-resolution thumbnail first.
             */
            private fun loadThumbnail(thumbnailUrl: String, fullImageUrl: String) {
                Glide.with(binding.root.context)
                    .load(thumbnailUrl)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            // If thumbnail fails, load full image directly
                            loadFullImage(fullImageUrl)
                            return false
                        }
                        
                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            // Thumbnail loaded, now load full image
                            binding.imageThumbnail.visibility = View.VISIBLE
                            loadFullImage(fullImageUrl)
                            return false
                        }
                    })
                    .into(binding.imageThumbnail)
            }
            
            /**
             * Load high-resolution image.
             */
            private fun loadFullImage(imageUrl: String) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.progressBar.visibility = View.GONE
                            binding.imageThumbnail.visibility = View.GONE
                            binding.errorContainer.visibility = View.VISIBLE
                            return false
                        }
                        
                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.progressBar.visibility = View.GONE
                            binding.imageThumbnail.visibility = View.GONE
                            binding.errorContainer.visibility = View.GONE
                            return false
                        }
                    })
                    .into(binding.photoView)
            }
        }
    }
}
