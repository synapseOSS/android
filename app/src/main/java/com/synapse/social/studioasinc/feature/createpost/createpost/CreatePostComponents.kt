package com.synapse.social.studioasinc.ui.createpost

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.User

@Composable
fun UserHeader(
    user: User?,
    privacy: String,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Profile info row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar
            if (user?.avatar != null) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.ic_person),
                    error = painterResource(R.drawable.ic_person)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name
            Text(
                text = user?.displayName ?: user?.username ?: "You",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Privacy selector as proper FilterChip
        FilterChip(
            onClick = onPrivacyClick,
            label = {
                Text(
                    text = privacy.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium
                )
            },
            selected = false,
            enabled = true,
            leadingIcon = {
                Icon(
                    imageVector = when(privacy) {
                        "followers" -> Icons.Default.Group
                        "private" -> Icons.Default.Lock
                        else -> Icons.Default.Public
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Change privacy",
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                labelColor = MaterialTheme.colorScheme.onSurface,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySelectionSheet(
    currentPrivacy: String,
    onPrivacySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Who can see your post?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Your post will appear in Feed, on your profile and in search results.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            val options = listOf(
                Triple("Public", "Anyone on or off the app", Icons.Default.Public),
                Triple("Followers", "Your followers on the app", Icons.Default.Group),
                Triple("Private", "Only me", Icons.Default.Lock)
            )

            options.forEach { (label, desc, icon) ->
                val isSelected = currentPrivacy == label.lowercase()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPrivacySelected(label.lowercase()) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = null // Handled by Row click
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollCreationSheet(
    onDismiss: () -> Unit,
    onCreatePoll: (PollData) -> Unit
) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var duration by remember { mutableIntStateOf(24) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Create Poll", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Ask a question...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            options.forEachIndexed { index, option ->
                OutlinedTextField(
                    value = option,
                    onValueChange = { options[index] = it },
                    label = { Text("Option ${index + 1}") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = if (options.size > 2) {
                        {
                            IconButton(onClick = { options.removeAt(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove option")
                            }
                        }
                    } else null
                )
            }

            if (options.size < 4) {
                TextButton(
                    onClick = { options.add("") },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Option")
                }
            }

            Button(
                onClick = {
                    val validOptions = options.filter { it.isNotBlank() }
                    if (question.isNotBlank() && validOptions.size >= 2) {
                        onCreatePoll(PollData(question, validOptions, duration))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = question.isNotBlank() && options.count { it.isNotBlank() } >= 2,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Poll to Post", modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
fun MediaPreviewGrid(
    mediaItems: List<MediaItem>,
    onRemove: (Int) -> Unit
) {
    if (mediaItems.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .heightIn(max = 300.dp) // Limit height
                .fillMaxWidth()
        ) {
            itemsIndexed(mediaItems) { index, item ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = item.url,
                        contentDescription = "Attached Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (item.type == MediaType.VIDEO) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onRemove(index) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPostSheet(
    onDismiss: () -> Unit,
    onMediaClick: () -> Unit,
    onPollClick: () -> Unit,
    onLocationClick: () -> Unit,
    onYoutubeClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Add content",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp, start = 8.dp)
            )

            val actions = listOf(
                Triple("Photo/Video", Icons.Filled.Image, MaterialTheme.colorScheme.primary) to onMediaClick,
                Triple("Poll", Icons.Default.Poll, MaterialTheme.colorScheme.tertiary) to onPollClick,
                Triple("Location", Icons.Filled.Place, MaterialTheme.colorScheme.error) to onLocationClick,
                Triple("YouTube", Icons.Default.VideoLibrary, MaterialTheme.colorScheme.secondary) to onYoutubeClick
            )

            actions.forEach { (item, action) ->
                val (label, icon, color) = item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = {
                            action()
                            onDismiss()
                        })
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = color.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = label, 
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AddToPostBar(
    onMediaClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onMediaClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    Icons.Filled.Image, 
                    contentDescription = "Add photo or video"
                )
            }
            
            FilledTonalIconButton(
                onClick = onMoreClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "More options"
                )
            }
        }
    }
}
