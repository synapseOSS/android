package com.synapse.social.studioasinc.feature.stories.creator

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import android.annotation.SuppressLint
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.StoryPrivacy
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import java.io.File
import java.util.concurrent.Executors

const val EXTRA_SHARED_POST_ID = "shared_post_id"

@AndroidEntryPoint
class StoryCreatorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPostId = intent.getStringExtra(EXTRA_SHARED_POST_ID)
        setContent {
            SynapseTheme {
                StoryCreatorScreen(
                    onClose = { finish() },
                    onStoryPosted = { finish() },
                    sharedPostId = sharedPostId
                )
            }
        }
    }
}

/**
 * Story creator screen with camera capture and editing tools
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryCreatorScreen(
    onClose: () -> Unit,
    onStoryPosted: () -> Unit,
    sharedPostId: String? = null,
    viewModel: StoryCreatorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sharedPostId) {
        if (sharedPostId != null) {
            viewModel.loadSharedPost(sharedPostId)
        }
    }
    
    // Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setMediaFromGallery(it) }
    }
    
    // Request camera permission if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Handle story posted event
    LaunchedEffect(state.isPosted) {
        if (state.isPosted) {
            onStoryPosted()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            state.capturedMediaUri != null || state.sharedPost != null -> {
                // Editing mode
                StoryEditorContent(
                    mediaUri = state.capturedMediaUri,
                    sharedPost = state.sharedPost,
                    mediaType = state.mediaType,
                    textOverlays = state.textOverlays,
                    selectedPrivacy = state.selectedPrivacy,
                    isPosting = state.isPosting,
                    onAddText = { viewModel.addTextOverlay() },
                    onRemoveText = { viewModel.removeTextOverlay(it) },
                    onUpdateTextPosition = { index, offset -> 
                        viewModel.updateTextPosition(index, offset) 
                    },
                    onUpdateTextContent = { index, content ->
                        viewModel.updateTextContent(index, content)
                    },
                    onPrivacyChange = { viewModel.setPrivacy(it) },
                    onPost = { viewModel.postStory() },
                    onCancel = { viewModel.clearCapturedMedia() },
                    onClose = onClose
                )
            }
            hasCameraPermission -> {
                // Capture mode
                CameraCaptureContent(
                    flashMode = state.flashMode,
                    isFrontCamera = state.isFrontCamera,
                    isRecording = state.isRecording,
                    recordingProgress = state.recordingProgress,
                    onFlashToggle = { viewModel.toggleFlash() },
                    onCameraFlip = { viewModel.flipCamera() },
                    onMediaCaptured = { uri, type -> viewModel.setCapturedMedia(uri, type) },
                    onStartRecording = { viewModel.startVideoRecording() },
                    onStopRecording = { viewModel.stopVideoRecording() },
                    onGalleryClick = { galleryLauncher.launch("image/*,video/*") },
                    onClose = onClose
                )
            }
            else -> {
                // Permission denied state
                PermissionDeniedContent(
                    onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    onClose = onClose
                )
            }
        }
        
        // Error snackbar
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

/**
 * Camera capture mode UI
 */
@Composable
private fun CameraCaptureContent(
    flashMode: FlashMode,
    isFrontCamera: Boolean,
    isRecording: Boolean,
    recordingProgress: Float,
    onFlashToggle: () -> Unit,
    onCameraFlip: () -> Unit,
    onMediaCaptured: (Uri, StoryMediaType) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onGalleryClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val recorder = remember { 
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    
    var recording: Recording? by remember { mutableStateOf(null) }
    
    LaunchedEffect(isFrontCamera, flashMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            
            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            
            // Configure Flash
            imageCapture.flashMode = when(flashMode) {
                FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            
            Row {
                IconButton(onClick = onFlashToggle) {
                    Icon(
                        imageVector = when (flashMode) {
                            FlashMode.OFF -> Icons.Default.FlashOff
                            FlashMode.ON -> Icons.Default.FlashOn
                            FlashMode.AUTO -> Icons.Default.FlashAuto
                        },
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Help text
            Text(
                text = if (isRecording) "Recording..." else "Tap for photo",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button
                IconButton(
                    onClick = onGalleryClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = "Gallery",
                        tint = Color.White
                    )
                }
                
                // Capture button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .background(
                            if (isRecording) Color.Red else Color.White,
                            CircleShape
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    onStartRecording()
                                    recordVideo(context, videoCapture, 
                                        onVideoRecorded = { uri ->
                                            onMediaCaptured(uri, StoryMediaType.VIDEO)
                                        },
                                        onRecordingStarted = { rec -> 
                                            recording = rec
                                        }
                                    )
                                },
                                onDragEnd = {
                                    if (isRecording) {
                                        onStopRecording()
                                        recording?.stop()
                                        recording = null
                                    }
                                },
                                onDragCancel = {
                                    if (isRecording) {
                                        onStopRecording()
                                        recording?.stop()
                                        recording = null
                                    }
                                },
                                onDrag = { _, _ -> }
                            )
                        }
                        .clickable { 
                            takePhoto(context, imageCapture) { uri ->
                                onMediaCaptured(uri, StoryMediaType.PHOTO)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        CircularProgressIndicator(
                            progress = { recordingProgress },
                            modifier = Modifier.size(70.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    }
                }
                

                if (isRecording) {
                    Spacer(modifier = Modifier.size(48.dp))
                } else {
                    IconButton(
                        onClick = onCameraFlip,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Flip camera",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun takePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Uri) -> Unit
) {
    val photoFile = File(
        context.getExternalFilesDir(null),
        "story_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                exc.printStackTrace()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onImageCaptured(Uri.fromFile(photoFile))
            }
        }
    )
}

@SuppressLint("MissingPermission")
private fun recordVideo(
    context: android.content.Context,
    videoCapture: VideoCapture<Recorder>,
    onVideoRecorded: (Uri) -> Unit,
    onRecordingStarted: (Recording) -> Unit
) {
    val videoFile = File(
        context.getExternalFilesDir(null),
        "story_video_${System.currentTimeMillis()}.mp4"
    )
    
    val outputOptions = FileOutputOptions.Builder(videoFile).build()
    
    val recording = videoCapture.output
        .prepareRecording(context, outputOptions)
        .apply {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == 
                PackageManager.PERMISSION_GRANTED) {
                withAudioEnabled()
            }
        }
        .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when(recordEvent) {
                is VideoRecordEvent.Start -> {
                    // Recording started
                }
                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        onVideoRecorded(recordEvent.outputResults.outputUri)
                    } else {
                        recordEvent.cause?.printStackTrace()
                    }
                }
            }
        }
        
    onRecordingStarted(recording)
}

/**
 * Story editor content with text overlays and privacy options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoryEditorContent(
    mediaUri: Uri?,
    sharedPost: Post? = null,
    mediaType: StoryMediaType,
    textOverlays: List<TextOverlay>,
    selectedPrivacy: StoryPrivacy,
    isPosting: Boolean,
    onAddText: () -> Unit,
    onRemoveText: (Int) -> Unit,
    onUpdateTextPosition: (Int, Offset) -> Unit,
    onUpdateTextContent: (Int, String) -> Unit,
    onPrivacyChange: (StoryPrivacy) -> Unit,
    onPost: () -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Media preview or background
        if (mediaUri != null) {
            AsyncImage(
                model = mediaUri,
                contentDescription = "Story preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
             Box(modifier = Modifier.fillMaxSize().background(
                 brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                     colors = listOf(
                         Color(0xFF8E2DE2),
                         Color(0xFF4A00E0)
                     )
                 )
             ))
        }

        // Shared Post View
        if (sharedPost != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                SharedPostView(post = sharedPost)
            }
        }
        
        // Text overlays
        textOverlays.forEachIndexed { index, overlay ->
            DraggableTextOverlay(
                text = overlay.text,
                position = overlay.position,
                scale = overlay.scale,
                onPositionChange = { onUpdateTextPosition(index, it) },
                onTextChange = { onUpdateTextContent(index, it) },
                onRemove = { onRemoveText(index) }
            )
        }
        
        // Top toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Row {
                // Text tool
                IconButton(onClick = onAddText) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Add text",
                        tint = Color.White
                    )
                }
                
                // Drawing tool (placeholder)
                IconButton(onClick = { /* TODO */ }) {
                    Icon(
                        imageVector = Icons.Default.Draw,
                        contentDescription = "Draw",
                        tint = Color.White
                    )
                }
                
                // Stickers (placeholder)
                IconButton(onClick = { /* TODO */ }) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Stickers",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Bottom actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Privacy selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = selectedPrivacy == StoryPrivacy.ALL_FRIENDS,
                    onClick = { onPrivacyChange(StoryPrivacy.ALL_FRIENDS) },
                    label = { Text("Friends") },
                    leadingIcon = if (selectedPrivacy == StoryPrivacy.ALL_FRIENDS) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                FilterChip(
                    selected = selectedPrivacy == StoryPrivacy.PUBLIC,
                    onClick = { onPrivacyChange(StoryPrivacy.PUBLIC) },
                    label = { Text("Public") },
                    leadingIcon = if (selectedPrivacy == StoryPrivacy.PUBLIC) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }
            
            // Share button
            Button(
                onClick = onPost,
                enabled = !isPosting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Story", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SharedPostView(post: Post) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (post.avatarUrl != null) {
                    AsyncImage(
                        model = post.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = post.username ?: "Unknown",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            if (!post.postText.isNullOrEmpty()) {
                Text(
                    text = post.postText,
                    color = Color.Black,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Image if available (first one)
            val firstMedia = post.mediaItems?.firstOrNull { it.type == com.synapse.social.studioasinc.domain.model.MediaType.IMAGE }
            if (firstMedia != null) {
                 AsyncImage(
                    model = firstMedia.url,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * Draggable text overlay component
 */
@Composable
private fun DraggableTextOverlay(
    text: String,
    position: Offset,
    scale: Float,
    onPositionChange: (Offset) -> Unit,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offset by remember { mutableStateOf(position) }
    var currentScale by remember { mutableFloatStateOf(scale) }
    var isEditing by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .offset(x = offset.x.dp, y = offset.y.dp)
            .graphicsLayer(scaleX = currentScale, scaleY = currentScale)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    offset = Offset(offset.x + pan.x, offset.y + pan.y)
                    currentScale *= zoom
                    onPositionChange(offset)
                }
            }
            .clickable { isEditing = true }
    ) {
        if (isEditing) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                ),
                singleLine = true
            )
        } else {
            Text(
                text = text.ifEmpty { "Add text" },
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Permission denied content
 */
@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.White
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Camera permission required",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Allow camera access to create stories",
            color = Color.White.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onClose) {
            Text("Cancel", color = Color.White)
        }
    }
}

enum class FlashMode {
    OFF, ON, AUTO
}

data class TextOverlay(
    val text: String = "",
    val position: Offset = Offset.Zero,
    val scale: Float = 1f,
    val color: Color = Color.White
)
