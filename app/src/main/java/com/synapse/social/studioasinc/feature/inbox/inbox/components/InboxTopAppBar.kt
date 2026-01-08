package com.synapse.social.studioasinc.ui.inbox.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.ui.inbox.theme.InboxDimens

/**
 * Top app bar for inbox screen.
 * Supports search mode, selection mode, and regular "Large" mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTopAppBar(
    title: String = "Inbox",
    avatarUrl: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    selectionMode: Boolean = false,
    selectedCount: Int = 0,
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSelectionClose: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onArchiveSelected: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedContent(targetState = selectionMode, label = "appBarState") { isSelectionMode ->
        if (isSelectionMode) {
            InboxSelectionTopAppBar(
                selectedCount = selectedCount,
                onClose = onSelectionClose,
                onDelete = onDeleteSelected,
                onArchive = onArchiveSelected,
                modifier = modifier
            )
        } else {
            InboxLargeTopAppBar(
                title = title,
                avatarUrl = avatarUrl,
                scrollBehavior = scrollBehavior,
                onSearchClick = onSearchClick,
                onProfileClick = onProfileClick,
                modifier = modifier
            )
        }
    }
}

/**
 * Google Messages Style Large Top App Bar.
 * Collapses from a large title + search/avatar row to a smaller version.
 *
 * Note: The Material 3 LargeTopAppBar puts the title in the expanded area.
 * To achieve "Title" then "Search Bar + Avatar", we use a custom implementation or
 * abuse the title slot.
 *
 * However, the requirement is: "Refactor InboxTopAppBar to support a Large Top App Bar behavior.
 * The AppBar must contain the user's Profile Picture (Avatar) to the right of the Search Bar."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxLargeTopAppBar(
    title: String,
    avatarUrl: String?,
    scrollBehavior: TopAppBarScrollBehavior?,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LargeTopAppBar(
        title = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        actions = {
             // Search Icon Button
             IconButton(onClick = onSearchClick) {
                 Icon(
                     imageVector = Icons.Default.Search,
                     contentDescription = "Search"
                 )
             }

             Spacer(modifier = Modifier.width(8.dp))

             // Avatar
             Box(
                 modifier = Modifier
                     .size(40.dp) // Larger avatar
                     .clip(CircleShape)
                     .background(MaterialTheme.colorScheme.primary)
                     .clickable(onClick = onProfileClick),
                 contentAlignment = Alignment.Center
             ) {
                 if (avatarUrl != null) {
                     AsyncImage(
                         model = avatarUrl,
                         contentDescription = "Profile",
                         modifier = Modifier.fillMaxSize(),
                         contentScale = ContentScale.Crop
                     )
                 } else {
                     Text(
                         text = "A", // Placeholder - Ideally current user's initial
                         style = MaterialTheme.typography.titleMedium,
                         color = MaterialTheme.colorScheme.onPrimary
                     )
                 }
             }

             Spacer(modifier = Modifier.width(16.dp))
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}

/**
 * Contextual App Bar for Selection Mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxSelectionTopAppBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close selection"
                )
            }
        },
        actions = {
            IconButton(onClick = onArchive) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archive"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer, // Distinct color
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

/**
 * Search mode top app bar with back button and profile picture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxSearchTopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    avatarUrl: String? = null,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Search bar
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search messages...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                
                // Profile picture
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

/**
 * Tab row for inbox tabs (Messages, Calls, Contacts).
 */
@Composable
fun InboxTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("Messages", "Calls", "Contacts")
    
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    height = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                icon = {
                    Icon(
                        imageVector = when (index) {
                            0 -> if (selectedTabIndex == index) Icons.Filled.Email else Icons.Outlined.Email
                            1 -> if (selectedTabIndex == index) Icons.Filled.Call else Icons.Outlined.Call
                            else -> if (selectedTabIndex == index) Icons.Filled.Group else Icons.Outlined.Group
                        },
                        contentDescription = null
                    )
                }
            )
        }
    }
}

/**
 * Segmented button row for M3 Expressive style tabs.
 */
@Composable
fun InboxSegmentedButtons(
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Messages", "Calls", "Contacts")
    
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = selectedIndex == index,
                onClick = { onSelectionChange(index) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                icon = {
                    SegmentedButtonDefaults.Icon(active = selectedIndex == index) {
                        Icon(
                            imageVector = when (index) {
                                0 -> Icons.Filled.Email
                                1 -> Icons.Filled.Call
                                else -> Icons.Filled.Group
                            },
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                }
            ) {
                Text(label)
            }
        }
    }
}
