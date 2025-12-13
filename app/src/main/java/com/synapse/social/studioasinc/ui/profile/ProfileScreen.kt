package com.synapse.social.studioasinc.ui.profile

// TODO: Improve Profile Cover Parallax Effect - enhance smoothness and depth
// TODO: Fix Profile Picture Display in Home Feed - ensure avatar URL is resolved correctly from Post model

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.ui.components.EmptyState
import com.synapse.social.studioasinc.ui.components.ErrorState
import com.synapse.social.studioasinc.ui.components.post.PostCard
import com.synapse.social.studioasinc.ui.components.post.PostCardState
import com.synapse.social.studioasinc.ui.components.post.SharedPostItem
import com.synapse.social.studioasinc.ui.components.post.PostActions
import com.synapse.social.studioasinc.ui.profile.animations.crossfadeContent
import com.synapse.social.studioasinc.ui.profile.components.*
import kotlinx.coroutines.delay

/**
 * Main Profile screen composable displaying user profile information and content.
 * 
 * Features:
 * - Cover photo with parallax scrolling
 * - Profile header with animated story ring
 * - Animated stat counters
 * - Content tabs with sliding indicator (Posts, Photos, Reels)
 * - Pull-to-refresh with custom animation
 * - Staggered content loading animations
 * - Bottom sheet actions (Share, View As, QR Code, etc.)
 * 
 * @param userId The ID of the user whose profile to display
 * @param currentUserId The ID of the currently logged-in user
 * @param onNavigateBack Callback for back navigation
 * @param onNavigateToEditProfile Callback to navigate to edit profile
 * @param onNavigateToFollowers Callback to navigate to followers list
 * @param onNavigateToFollowing Callback to navigate to following list
 * @param onNavigateToSettings Callback to navigate to settings
 * @param onNavigateToActivityLog Callback to navigate to activity log
 * @param onNavigateToUserProfile Callback to navigate to another user's profile
 * @param viewModel ProfileViewModel instance for state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToFollowers: () -> Unit = {},
    onNavigateToFollowing: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToActivityLog: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    viewModel: ProfileViewModel = viewModel<ProfileViewModel>()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }
    var showCustomizationDialog by remember { mutableStateOf(false) }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val coverHeightPx = with(density) { 180.dp.toPx() }

    // Calculate scroll progress for parallax effect
    val scrollProgress = remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / coverHeightPx).coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId, currentUserId)
    }

    Scaffold(
        topBar = {
            ProfileTopAppBar(
                username = (state.profileState as? ProfileUiState.Success)?.profile?.username ?: "",
                scrollProgress = scrollProgress.value,
                onBackClick = onNavigateBack,
                onMoreClick = { viewModel.toggleMoreMenu() }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { isTraversalGroup = true }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refreshProfile(userId)
                isRefreshing = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val profileState = state.profileState) {
                is ProfileUiState.Loading -> {
                    ProfileSkeletonScreen()
                }
                is ProfileUiState.Success -> {
                    ProfileContent(
                        state = state,
                        profile = profileState.profile,
                        listState = listState,
                        scrollProgress = scrollProgress.value,
                        viewModel = viewModel,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToFollowers = onNavigateToFollowers,
                        onNavigateToFollowing = onNavigateToFollowing,
                        onNavigateToUserProfile = onNavigateToUserProfile,
                        onNavigateToChat = onNavigateToChat,
                        onCustomizeClick = { showCustomizationDialog = true }
                    )
                }
                is ProfileUiState.Error -> {
                    ErrorState(
                        title = "Error Loading Profile",
                        message = profileState.message,
                        onRetry = { viewModel.refreshProfile(userId) }
                    )
                }
                is ProfileUiState.Empty -> {
                    EmptyState(
                        icon = Icons.Default.Person,
                        title = "Profile Not Found",
                        message = "This profile doesn't exist or has been removed."
                    )
                }
            }
        }
    }

    // Bottom Sheets
    if (state.showMoreMenu) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        ProfileMoreMenuBottomSheet(
            isOwnProfile = state.isOwnProfile,
            onDismiss = { viewModel.toggleMoreMenu() },
            onShareProfile = { viewModel.showShareSheet() },
            onViewAs = { viewModel.showViewAsSheet() },
            onLockProfile = { 
                profile?.let { viewModel.lockProfile(!it.isPrivate) }
            },
            onArchiveProfile = { 
                profile?.let { viewModel.archiveProfile(true) }
            },
            onQrCode = { viewModel.showQrCode() },
            onCopyLink = { /* TODO: Copy to clipboard */ },
            onSettings = onNavigateToSettings,
            onActivityLog = onNavigateToActivityLog,
            onBlockUser = { 
                profile?.let { viewModel.blockUser(it.id) }
            },
            onReportUser = { viewModel.showReportDialog() },
            onMuteUser = { 
                profile?.let { viewModel.muteUser(it.id) }
            }
        )
    }

    if (state.showShareSheet) {
        ShareProfileBottomSheet(
            onDismiss = { viewModel.hideShareSheet() },
            onCopyLink = { /* TODO */ },
            onShareToStory = { /* TODO */ },
            onShareViaMessage = { /* TODO */ },
            onShareExternal = { /* TODO */ }
        )
    }

    if (state.showViewAsSheet) {
        ViewAsBottomSheet(
            onDismiss = { viewModel.hideViewAsSheet() },
            onViewAsPublic = { 
                viewModel.setViewAsMode(ViewAsMode.PUBLIC)
                viewModel.hideViewAsSheet()
            },
            onViewAsFriends = { 
                viewModel.setViewAsMode(ViewAsMode.FRIENDS)
                viewModel.hideViewAsSheet()
            },
            onViewAsSpecificUser = { 
                // TODO: Show user search dialog
                viewModel.setViewAsMode(ViewAsMode.SPECIFIC_USER, "User")
                viewModel.hideViewAsSheet()
            }
        )
    }

    if (state.showQrCode) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        QRCodeDialog(
            profileUrl = "https://synapse.app/profile/${profile?.username ?: ""}",
            username = profile?.username ?: "",
            onDismiss = { viewModel.hideQrCode() }
        )
    }

    if (state.showReportDialog) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        profile?.let {
            ReportUserDialog(
                username = it.username,
                onDismiss = { viewModel.hideReportDialog() },
                onReport = { reason -> viewModel.reportUser(it.id, reason) }
            )
        }
    }

    if (showCustomizationDialog) {
        ProfileInfoCustomizationDialog(
            onDismiss = { showCustomizationDialog = false },
            onNavigateToEditProfile = {
                showCustomizationDialog = false
                onNavigateToEditProfile()
            }
        )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileScreenState,
    profile: com.synapse.social.studioasinc.data.model.UserProfile,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scrollProgress: Float,
    viewModel: ProfileViewModel,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToFollowing: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onCustomizeClick: () -> Unit = {}
) {
    // Entry animation for content
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        contentVisible = true
    }
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "contentAlpha"
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
    ) {
        // View As Banner
        if (state.viewAsMode != null) {
            item {
                ViewAsBanner(
                    viewMode = state.viewAsMode,
                    specificUserName = state.viewAsUserName,
                    onExitViewAs = { viewModel.exitViewAs() }
                )
            }
        }

        // Enhanced Profile Header with Cover Photo
        item {
            ProfileHeader(
                profileImageUrl = profile.profileImageUrl,
                coverImageUrl = profile.coverImageUrl,
                name = profile.name,
                username = profile.username,
                nickname = profile.nickname,
                bio = profile.bio,
                isVerified = profile.isVerified,
                hasStory = false, // TODO: Implement story check
                postsCount = profile.postCount,
                followersCount = profile.followerCount,
                followingCount = profile.followingCount,
                isOwnProfile = state.isOwnProfile,
                isFollowing = state.isFollowing,
                scrollOffset = scrollProgress,
                onProfileImageClick = { /* TODO: Open full screen */ },
                onCoverPhotoClick = { /* TODO: Edit cover */ },
                onEditProfileClick = onNavigateToEditProfile,
                onFollowClick = {
                    if (state.isFollowing) {
                        viewModel.unfollowUser(profile.id)
                    } else {
                        viewModel.followUser(profile.id)
                    }
                },
                onMessageClick = { onNavigateToChat(profile.id) },
                onAddStoryClick = { /* TODO: Open story creation */ },
                onMoreClick = { viewModel.toggleMoreMenu() },
                onStatsClick = { stat ->
                    when (stat) {
                        "followers" -> onNavigateToFollowers()
                        "following" -> onNavigateToFollowing()
                    }
                }
            )
        }

        // Content Filter Bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ContentFilterBar(
                selectedFilter = state.contentFilter,
                onFilterSelected = { filter -> viewModel.switchContentFilter(filter) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Content Section with Crossfade Animation
        item {
            crossfadeContent(targetState = state.contentFilter) { filter ->
                when (filter) {
                    ProfileContentFilter.PHOTOS -> {
                        if (state.photos.isEmpty() && !state.isLoadingMore) {
                            EmptyState(
                                icon = Icons.Default.PhotoLibrary,
                                title = "No Photos",
                                message = "Photos you share will appear here."
                            )
                        } else {
                            val photos = remember(state.photos) { 
                                state.photos.filterIsInstance<MediaItem>()
                            }
                            PhotoGrid(
                                items = photos,
                                onItemClick = { /* TODO: Open photo viewer */ },
                                isLoading = state.isLoadingMore
                            )
                        }
                    }
                    ProfileContentFilter.POSTS -> {
                        Column {
                            // User Details Section
                            Spacer(modifier = Modifier.height(16.dp))
                            UserDetailsSection(
                                details = UserDetails(
                                    location = profile.location,
                                    joinedDate = formatJoinedDate(profile.joinedDate),
                                    relationshipStatus = profile.relationshipStatus,
                                    birthday = profile.birthday,
                                    work = profile.work,
                                    education = profile.education,
                                    website = profile.website,
                                    gender = profile.gender,
                                    pronouns = profile.pronouns,
                                    linkedAccounts = profile.linkedAccounts.map { 
                                        LinkedAccount(
                                            platform = it.platform,
                                            username = it.username
                                        )
                                    }
                                ),
                                isOwnProfile = state.isOwnProfile,
                                onCustomizeClick = onCustomizeClick,
                                onWebsiteClick = { /* TODO: Open website */ },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Following Section
                            FollowingSection(
                                users = emptyList(),
                                selectedFilter = FollowingFilter.ALL,
                                onFilterSelected = { },
                                onUserClick = { user -> onNavigateToUserProfile(user.id) },
                                onSeeAllClick = onNavigateToFollowing,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Posts Feed - Empty state shown in Column
                            if (state.posts.isEmpty() && !state.isLoadingMore) {
                                EmptyState(
                                    icon = Icons.Default.Article,
                                    title = "No Posts",
                                    message = "Posts will appear here when shared."
                                )
                            }
                        }
                    }
                    ProfileContentFilter.REELS -> {
                        if (state.reels.isEmpty() && !state.isLoadingMore) {
                            EmptyState(
                                icon = Icons.Default.VideoLibrary,
                                title = "No Reels",
                                message = "Reels you create will appear here."
                            )
                        } else {
                            // TODO: Implement ReelsGrid component
                            Text(
                                text = "Reels coming soon",
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // Posts items - added directly to parent LazyColumn
        if (state.contentFilter == ProfileContentFilter.POSTS && state.posts.isNotEmpty()) {
            val posts = state.posts.filterIsInstance<com.synapse.social.studioasinc.model.Post>()
            items(posts, key = { it.id }) { post ->
                // Context for profile actions
                val currentProfile = (state.profileState as? ProfileUiState.Success)?.profile
                
                AnimatedPostCard(
                    post = post,
                    currentProfile = currentProfile,
                    actions = PostActions(
                        onUserClick = { onNavigateToUserProfile(post.authorUid) },
                        onLike = { viewModel.toggleLike(post.id) },
                        onComment = { /* TODO: Navigate to comments */ },
                        onShare = { /* TODO: Share post */ },
                        onBookmark = { viewModel.toggleSave(post.id) },
                        onOptionClick = { /* TODO: Show menu */ },
                        onMediaClick = { /* TODO: Open media */ },
                        onPollVote = { p, idx -> viewModel.votePoll(p.id, idx) }
                    )
                )
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Animated post card with entrance animation.
 */
@Composable
private fun AnimatedPostCard(
    post: com.synapse.social.studioasinc.model.Post,
    currentProfile: com.synapse.social.studioasinc.data.model.UserProfile?,
    actions: PostActions
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "postAlpha"
    )
    
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "postOffset"
    )
    
    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
    ) {
        SharedPostItem(
            post = post,
            currentProfile = currentProfile,
            actions = actions
        )
    }
}

/**
 * Format joined date from timestamp to readable string.
 */
private fun formatJoinedDate(timestamp: Long): String {
    if (timestamp == 0L) return ""
    
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
    return format.format(date)
}
