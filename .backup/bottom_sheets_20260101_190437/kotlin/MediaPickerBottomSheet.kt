package com.synapse.social.studioasinc.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.BottomSheetMediaPickerBinding
import com.synapse.social.studioasinc.databinding.ItemMediaPickerOptionBinding

/**
 * Bottom sheet dialog for selecting media type to attach to messages
 * Supports images (multi-select), videos, audio, and documents (single select)
 */
class MediaPickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMediaPickerBinding? = null
    private val binding get() = _binding!!

    private var listener: MediaPickerListener? = null

    // Activity result launchers for media selection
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var audioPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var documentPickerLauncher: ActivityResultLauncher<Intent>

    // Permission request launcher
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var pendingMediaType: MediaType? = null

    /**
     * Enum representing media types
     */
    private enum class MediaType {
        IMAGES, VIDEO, AUDIO, DOCUMENT
    }

    /**
     * Data class representing a media picker option
     */
    data class MediaOption(
        val id: String,
        val label: String,
        val icon: Int
    )

    /**
     * Listener interface for media selection callbacks
     * 
     * Integration with MediaPreviewActivity:
     * When onImagesSelected is called with multiple images, the calling activity
     * should launch MediaPreviewActivity to allow the user to preview, add captions,
     * and confirm before uploading.
     * 
     * Example:
     * ```
     * override fun onImagesSelected(uris: List<Uri>) {
     *     val intent = Intent(this, MediaPreviewActivity::class.java).apply {
     *         putExtra(MediaPreviewActivity.EXTRA_CHAT_ID, chatId)
     *         putParcelableArrayListExtra(MediaPreviewActivity.EXTRA_MEDIA_URIS, ArrayList(uris))
     *     }
     *     startActivityForResult(intent, REQUEST_MEDIA_PREVIEW)
     * }
     * ```
     */
    interface MediaPickerListener {
        fun onImagesSelected(uris: List<Uri>)
        fun onVideoSelected(uri: Uri)
        fun onAudioSelected(uri: Uri)
        fun onDocumentSelected(uri: Uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerActivityResultLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMediaPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Trigger haptic feedback
        triggerHapticFeedback()

        // Apply slide-up animation
        applyBottomSheetAnimations()

        // Set up media options list
        setupMediaOptionsList()
    }

    /**
     * Register activity result launchers for media pickers
     */
    private fun registerActivityResultLaunchers() {
        // Image picker - multi-select (max 10)
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uris = mutableListOf<Uri>()
                result.data?.clipData?.let { clipData ->
                    // Multiple images selected
                    val count = minOf(clipData.itemCount, 10)
                    for (i in 0 until count) {
                        clipData.getItemAt(i).uri?.let { uris.add(it) }
                    }
                } ?: result.data?.data?.let { uri ->
                    // Single image selected
                    uris.add(uri)
                }
                
                if (uris.isNotEmpty()) {
                    listener?.onImagesSelected(uris)
                    dismiss()
                }
            }
        }

        // Video picker - single select
        videoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    listener?.onVideoSelected(uri)
                    dismiss()
                }
            }
        }

        // Audio picker - single select
        audioPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    listener?.onAudioSelected(uri)
                    dismiss()
                }
            }
        }

        // Document picker - single select
        documentPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    listener?.onDocumentSelected(uri)
                    dismiss()
                }
            }
        }

        // Permission request launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                pendingMediaType?.let { launchMediaPicker(it) }
                pendingMediaType = null
            }
        }
    }

    /**
     * Apply slide-up animation to bottom sheet appearance
     */
    private fun applyBottomSheetAnimations() {
        val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.root.startAnimation(slideUpAnimation)
        
        dialog?.window?.let { window ->
            window.setWindowAnimations(R.style.BottomSheetAnimation)
        }
    }

    /**
     * Trigger haptic feedback on bottom sheet appearance
     */
    private fun triggerHapticFeedback() {
        try {
            val vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Haptic feedback is optional, ignore errors
        }
    }

    /**
     * Set up RecyclerView with media options
     */
    private fun setupMediaOptionsList() {
        val options = listOf(
            MediaOption(
                id = "photos",
                label = getString(R.string.media_picker_option_photos),
                icon = R.drawable.ic_photo_library_48px
            ),
            MediaOption(
                id = "videos",
                label = getString(R.string.media_picker_option_videos),
                icon = R.drawable.ic_video_library_48px
            ),
            MediaOption(
                id = "audio",
                label = getString(R.string.media_picker_option_audio),
                icon = R.drawable.ic_videocam // Using videocam as placeholder for audio
            ),
            MediaOption(
                id = "documents",
                label = getString(R.string.media_picker_option_documents),
                icon = R.drawable.ic_docs_48px
            )
        )
        
        binding.mediaOptionsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = MediaOptionsAdapter(options) { option ->
                onOptionSelected(option)
            }
        }
    }

    /**
     * Handle media option selection
     */
    private fun onOptionSelected(option: MediaOption) {
        when (option.id) {
            "photos" -> checkPermissionsAndLaunch(MediaType.IMAGES)
            "videos" -> checkPermissionsAndLaunch(MediaType.VIDEO)
            "audio" -> checkPermissionsAndLaunch(MediaType.AUDIO)
            "documents" -> checkPermissionsAndLaunch(MediaType.DOCUMENT)
        }
    }

    /**
     * Check storage permissions and launch media picker
     */
    private fun checkPermissionsAndLaunch(mediaType: MediaType) {
        val permissions = getRequiredPermissions()
        
        if (permissions.all { 
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED 
        }) {
            launchMediaPicker(mediaType)
        } else {
            pendingMediaType = mediaType
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    /**
     * Get required permissions based on Android version
     */
    private fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Launch appropriate media picker based on type
     */
    private fun launchMediaPicker(mediaType: MediaType) {
        when (mediaType) {
            MediaType.IMAGES -> showImagePicker()
            MediaType.VIDEO -> showVideoPicker()
            MediaType.AUDIO -> showAudioPicker()
            MediaType.DOCUMENT -> showDocumentPicker()
        }
    }

    /**
     * Show image picker with multi-select (max 10)
     */
    private fun showImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        imagePickerLauncher.launch(intent)
    }

    /**
     * Show video picker with single select
     */
    private fun showVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            type = "video/*"
        }
        videoPickerLauncher.launch(intent)
    }

    /**
     * Show audio picker with single select
     */
    private fun showAudioPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).apply {
            type = "audio/*"
        }
        audioPickerLauncher.launch(intent)
    }

    /**
     * Show document picker with single select
     */
    private fun showDocumentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain"
            ))
        }
        documentPickerLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "MediaPickerBottomSheet"

        /**
         * Show the bottom sheet with listener
         */
        fun show(
            fragmentManager: FragmentManager,
            listener: MediaPickerListener
        ) {
            val bottomSheet = MediaPickerBottomSheet().apply {
                this.listener = listener
            }
            bottomSheet.show(fragmentManager, TAG)
        }
    }

    /**
     * RecyclerView adapter for media options
     */
    private class MediaOptionsAdapter(
        private val options: List<MediaOption>,
        private val onOptionClick: (MediaOption) -> Unit
    ) : RecyclerView.Adapter<MediaOptionsAdapter.OptionViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
            val binding = ItemMediaPickerOptionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return OptionViewHolder(binding)
        }

        override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
            holder.bind(options[position], onOptionClick)
        }

        override fun getItemCount(): Int = options.size

        class OptionViewHolder(
            private val binding: ItemMediaPickerOptionBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(option: MediaOption, onOptionClick: (MediaOption) -> Unit) {
                binding.optionIcon.setImageResource(option.icon)
                binding.optionLabel.text = option.label

                binding.root.setOnClickListener {
                    onOptionClick(option)
                }
            }
        }
    }
}
