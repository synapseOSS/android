package com.synapse.social.studioasinc.ui.postdetail

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.synapse.social.studioasinc.domain.model.CommentAction
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.ui.postdetail.components.*
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.components.post.PostInteractionBar
import com.synapse.social.studioasinc.ui.components.post.PollContent
import com.synapse.social.studioasinc.ui.components.post.PollOption
import com.synapse.social.studioasinc.ui.components.post.PostOptionsBottomSheet
import com.synapse.social.studioasinc.ui.components.post.ReactionPicker
import com.synapse.social.studioasinc.domain.model.User as HomeUser
import com.synapse.social.studioasinc.ui.components.MediaViewer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: PostDetailViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagingItems = viewModel.commentsPagingFlow.collectAsState().value.collectAsLazyPagingItems()

    // Observe refresh trigger to reload list without full screen reload
    LaunchedEffect(uiState.refreshTrigger) {
        if (uiState.refreshTrigger > 0) {
            pagingItems.refresh()
        }
    }

    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableStateOf(0) }
    var showPostOptions by remember { mutableStateOf(false) }
    var showCommentOptions by remember { mutableStateOf<CommentWithUser?>(null) }
    var showReactionPickerForComment by remember { mutableStateOf<CommentWithUser?>(null) }
    var showReactionPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentUserId = uiState.currentUserId
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    LaunchedEffect(uiState.replyToComment, uiState.editingComment) {
        if (uiState.replyToComment != null || uiState.editingComment != null) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    if (showMediaViewer && uiState.post?.post?.mediaItems?.isNotEmpty() == true) {
        MediaViewer(
            mediaUrls = uiState.post!!.post.mediaItems!!.map { it.url },
            initialPage = selectedMediaIndex,
            onDismiss = { showMediaViewer = false }
        )
    }

    if (showReactionPickerForComment != null) {
        ReactionPicker(
            onReactionSelected = { reaction ->
                viewModel.toggleCommentReaction(showReactionPickerForComment!!.id, reaction)
                showReactionPickerForComment = null
            },
            onDismiss = { showReactionPickerForComment = null }
        )
    }
    
    if (showReactionPicker) {
        ReactionPicker(
            onReactionSelected = { reaction ->
                viewModel.toggleReaction(reaction)
                showReactionPicker = false
            },
            onDismiss = { showReactionPicker = false }
        )
    }

    if (showPostOptions && uiState.post != null) {
        val post = uiState.post!!.post
        PostOptionsBottomSheet(
            post = post,
            isOwner = post.authorUid == currentUserId,
            onDismiss = { showPostOptions = false },
            onEdit = { /* Navigate to edit */ },
            onDelete = {
                 viewModel.deletePost(postId)
                 onNavigateBack()
            },
            onShare = {
                 val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Check out this post on Synapse: synapse://post/$postId")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Post"))
            },
            onCopyLink = {
                 viewModel.copyLink(postId, context)
            },
            onBookmark = { viewModel.toggleBookmark() },
            onReshare = { viewModel.createReshare(null) },
            onToggleComments = { viewModel.toggleComments() },
            onReport = { viewModel.reportPost("Spam") },
            onBlock = {
                post.authorUid?.let { viewModel.blockUser(it) }
            },
            onRevokeVote = { viewModel.revokeVote() }
        )
    }

    if (showCommentOptions != null) {
        val comment = showCommentOptions!!
        CommentOptionsBottomSheet(
            comment = comment,
            isOwnComment = comment.userId == currentUserId,
            isPostAuthor = uiState.post?.post?.authorUid == currentUserId,
            onDismiss = { showCommentOptions = null },
            onAction = { action ->
                when (action) {
                    is CommentAction.Reply -> viewModel.setReplyTo(comment)
                    is CommentAction.Delete -> viewModel.deleteComment(action.commentId)
                    is CommentAction.Edit -> viewModel.setEditingComment(comment)
                    is CommentAction.Report -> viewModel.reportComment(action.commentId, action.reason, action.description)
                    is CommentAction.Pin -> viewModel.pinComment(action.commentId, action.postId)
                    is CommentAction.Hide -> viewModel.hideComment(action.commentId)
                    is CommentAction.Share -> {
                         val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Check out this comment: ${action.content}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Comment"))
                    }
                    is CommentAction.Copy -> {
                        viewModel.copyLink(postId, context) // Actually copy comment content
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        bottomBar = {
             Column {
                 if (uiState.replyToComment != null) {
                     ReplyIndicator(
                         replyTo = uiState.replyToComment!!,
                         onCancelReply = { viewModel.setReplyTo(null) }
                     )
                 }
                 if (uiState.editingComment != null) {
                     EditIndicator(
                         comment = uiState.editingComment!!,
                         onCancel = { viewModel.setEditingComment(null) }
                     )
                 }
                 CommentInput(
                     onSend = {
                         if (uiState.editingComment != null) {
                             viewModel.editComment(uiState.editingComment!!.id, it)
                         } else {
                             viewModel.addComment(it)
                         }
                     },
                     initialValue = uiState.editingComment?.content ?: "",
                     focusRequester = focusRequester
                 )
             }
        }
    ) { paddingValues ->
        // Only show full screen loader if we have no data and are loading
        if (uiState.isLoading && uiState.post == null) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                 ExpressiveLoadingIndicator()
             }
        } else if (uiState.error != null && uiState.post == null) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                 Text("Error: ${uiState.error}")
             }
        } else {
             val postDetail = uiState.post
             if (postDetail != null) {
                 CommentsList(
                     comments = pagingItems,
                     repliesState = uiState.replies,
                     replyLoadingState = uiState.replyLoading,
                     commentActionsLoading = uiState.commentActionsLoading,
                     onReplyClick = { viewModel.setReplyTo(it) },
                     onLikeClick = { viewModel.toggleCommentReaction(it, ReactionType.LIKE) },
                     onShowReactions = { showReactionPickerForComment = it },
                     onShowOptions = { showCommentOptions = it },
                     onUserClick = onNavigateToProfile,
                     onViewReplies = { commentId: String -> viewModel.loadReplies(commentId) },
                     modifier = Modifier.padding(paddingValues),
                     headerContent = {
                         Column {
                             // Header
                             val author = postDetail.author
                             if (author != null) {
                                 val homeUser = HomeUser(
                                     uid = author.uid,
                                     username = author.username ?: "Unknown",
                                     avatar = author.avatar,
                                     verify = author.isVerified == true,
                                     // Removed bio, location, website mapping as they don't match Home User definition
                                     // and aren't displayed in PostHeader anyway.
                                 )
                                 PostDetailHeader(
                                     user = homeUser,
                                     timestamp = com.synapse.social.studioasinc.core.util.TimeUtils.getTimeAgo(postDetail.post.publishDate ?: ""),
                                     onUserClick = { onNavigateToProfile(author.uid) },
                                     onOptionsClick = { showPostOptions = true }
                                 )
                             }

                             // Content
                             Text(
                                 text = postDetail.post.postText ?: "",
                                 style = MaterialTheme.typography.bodyLarge,
                                 modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                             )

                             // Media
                             if (!postDetail.post.mediaItems.isNullOrEmpty()) {
                                 postDetail.post.mediaItems?.forEachIndexed { index, mediaItem ->
                                     Box(
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .padding(vertical = 4.dp)
                                             .clickable {
                                                 selectedMediaIndex = index
                                                 showMediaViewer = true
                                             }
                                     ) {
                                         AsyncImage(
                                             model = mediaItem.url,
                                             contentDescription = "Post Media",
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .wrapContentHeight(),
                                             contentScale = ContentScale.FillWidth
                                         )

                                         if (postDetail.post.postType == "video") {
                                             Icon(
                                                 imageVector = Icons.Default.PlayCircle,
                                                 contentDescription = "Play Video",
                                                 modifier = Modifier
                                                     .align(Alignment.Center)
                                                     .size(48.dp),
                                                 tint = androidx.compose.ui.graphics.Color.White
                                             )
                                         }
                                     }
                                 }
                             }

                             // Poll
                             if (postDetail.post.hasPoll == true) {
                                 val totalVotes = postDetail.pollResults?.sumOf { it.voteCount } ?: 0
                                 val userVote = postDetail.userPollVote
                                 val options = postDetail.pollResults?.map {
                                     PollOption(
                                         id = it.index.toString(), // Mapped from PollOptionResult.index
                                         text = it.text,
                                         voteCount = it.voteCount,
                                         isSelected = userVote == it.index
                                     )
                                 } ?: emptyList()

                                 PollContent(
                                     question = postDetail.post.pollQuestion ?: "",
                                     options = options,
                                     totalVotes = totalVotes,
                                     hasVoted = userVote != null,
                                     onVote = { index -> viewModel.votePoll(index.toIntOrNull() ?: 0) }
                                 )
                             }

                             // Interaction Bar
                             PostInteractionBar(
                                 isLiked = postDetail.userReaction == ReactionType.LIKE,
                                 likeCount = postDetail.reactionSummary.values.sum(),
                                 commentCount = postDetail.post.commentsCount,
                                 isBookmarked = false,
                                 hideLikeCount = postDetail.post.postHideLikeCount == "true",
                                 onLikeClick = { viewModel.toggleReaction(ReactionType.LIKE) },
                                 onCommentClick = { /* Focus input */ },
                                 onShareClick = {
                                     val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Check out this post on Synapse: synapse://post/$postId")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                                 },
                                 onBookmarkClick = { viewModel.toggleBookmark() },
                                 onReactionLongPress = { showReactionPicker = true }
                             )

                             @Suppress("DEPRECATION")
                             Divider()
                         }
                     }
                 )
             }
        }
    }
}
