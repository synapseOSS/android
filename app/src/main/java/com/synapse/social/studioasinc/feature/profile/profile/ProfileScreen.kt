package com.synapse.social.studioasinc.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.PostDetailActivity
import com.synapse.social.studioasinc.ui.components.EmptyState
import com.synapse.social.studioasinc.ui.components.ErrorState
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.components.MediaViewer
import com.synapse.social.studioasinc.ui.components.post.PostActions
import com.synapse.social.studioasinc.ui.components.post.PostCard
import com.synapse.social.studioasinc.ui.components.post.PostCardState
import com.synapse.social.studioasinc.ui.components.post.PostOptionsBottomSheet
import com.synapse.social.studioasinc.ui.components.post.SharedPostItem
import com.synapse.social.studioasinc.ui.profile.animations.crossfadeContent
import com.synapse.social.studioasinc.ui.profile.components.*
import com.synapse.social.studioasinc.ui.profile.components.UserSearchDialog
import com.synapse.social.studioasinc.domain.model.Post
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

    // Calculate effective ownership state based on View As mode
    // If in View As mode, we simulate NOT being the owner
    val effectiveIsOwnProfile = state.isOwnProfile && state.viewAsMode == null

    // Create an effective state for UI components to consume
    val effectiveState = state.copy(isOwnProfile = effectiveIsOwnProfile)

    val listState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }
    var showUserSearchDialog by remember { mutableStateOf(false) }

    // Media Viewer State
    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var initialMediaPage by remember { mutableStateOf(0) }

    // Post Options State
    var showPostOptions by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }

    val context = LocalContext.current

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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 100.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        ExpressiveLoadingIndicator()
                    }
                }
                is ProfileUiState.Success -> {
                    ProfileContent(
                        state = effectiveState,
                        profile = profileState.profile,
                        listState = listState,
                        scrollProgress = scrollProgress.value,
                        viewModel = viewModel,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToFollowers = onNavigateToFollowers,
                        onNavigateToFollowing = onNavigateToFollowing,
                        onNavigateToUserProfile = onNavigateToUserProfile,
                        onNavigateToChat = onNavigateToChat,
                        onCustomizeClick = { },
                        onOpenMediaViewer = { urls, index ->
                            selectedMediaUrls = urls
                            initialMediaPage = index
                            showMediaViewer = true
                        },
                        onShowPostOptions = { post ->
                            selectedPost = post
                            showPostOptions = true
                        }
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
            isOwnProfile = effectiveIsOwnProfile,
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
            onCopyLink = {
                val username = profile?.username ?: ""
                val url = "${BuildConfig.APP_DOMAIN}/profile/$username"
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("Profile Link", url)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(context, "Link copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
            },
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
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        ShareProfileBottomSheet(
            onDismiss = { viewModel.hideShareSheet() },
            onCopyLink = {
                val username = profile?.username ?: ""
                val url = "${BuildConfig.APP_DOMAIN}/profile/$username"
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("Profile Link", url)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(context, "Link copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                viewModel.hideShareSheet()
            },
            onShareToStory = {
                Toast.makeText(context, "Share to Story coming soon", Toast.LENGTH_SHORT).show()
                viewModel.hideShareSheet()
            },
            onShareViaMessage = {
                Toast.makeText(context, "Share via Message coming soon", Toast.LENGTH_SHORT).show()
                viewModel.hideShareSheet()
            },
            onShareExternal = {
                val username = profile?.username ?: ""
                val url = "${BuildConfig.APP_DOMAIN}/profile/$username"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Check out this profile: $url")
                }
                context.startActivity(Intent.createChooser(intent, "Share Profile"))
                viewModel.hideShareSheet()
            }
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
                showUserSearchDialog = true
                viewModel.hideViewAsSheet()
            }
        )
    }

    if (showUserSearchDialog) {
        UserSearchDialog(
            onDismiss = {
                showUserSearchDialog = false
                viewModel.clearSearchResults()
            },
            onUserSelected = { user ->
                showUserSearchDialog = false
                viewModel.clearSearchResults()
                viewModel.setViewAsMode(ViewAsMode.SPECIFIC_USER, user.username ?: "User")
            },
            onSearch = { query ->
                viewModel.searchUsers(query)
            },
            searchResults = state.searchResults,
            isSearching = state.isSearching
        )
    }

    if (state.showQrCode) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        QRCodeDialog(
            profileUrl = "${BuildConfig.APP_DOMAIN}/profile/${profile?.username ?: ""}",
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

    // Media Viewer Overlay
    if (showMediaViewer) {
        MediaViewer(
            mediaUrls = selectedMediaUrls,
            initialPage = initialMediaPage,
            onDismiss = { showMediaViewer = false }
        )
    }

    // Post Options Bottom Sheet
    if (showPostOptions && selectedPost != null) {
        val post = selectedPost!!
        PostOptionsBottomSheet(
            post = post,
            isOwner = (post.authorUid == currentUserId) && (state.viewAsMode == null),
            commentsDisabled = post.postDisableComments == "true",
            onDismiss = {
                showPostOptions = false
                selectedPost = null
            },
            onEdit = {
                Toast.makeText(context, "Edit post not implemented", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                viewModel.deletePost(post.id)
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Check out this post: ${BuildConfig.APP_DOMAIN}/post/${post.id}")
                }
                context.startActivity(Intent.createChooser(intent, "Share Post"))
            },
            onCopyLink = {
                val url = "${BuildConfig.APP_DOMAIN}/post/${post.id}"
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("Post Link", url)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onBookmark = {
                viewModel.toggleSave(post.id)
            },
            onToggleComments = {
                Toast.makeText(context, "Toggle comments not implemented", Toast.LENGTH_SHORT).show()
            },
            onReport = {
                viewModel.reportPost(post.id, "Reported from profile")
                Toast.makeText(context, "Report submitted", Toast.LENGTH_SHORT).show()
            },
            onBlock = {
                viewModel.blockUser(post.authorUid)
            },
            onRevokeVote = {
                Toast.makeText(context, "Revoke vote not implemented", Toast.LENGTH_SHORT).show()
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
    onCustomizeClick: () -> Unit = {},
    onOpenMediaViewer: (List<String>, Int) -> Unit,
    onShowPostOptions: (Post) -> Unit
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

    val context = LocalContext.current

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
                avatar = profile.avatar,
                coverImageUrl = profile.coverImageUrl,
                name = profile.name,
                username = profile.username,
                nickname = profile.nickname,
                bio = profile.bio,
                isVerified = profile.isVerified,
                hasStory = state.hasStory,
                postsCount = profile.postCount,
                followersCount = profile.followerCount,
                followingCount = profile.followingCount,
                isOwnProfile = state.isOwnProfile && state.viewAsMode == null,
                isFollowing = state.isFollowing,
                isFollowLoading = state.isFollowLoading,
                scrollOffset = scrollProgress,
                onProfileImageClick = {
                     if (state.isOwnProfile) {
                         onNavigateToEditProfile()
                     } else if (!profile.avatar.isNullOrBlank()) {
                         onOpenMediaViewer(listOf(profile.avatar), 0)
                     }
                },
                onCoverPhotoClick = {
                     if (state.isOwnProfile) {
                         onNavigateToEditProfile()
                     } else if (!profile.coverImageUrl.isNullOrBlank()) {
                         onOpenMediaViewer(listOf(profile.coverImageUrl), 0)
                     }
                },
                onEditProfileClick = onNavigateToEditProfile,
                onFollowClick = {
                    if (state.isFollowing) {
                        viewModel.unfollowUser(profile.id)
                    } else {
                        viewModel.followUser(profile.id)
                    }
                },
                onMessageClick = { onNavigateToChat(profile.id) },
                onAddStoryClick = {
                    Toast.makeText(context, "Story creation coming soon", Toast.LENGTH_SHORT).show()
                },
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
                modifier = Modifier.fillMaxWidth(),
                showLabels = true
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
                                onItemClick = { mediaItem ->
                                    // Construct list of URLs for viewer
                                    val allUrls = photos.map { it.url }
                                    val index = photos.indexOf(mediaItem)
                                    onOpenMediaViewer(allUrls, if (index >= 0) index else 0)
                                },
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
                                onWebsiteClick = { url ->
                                     try {
                                         val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                         context.startActivity(intent)
                                     } catch (e: Exception) {
                                         Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                     }
                                },
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
                                    icon = Icons.AutoMirrored.Filled.Article,
                                    title = if (state.isOwnProfile) "No Posts Yet" else "No Posts",
                                    message = if (state.isOwnProfile)
                                        "Share your first post with the world!"
                                    else
                                        "This user hasn't posted anything yet."
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
                            val reels = remember(state.reels) {
                                state.reels.filterIsInstance<MediaItem>()
                            }
                            ReelsGrid(
                                items = reels,
                                onItemClick = {
                                    Toast.makeText(context, "Reels viewer coming soon", Toast.LENGTH_SHORT).show()
                                },
                                isLoading = state.isLoadingMore
                            )
                        }
                    }
                }
            }
        }

        // Posts items - added directly to parent LazyColumn
        if (state.contentFilter == ProfileContentFilter.POSTS && state.posts.isNotEmpty()) {
            val posts = state.posts.filterIsInstance<com.synapse.social.studioasinc.domain.model.Post>()
            items(posts, key = { it.id }) { post ->
                // Context for profile actions
                val currentProfile = (state.profileState as? ProfileUiState.Success)?.profile
                
                AnimatedPostCard(
                    post = post,
                    currentProfile = currentProfile,
                    actions = PostActions(
                        onUserClick = { onNavigateToUserProfile(post.authorUid) },
                        onLike = { viewModel.toggleLike(post.id) },
                        onComment = { selectedPost ->
                            val intent = Intent(context, PostDetailActivity::class.java).apply {
                                putExtra(PostDetailActivity.EXTRA_POST_ID, selectedPost.id)
                                putExtra(PostDetailActivity.EXTRA_AUTHOR_UID, selectedPost.authorUid)
                            }
                            context.startActivity(intent)
                        },
                        onShare = { selectedPost ->
                             val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Check out this post: ${BuildConfig.APP_DOMAIN}/post/${selectedPost.id}")
                             }
                             context.startActivity(Intent.createChooser(intent, "Share Post"))
                        },
                        onBookmark = { viewModel.toggleSave(post.id) },
                        onOptionClick = { onShowPostOptions(post) },
                        onMediaClick = { index ->
                            val urls = post.mediaItems?.mapNotNull { it.url } ?: listOfNotNull(post.postImage)
                            if (urls.isNotEmpty()) {
                                onOpenMediaViewer(urls, index)
                            }
                        },
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
    post: com.synapse.social.studioasinc.domain.model.Post,
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
