package com.synapse.social.studioasinc.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
// import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.synapse.social.studioasinc.BaseActivity
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.ActivityMediaPreviewBinding
import java.text.DecimalFormat
import kotlin.math.ln
import kotlin.math.pow

/**
 * Activity for previewing selected media files before sending
 * Displays thumbnails in a grid, allows caption input, and shows upload estimates
 */
class MediaPreviewActivity : BaseActivity() {

    private lateinit var binding: ActivityMediaPreviewBinding
    private lateinit var mediaAdapter: MediaPreviewAdapter
    private val selectedMediaUris = mutableListOf<Uri>()
    private var chatId: String? = null
    private var maxMediaCount: Int = 10

    // Activity result launcher for adding more media
    private lateinit var addMoreMediaLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get intent extras
        chatId = intent.getStringExtra(EXTRA_CHAT_ID)
        val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_MEDIA_URIS, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_MEDIA_URIS)
        }
        
        uris?.let { selectedMediaUris.addAll(it) }

        // Register activity result launcher
        registerAddMoreMediaLauncher()

        // Setup UI
        setupToolbar()
        setupMediaGrid()
        setupCaptionInput()
        setupSendButton()
        
        // Calculate and display upload info
        updateUploadInfo()
    }

    /**
     * Register activity result launcher for adding more media
     */
    private fun registerAddMoreMediaLauncher() {
        addMoreMediaLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val newUris = mutableListOf<Uri>()
                result.data?.clipData?.let { clipData ->
                    // Multiple images selected
                    for (i in 0 until clipData.itemCount) {
                        clipData.getItemAt(i).uri?.let { newUris.add(it) }
                    }
                } ?: result.data?.data?.let { uri ->
                    // Single image selected
                    newUris.add(uri)
                }
                
                // Add new URIs up to the limit
                val remainingSlots = maxMediaCount - selectedMediaUris.size
                val urisToAdd = newUris.take(remainingSlots)
                selectedMediaUris.addAll(urisToAdd)
                
                // Update UI
                mediaAdapter.notifyDataSetChanged()
                updateUploadInfo()
            }
        }
    }

    /**
     * Setup toolbar with back button
     */
    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    /**
     * Setup media grid with GridLayoutManager (2 columns)
     */
    private fun setupMediaGrid() {
        mediaAdapter = MediaPreviewAdapter(
            context = this,
            mediaUris = selectedMediaUris,
            onRemoveClick = { position ->
                removeMediaAt(position)
            },
            onAddMoreClick = {
                addMoreMedia()
            }
        )
        
        binding.mediaGrid.apply {
            layoutManager = GridLayoutManager(this@MediaPreviewActivity, 2)
            adapter = mediaAdapter
        }
    }

    /**
     * Setup caption input with text watcher
     */
    private fun setupCaptionInput() {
        binding.captionEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                // Update character count if needed
                val length = s?.length ?: 0
                val maxLength = 500
                
                // Optional: Show character count
                // binding.characterCountText.text = "$length/$maxLength"
            }
        })
    }

    /**
     * Setup send button click listener
     */
    private fun setupSendButton() {
        binding.sendFab.setOnClickListener {
            sendMedia()
        }
    }

    /**
     * Remove media at specified position
     */
    private fun removeMediaAt(position: Int) {
        if (position in selectedMediaUris.indices) {
            selectedMediaUris.removeAt(position)
            mediaAdapter.notifyItemRemoved(position)
            mediaAdapter.notifyItemRangeChanged(position, selectedMediaUris.size)
            updateUploadInfo()
            
            // If no media left, finish activity
            if (selectedMediaUris.isEmpty()) {
                finish()
            }
        }
    }

    /**
     * Add more media files (up to limit)
     */
    private fun addMoreMedia() {
        val remainingSlots = maxMediaCount - selectedMediaUris.size
        if (remainingSlots <= 0) {
            // Show toast that limit reached
            return
        }
        
        // Check permissions
        val permissions = getRequiredPermissions()
        if (permissions.all { 
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }) {
            launchImagePicker()
        } else {
            // Request permissions
            requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSION_CODE)
        }
    }

    /**
     * Get required permissions based on Android version
     */
    private fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Launch image picker
     */
    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        addMoreMediaLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                launchImagePicker()
            }
        }
    }

    /**
     * Calculate total size of all selected media files
     */
    private fun calculateTotalSize(): Long {
        var totalSize = 0L
        
        selectedMediaUris.forEach { uri ->
            try {
                contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    totalSize += descriptor.statSize
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return totalSize
    }

    /**
     * Estimate upload time based on file sizes
     * Assumes average upload speed of 1 MB/s (adjust based on typical network conditions)
     */
    private fun estimateUploadTime(totalSizeBytes: Long): String {
        val averageUploadSpeedBytesPerSecond = 1_000_000L // 1 MB/s
        val estimatedSeconds = totalSizeBytes / averageUploadSpeedBytesPerSecond
        
        return when {
            estimatedSeconds < 5 -> "< 5 seconds"
            estimatedSeconds < 60 -> "~${estimatedSeconds} seconds"
            estimatedSeconds < 3600 -> {
                val minutes = estimatedSeconds / 60
                "~$minutes minute${if (minutes > 1) "s" else ""}"
            }
            else -> {
                val hours = estimatedSeconds / 3600
                val minutes = (estimatedSeconds % 3600) / 60
                "~$hours hour${if (hours > 1) "s" else ""} $minutes min"
            }
        }
    }

    /**
     * Format file size in human-readable format
     */
    private fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(sizeBytes.toDouble()) / ln(1024.0)).toInt()
        
        val size = sizeBytes / 1024.0.pow(digitGroups.toDouble())
        val df = DecimalFormat("#,##0.#")
        
        return "${df.format(size)} ${units[digitGroups]}"
    }

    /**
     * Update upload information display
     */
    private fun updateUploadInfo() {
        val totalSize = calculateTotalSize()
        val estimatedTime = estimateUploadTime(totalSize)
        
        binding.totalSizeText.text = formatFileSize(totalSize)
        binding.estimatedTimeText.text = estimatedTime
        
        // Update title to show count
        binding.titleText.text = getString(
            R.string.media_preview_title_with_count,
            selectedMediaUris.size,
            maxMediaCount
        )
    }

    /**
     * Validate all files before sending
     */
    private fun validateFiles(): Boolean {
        // Check if there are any files
        if (selectedMediaUris.isEmpty()) {
            showError(getString(R.string.media_preview_error_no_files))
            return false
        }
        
        // Validate each file
        for (uri in selectedMediaUris) {
            try {
                // Check if file is accessible
                contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    val fileSize = descriptor.statSize
                    
                    // Check file size (max 100MB per file for videos, 2MB for images after compression)
                    val mimeType = contentResolver.getType(uri)
                    val maxSize = when {
                        mimeType?.startsWith("video/") == true -> 100 * 1024 * 1024L // 100MB
                        mimeType?.startsWith("image/") == true -> 10 * 1024 * 1024L // 10MB (before compression)
                        else -> 50 * 1024 * 1024L // 50MB for other files
                    }
                    
                    if (fileSize > maxSize) {
                        showError(
                            getString(
                                R.string.media_preview_error_file_too_large,
                                formatFileSize(maxSize)
                            )
                        )
                        return false
                    }
                } ?: run {
                    showError(getString(R.string.media_preview_error_file_not_accessible))
                    return false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError(getString(R.string.media_preview_error_file_validation))
                return false
            }
        }
        
        return true
    }

    /**
     * Show error message
     */
    private fun showError(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.error_unexpected)
            .setMessage(message)
            .setPositiveButton(R.string.okay) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Send media with caption
     */
    private fun sendMedia() {
        // Validate files first
        if (!validateFiles()) {
            return
        }
        
        val caption = binding.captionEditText.text?.toString()?.trim() ?: ""
        
        // Check for large files (>50MB)
        val totalSize = calculateTotalSize()
        val maxSizeBytes = 50 * 1024 * 1024L // 50MB
        
        if (totalSize > maxSizeBytes) {
            // Show warning dialog
            showLargeFileWarning(totalSize) {
                proceedWithSend(caption)
            }
        } else {
            proceedWithSend(caption)
        }
    }

    /**
     * Show warning dialog for large files
     */
    private fun showLargeFileWarning(totalSize: Long, onProceed: () -> Unit) {
        val estimatedTime = estimateUploadTime(totalSize)
        val formattedSize = formatFileSize(totalSize)
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.media_preview_file_too_large)
            .setMessage(
                getString(
                    R.string.media_preview_warning_large_file_detailed,
                    formattedSize,
                    estimatedTime
                )
            )
            .setPositiveButton(R.string.continue_button) { _, _ ->
                onProceed()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        dialog.show()
    }

    /**
     * Proceed with sending media
     */
    private fun proceedWithSend(caption: String) {
        // Return result to calling activity (ChatActivity)
        // The ChatActivity will then trigger upload via ChatViewModel
        // This follows the pattern where the Activity handles the result
        // and delegates to ViewModel for business logic
        val resultIntent = Intent().apply {
            putParcelableArrayListExtra(RESULT_MEDIA_URIS, ArrayList(selectedMediaUris))
            putExtra(RESULT_CAPTION, caption)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_MEDIA_URIS = "extra_media_uris"
        const val RESULT_MEDIA_URIS = "result_media_uris"
        const val RESULT_CAPTION = "result_caption"
        private const val REQUEST_PERMISSION_CODE = 1001
    }
}
