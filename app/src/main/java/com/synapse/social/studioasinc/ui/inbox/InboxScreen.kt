package com.synapse.social.studioasinc.ui.inbox

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    viewModel: InboxViewModel = viewModel(factory = InboxViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    // Search state managed by ViewModel but exposed here for UI
    val isSearchActive = viewModel.isSearchActive.collectAsState().value
    val searchQuery = viewModel.searchQuery.collectAsState().value
    
    // FAB state
    var isFabExpanded by remember { mutableStateOf(false) }

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
                        onBackClick = { viewModel.toggleSearch(false) }
                    )
                } else {
                    InboxTopAppBar(
                        title = "Inbox",
                        scrollBehavior = scrollBehavior,
                        onSearchClick = { viewModel.toggleSearch(true) }
                    )
                }
            }
        },
        bottomBar = {
             NavigationBar {
                 val tabs = listOf("Chats", "Calls", "Contacts")
                 tabs.forEachIndexed { index, title ->
                     val selected = pagerState.currentPage == index
                     NavigationBarItem(
                         selected = selected,
                         onClick = {
                             scope.launch { pagerState.animateScrollToPage(index) }
                         },
                         label = { Text(title) },
                         icon = {
                             Icon(
                                 imageVector = when (index) {
                                     0 -> if (selected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
                                     1 -> if (selected) Icons.Filled.Call else Icons.Outlined.Call
                                     else -> if (selected) Icons.Filled.Group else Icons.Outlined.Group
                                 },
                                 contentDescription = title
                             )
                         }
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
                        }
                    )
                }
                1 -> CallsTabScreen()
                2 -> ContactsTabScreen()
            }
        }
    }
}
