package com.synapse.social.studioasinc.chat

import android.Manifest
import android.app.Application
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
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.chrisbanes.photoview.PhotoView
import com.synapse.social.studioasinc.BaseActivity
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.remote.services.SupabaseStorageService
import com.synapse.social.studioasinc.chat.service.MediaDownloadManager
import com.synapse.social.studioasinc.core.util.MediaCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

// ViewModel for ImageGallery
class ImageGalleryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val mediaDownloadManager: MediaDownloadManager

    init {
        val storageService = SupabaseStorageService()
        val mediaCache = MediaCache(application)
        mediaDownloadManager = MediaDownloadManager(application, storageService, mediaCache, viewModelScope)
    }

    fun initialize(
        imageUrls: List<String>,
        thumbnailUrls: List<String>?,
        imageNames: List<String>?,
        imageSizes: List<Long>?,
        imageDimensions: List<String>?,
        initialPosition: Int
    ) {
        _uiState.update {
            it.copy(
                imageUrls = imageUrls,
                thumbnailUrls = thumbnailUrls,
                imageNames = imageNames,
                imageSizes = imageSizes,
                imageDimensions = imageDimensions,
                currentPosition = initialPosition
            )
        }
        preloadAdjacentImages(initialPosition)
    }

    fun updatePosition(position: Int) {
        _uiState.update { it.copy(currentPosition = position) }
        preloadAdjacentImages(position)
    }

    private fun preloadAdjacentImages(position: Int) {
        val urls = _uiState.value.imageUrls
        if (urls.isNotEmpty()) {
            mediaDownloadManager.preloadGalleryImages(urls, position)
        }
    }

    fun downloadCurrentImage(context: Context, onSuccess: () -> Unit, onError: () -> Unit) {
        val state = uiState.value
        val position = state.currentPosition
        val imageUrl = state.imageUrls.getOrNull(position) ?: return
        val imageName = state.imageNames?.getOrNull(position) ?: "image_${System.currentTimeMillis()}.jpg"

        _uiState.update { it.copy(isLoading = true, loadingMessage = context.getString(R.string.downloading_image)) }

        viewModelScope.launch {
            try {
                // Download image using Glide to get bitmap
                val bitmap = withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(imageUrl)
                        .submit()
                        .get()
                }

                // Save to gallery
                val saved = saveImageToGallery(context, bitmap, imageName)
                
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null) }
                    if (saved) {
                        onSuccess()
                    } else {
                        onError()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null) }
                    onError()
                }
            }
        }
    }

    private suspend fun saveImageToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val outputStream: OutputStream?
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Synapse")
                    }
                    
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    
                    outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
                } else {
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
                false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaDownloadManager.cancelPreloading()
    }
}

data class GalleryUiState(
    val imageUrls: List<String> = emptyList(),
    val thumbnailUrls: List<String>? = null,
    val imageNames: List<String>? = null,
    val imageSizes: List<Long>? = null,
    val imageDimensions: List<String>? = null,
    val currentPosition: Int = 0,
    val isLoading: Boolean = false,
    val loadingMessage: String? = null
)

class ImageGalleryActivity : BaseActivity() {

    companion object {
        private const val TAG = "ImageGalleryActivity"
        private const val EXTRA_IMAGE_URLS = "extra_image_urls"
        private const val EXTRA_THUMBNAIL_URLS = "extra_thumbnail_urls"
        private const val EXTRA_IMAGE_NAMES = "extra_image_names"
        private const val EXTRA_IMAGE_SIZES = "extra_image_sizes"
        private const val EXTRA_IMAGE_DIMENSIONS = "extra_image_dimensions"
        private const val EXTRA_INITIAL_POSITION = "extra_initial_position"

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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.downloadCurrentImage(
                this,
                onSuccess = { Toast.makeText(this, getString(R.string.image_downloaded), Toast.LENGTH_SHORT).show() },
                onError = { Toast.makeText(this, getString(R.string.failed_to_download_image), Toast.LENGTH_SHORT).show() }
            )
        } else {
            Toast.makeText(
                this,
                "Storage permission is required to download images",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private lateinit var viewModel: ImageGalleryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide system bars for immersive experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application))[ImageGalleryViewModel::class.java]

        if (savedInstanceState == null) {
            val imageUrls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS) ?: emptyList()
            if (imageUrls.isEmpty()) {
                finish()
                return
            }
            val thumbnailUrls = intent.getStringArrayListExtra(EXTRA_THUMBNAIL_URLS)
            val imageNames = intent.getStringArrayListExtra(EXTRA_IMAGE_NAMES)
            val imageSizes = intent.getLongArrayExtra(EXTRA_IMAGE_SIZES)?.toList()
            val imageDimensions = intent.getStringArrayListExtra(EXTRA_IMAGE_DIMENSIONS)
            val initialPosition = intent.getIntExtra(EXTRA_INITIAL_POSITION, 0)

            viewModel.initialize(imageUrls, thumbnailUrls, imageNames, imageSizes, imageDimensions, initialPosition)
        }

        setContent {
            ImageGalleryScreen(
                viewModel = viewModel,
                onBack = { finish() },
                onDownload = {
                    checkPermissionAndDownload()
                }
            )
        }
    }

    private fun checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }
        viewModel.downloadCurrentImage(
            this,
            onSuccess = { Toast.makeText(this, getString(R.string.image_downloaded), Toast.LENGTH_SHORT).show() },
            onError = { Toast.makeText(this, getString(R.string.failed_to_download_image), Toast.LENGTH_SHORT).show() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGalleryScreen(
    viewModel: ImageGalleryViewModel,
    onBack: () -> Unit,
    onDownload: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(initialPage = uiState.currentPosition) { uiState.imageUrls.size }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.updatePosition(pagerState.currentPage)
    }

    var showControls by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls }
                )
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val imageUrl = uiState.imageUrls.getOrNull(page)
            val thumbnailUrl = uiState.thumbnailUrls?.getOrNull(page)
            if (imageUrl != null) {
                PhotoViewComposable(
                    imageUrl = imageUrl,
                    thumbnailUrl = thumbnailUrl,
                    onTap = { showControls = !showControls }
                )
            }
        }

        // Toolbar
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            GalleryTopBar(
                title = uiState.imageNames?.getOrNull(pagerState.currentPage) ?: "Image ${pagerState.currentPage + 1}",
                subtitle = formatMetadata(
                    uiState.imageSizes?.getOrNull(pagerState.currentPage),
                    uiState.imageDimensions?.getOrNull(pagerState.currentPage)
                ),
                onBack = onBack
            )
        }

        // Download FAB
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = onDownload,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download"
                )
            }
        }
        
        // Loading Indicator
        if (uiState.isLoading) {
             Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    uiState.loadingMessage?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = it, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTopBar(
    title: String,
    subtitle: String?,
    onBack: () -> Unit
) {
    // Add safe area padding for status bar
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun PhotoViewComposable(
    imageUrl: String,
    thumbnailUrl: String?,
    onTap: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        var isLoading by remember { mutableStateOf(true) }
        var isError by remember { mutableStateOf(false) }

        if (isLoading) {
             CircularProgressIndicator(color = Color.White)
        }

        if (isError) {
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_error), // Make sure this resource exists or use a vector icon
                    contentDescription = "Error",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Failed to load image",
                    color = Color.White,
                    fontSize = 14.sp
                )
             }
        }

        AndroidView(
            factory = { context ->
                PhotoView(context).apply {
                    maximumScale = 3.0f
                    mediumScale = 2.0f
                    minimumScale = 1.0f

                    setOnMatrixChangeListener { _ -> }

                    setOnPhotoTapListener { _, _, _ ->
                        onTap()
                    }
                }
            },
            update = { photoView ->
                val requestBuilder = Glide.with(photoView).load(imageUrl)
                
                if (thumbnailUrl != null) {
                    requestBuilder.thumbnail(Glide.with(photoView).load(thumbnailUrl))
                }

                requestBuilder.listener(object : com.bumptech.glide.request.RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        isLoading = false
                        isError = true
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        isLoading = false
                        isError = false
                        return false
                    }
                }).into(photoView)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

fun formatMetadata(size: Long?, dimensions: String?): String? {
    val sizeText = size?.let { formatFileSize(it) }

    return when {
        sizeText != null && dimensions != null -> "$sizeText • $dimensions"
        sizeText != null -> sizeText
        dimensions != null -> dimensions
        else -> null
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
