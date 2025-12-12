package com.synapse.social.studioasinc.presentation.editprofile.photohistory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoHistoryScreen(
    viewModel: PhotoHistoryViewModel = viewModel(),
    type: PhotoType,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Load data on start
    LaunchedEffect(type) {
        viewModel.onEvent(PhotoHistoryEvent.LoadHistory(type))
    }

    // Confirmation Dialog State
    var itemToDelete by remember { mutableStateOf<HistoryItem?>(null) }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Photo?") },
            text = { Text("Are you sure you want to delete this photo from history? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { viewModel.onEvent(PhotoHistoryEvent.DeleteItem(it)) }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = when (type) {
                            PhotoType.PROFILE -> "Profile Photos"
                            PhotoType.COVER -> "Cover Photos"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveLoadingIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Enhanced Empty State with Pulse Animation
                    // Since Lottie is not available, we simulate a "breathing" or "pulsing" effect
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                CircleShape
                            )
                    ) {
                         Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "No history yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your previous photos will appear here",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    contentPadding = PaddingValues(SettingsSpacing.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.items,
                        key = { it.key }
                    ) { item ->
                        val isSelected = item.imageUrl == uiState.currentPhotoUrl

                        PhotoHistoryItem(
                            modifier = Modifier.animateItem(),
                            item = item,
                            isSelected = isSelected,
                            onClick = { viewModel.onEvent(PhotoHistoryEvent.SetAsCurrent(item)) },
                            onLongClick = { itemToDelete = item }
                        )
                    }
                }
            }

            // Error Snackbar or Banner could go here
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoHistoryItem(
    modifier: Modifier = Modifier,
    item: HistoryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "History Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Selection Indicator
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
