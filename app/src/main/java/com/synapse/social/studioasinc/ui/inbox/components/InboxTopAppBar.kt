package com.synapse.social.studioasinc.ui.inbox.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.ui.inbox.theme.InboxDimens

/**
 * Top app bar for inbox screen.
 * Supports search mode, selection mode, and regular "Large" mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTopAppBar(
    title: String = "Google Messages",
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
    scrollBehavior: TopAppBarScrollBehavior?,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // We use LargeTopAppBar but customize the title to be the "Google Messages" text
    // and we place the Search Bar + Avatar in the 'actions' or we might need a custom layout
    // if the search bar is supposed to be *below* the title in the expanded state.

    // Looking at Google Messages:
    // Expanded:
    // [ Google Messages ] (Large Text)
    // [ Search ...      ] (Avatar)

    // Collapsed:
    // [ Google Messages ] [ Search Icon ] [ Avatar ]

    // Implementation:
    // We can't easily put the Search Bar *inside* the standard LargeTopAppBar title slot nicely without it getting clipped or behaving oddly during collapse.
    // A better approach for this specific custom behavior is to use a standard LargeTopAppBar
    // where the "title" slot contains the Text, and the Search/Avatar are in the 'actions' slot?
    // No, that doesn't match the "Search bar below title" layout.

    // Alternative: The Search Bar + Avatar IS the title in expanded mode?
    // Let's try to mimic it using the standard components first to ensure scrolling works.

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
             // We render a pill-shaped search container in the actions area
             // This mimics the "Search" button next to the Avatar

             Surface(
                 shape = CircleShape,
                 color = MaterialTheme.colorScheme.surfaceContainerHigh,
                 modifier = Modifier
                     .height(48.dp)
                     .clickable(onClick = onSearchClick)
             ) {
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.padding(horizontal = 16.dp)
                 ) {
                     Icon(
                         imageVector = Icons.Default.Search,
                         contentDescription = "Search",
                         modifier = Modifier.size(24.dp)
                     )
                     Spacer(modifier = Modifier.width(8.dp))

                     // Avatar inside the pill? Or outside?
                     // Google Messages: Search Icon is distinct, Avatar is distinct.
                     // BUT, often the Search is a full bar.
                     // Given "Right of Search Bar", let's assume they are siblings.
                 }
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
                 Text(
                     text = "A", // Placeholder
                     style = MaterialTheme.typography.titleMedium,
                     color = MaterialTheme.colorScheme.onPrimary
                 )
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
 * Search mode top app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxSearchTopAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            SimpleSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Search messages...",
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

/**
 * Tab row for inbox tabs (Chats, Calls, Contacts).
 */
@Composable
fun InboxTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("Chats", "Calls", "Contacts")
    
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
                            0 -> if (selectedTabIndex == index) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
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
    val options = listOf("Chats", "Calls", "Contacts")
    
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
                                0 -> Icons.Filled.ChatBubble
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
