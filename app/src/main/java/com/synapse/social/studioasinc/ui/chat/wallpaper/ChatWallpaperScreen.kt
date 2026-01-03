package com.synapse.social.studioasinc.ui.chat.wallpaper

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.domain.model.ChatWallpaper
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing
import com.synapse.social.studioasinc.domain.model.WallpaperType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWallpaperScreen(
    onBackClick: () -> Unit,
    viewModel: ChatWallpaperViewModel = hiltViewModel()
) {
    val currentWallpaper by viewModel.currentWallpaper.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Predefined solid colors
    val solidColors = listOf(
        Color(0xFF000000), // Black
        Color(0xFFFFFFFF), // White
        Color(0xFFF5F5F5), // Light Grey
        Color(0xFF212121), // Dark Grey
        Color(0xFFE3F2FD), // Light Blue
        Color(0xFFFFEBEE), // Light Red
        Color(0xFFE8F5E9), // Light Green
        Color(0xFFFFF3E0), // Light Orange
        Color(0xFFF3E5F5), // Light Purple
        Color(0xFF6B4EFF), // Brand Purple
    )

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setImageWallpaper(uri)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Chat Wallpaper") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (currentWallpaper.type != WallpaperType.DEFAULT) {
                        IconButton(onClick = { viewModel.resetToDefault() }) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Reset to Default"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Preview Section (could be improved with a mock chat view)
            item {
                Text(
                    text = "Current Wallpaper",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                WallpaperPreview(wallpaper = currentWallpaper)
            }

            // Custom Image
            item {
                Text(
                    text = "Custom Image",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    onClick = {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Choose from Gallery",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Solid Colors
            item {
                Text(
                    text = "Solid Colors",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Using Grid inside Item requires fixed height or carefully managed layout
                // Since we are inside a LazyColumn, we can't easily nest a vertical grid.
                // We'll use a FlowRow equivalent or a fixed grid layout.
                // For simplicity, let's use a custom grid layout here manually.

                Column {
                   val chunkedColors = solidColors.chunked(5)
                   chunkedColors.forEach { rowColors ->
                       Row(
                           modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                           horizontalArrangement = Arrangement.spacedBy(12.dp)
                       ) {
                           rowColors.forEach { color ->
                               val hexColor = String.format("#%08X", (color.toArgb()))
                               val isSelected = currentWallpaper.type == WallpaperType.SOLID_COLOR &&
                                               currentWallpaper.value == hexColor

                               Box(
                                   modifier = Modifier
                                       .size(56.dp)
                                       .clip(CircleShape)
                                       .background(color)
                                       .then(
                                            Modifier.border(
                                                width = if (color == Color.White) 1.dp else 0.dp,
                                                color = Color.LightGray,
                                                shape = CircleShape
                                            )
                                       )
                                       .clickable { viewModel.setSolidColor(hexColor) },
                                   contentAlignment = Alignment.Center
                               ) {
                                   if (isSelected) {
                                       Icon(
                                           imageVector = Icons.Default.Check,
                                           contentDescription = "Selected",
                                           tint = if (color.luminance() > 0.5) Color.Black else Color.White
                                       )
                                   }
                               }
                           }
                       }
                   }
                }
            }
        }
    }
}

@Composable
fun WallpaperPreview(wallpaper: ChatWallpaper) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (wallpaper.type) {
                WallpaperType.DEFAULT -> {
                    // Default Pattern or Color
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                         Text(
                            text = "Default Wallpaper",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                         )
                    }
                }
                WallpaperType.SOLID_COLOR -> {
                    val color = try {
                        Color(android.graphics.Color.parseColor(wallpaper.value))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color)
                    )
                }
                WallpaperType.IMAGE_URI -> {
                    if (wallpaper.value != null) {
                        AsyncImage(
                            model = wallpaper.value,
                            contentDescription = "Wallpaper Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Mock Message Bubbles to show contrast
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface, // Received
                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Hello!",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary, // Sent
                    shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Hi there!",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
