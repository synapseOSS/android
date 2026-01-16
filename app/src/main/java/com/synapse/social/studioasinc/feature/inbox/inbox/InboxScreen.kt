package com.synapse.social.studioasinc.ui.inbox

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.ui.inbox.models.InboxAction
import com.synapse.social.studioasinc.ui.inbox.models.InboxUiState
import com.synapse.social.studioasinc.ui.inbox.components.*
import com.synapse.social.studioasinc.ui.inbox.screens.CallsTabScreen
import com.synapse.social.studioasinc.ui.inbox.screens.ChatsTabScreen
import com.synapse.social.studioasinc.ui.inbox.screens.ContactsTabScreen
import com.synapse.social.studioasinc.ui.inbox.theme.InboxTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: InboxViewModel = viewModel(factory = InboxViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    // Search state managed by ViewModel but exposed here for UI
    val isSearchActive = viewModel.isSearchActive.collectAsState().value
    val searchQuery = viewModel.searchQuery.collectAsState().value
    
    // FAB state
    var isFabExpanded by remember { mutableStateOf(false) }

    // Bottom Sheet for long-press actions
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedChatIdForSheet by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Determine selection state
    val selectionMode = (uiState as? InboxUiState.Success)?.selectionMode == true
    val selectedCount = (uiState as? InboxUiState.Success)?.selectedItems?.size ?: 0

    if (showBottomSheet && selectedChatIdForSheet != null) {
        val isSelectedChatArchived = remember(selectedChatIdForSheet, uiState) {
            if (uiState is InboxUiState.Success && selectedChatIdForSheet != null) {
                (uiState as InboxUiState.Success).archivedChats.any { it.id == selectedChatIdForSheet }
            } else false
        }

        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                selectedChatIdForSheet = null
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Message Options",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showBottomSheet = false
                            viewModel.onAction(InboxAction.ToggleSelectionMode(selectedChatIdForSheet))
                            selectedChatIdForSheet = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showBottomSheet = false
                            selectedChatIdForSheet?.let { 
                                if (isSelectedChatArchived) {
                                    viewModel.onAction(InboxAction.UnarchiveChat(it)) 
                                } else {
                                    viewModel.onAction(InboxAction.ArchiveChat(it))
                                }
                            }
                            selectedChatIdForSheet = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSelectedChatArchived) "Unarchive" else "Archive")
                }

                 Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showBottomSheet = false
                            selectedChatIdForSheet?.let { viewModel.onAction(InboxAction.DeleteChat(it)) }
                            selectedChatIdForSheet = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedContent(
                targetState = isSearchActive,
                label = "topBar"
            ) { searchActive ->
                if (searchActive) {
                    InboxSearchTopAppBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.onAction(InboxAction.SearchQueryChanged(it)) },
                        onBackClick = { viewModel.toggleSearch(false) },
                        avatarUrl = currentUserProfile?.avatar,
                        onProfileClick = { currentUserProfile?.id?.let(onNavigateToProfile) }
                    )
                } else {
                    InboxTopAppBar(
                        title = "Inbox",
                        avatarUrl = currentUserProfile?.avatar,
                        scrollBehavior = scrollBehavior,
                        selectionMode = selectionMode,
                        selectedCount = selectedCount,
                        onSearchClick = { viewModel.toggleSearch(true) },
                        onSelectionClose = { viewModel.onAction(InboxAction.ClearSelection) },
                        onDeleteSelected = { viewModel.onAction(InboxAction.DeleteSelected) },
                        onArchiveSelected = { viewModel.onAction(InboxAction.ArchiveSelected) },
                        onProfileClick = { currentUserProfile?.id?.let(onNavigateToProfile) }
                    )
                }
            }
        },
        bottomBar = {
             // Hide navigation bar when in selection mode? Google Messages does not hide it usually,
             // but context actions are in top bar.
             // If we want full focus, we can hide it. Let's keep it for now as per requirement "Selection Mode UI - The AppBar transforms..."
             NavigationBar(
                 containerColor = MaterialTheme.colorScheme.surfaceContainer,
                 contentColor = MaterialTheme.colorScheme.onSurface
             ) {
                 val tabs = listOf("Messages", "Calls", "Contacts")
                 tabs.forEachIndexed { index, title ->
                     val selected = pagerState.currentPage == index
                     NavigationBarItem(
                         selected = selected,
                         onClick = {
                             scope.launch { pagerState.animateScrollToPage(index) }
                         },
                         label = { 
                             Text(
                                 text = title,
                                 color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer 
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                             ) 
                         },
                         icon = {
                             Icon(
                                 imageVector = when (index) {
                                     0 -> if (selected) Icons.Filled.Email else Icons.Outlined.Email
                                     1 -> if (selected) Icons.Filled.Call else Icons.Outlined.Call
                                     else -> if (selected) Icons.Filled.Group else Icons.Outlined.Group
                                 },
                                 contentDescription = title,
                                 tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer 
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                             )
                         },
                         colors = NavigationBarItemDefaults.colors(
                             selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                             selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                             indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                             unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                             unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                     )
                 }
             }
        },
        floatingActionButton = {
            // Only show FAB on Chats tab (page 0)
            AnimatedVisibility(
                visible = pagerState.currentPage == 0 && !isSearchActive,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                NewChatFab(
                    onClick = { viewModel.onAction(InboxAction.NavigateToNewChat) },
                    onGroupClick = onNavigateToCreateGroup,
                    expanded = isFabExpanded,
                    onExpandChange = { isFabExpanded = it }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { paddingValues ->
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> {
                    // Inject filtered chats if search is active
                    val chatsState = if (isSearchActive) {
                        val filtered = viewModel.filteredChats.collectAsState().value
                        uiState.let {
                            if (it is com.synapse.social.studioasinc.ui.inbox.models.InboxUiState.Success) {
                                it.copy(chats = filtered, pinnedChats = emptyList())
                            } else it
                        }
                    } else uiState
                    
                    ChatsTabScreen(
                        state = chatsState,
                        searchQuery = searchQuery,
                        onAction = { action ->
                            when (action) {
                                is InboxAction.OpenChat -> {
                                    onNavigateToChat(action.chatId, action.userId)
                                }
                                else -> viewModel.onAction(action)
                            }
                        },
                        onLongPressChat = { chatId ->
                            selectedChatIdForSheet = chatId
                            showBottomSheet = true
                        }
                    )
                }
                1 -> CallsTabScreen()
                2 -> ContactsTabScreen()
            }
        }
    }
}
