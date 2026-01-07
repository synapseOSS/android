package com.synapse.social.studioasinc.ui.postdetail

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.synapse.social.studioasinc.data.local.database.AppDatabase
import com.synapse.social.studioasinc.data.repository.*
import com.synapse.social.studioasinc.domain.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth

class PostDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val postDetailRepository = PostDetailRepository()
    private val commentRepository = CommentRepository(AppDatabase.getDatabase(application).commentDao())
    private val reactionRepository = ReactionRepository()
    private val pollRepository = PollRepository()
    private val bookmarkRepository = BookmarkRepository()
    private val reshareRepository = ReshareRepository()
    private val reportRepository = ReportRepository()
    private val userRepository = UserRepository(AppDatabase.getDatabase(application).userDao())

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _commentsPagingFlow = MutableStateFlow<Flow<PagingData<CommentWithUser>>>(emptyFlow())
    val commentsPagingFlow: StateFlow<Flow<PagingData<CommentWithUser>>> = _commentsPagingFlow.asStateFlow()

    private var currentPostId: String? = null

    init {
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        _uiState.update { it.copy(currentUserId = currentUser?.id) }
    }

    fun loadPost(postId: String) {
        // Only initialize pager if it's a new post or flow is empty
        if (currentPostId != postId) {
            currentPostId = postId
            val pagingSource = CommentPagingSource(commentRepository, postId)
            val flow = Pager(
                PagingConfig(pageSize = 20)
            ) {
                 CommentPagingSource(commentRepository, postId)
            }.flow.cachedIn(viewModelScope)

            _commentsPagingFlow.value = flow
        }

        viewModelScope.launch {
            // Only show global loading if we don't have the post data yet
            if (_uiState.value.post == null) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            postDetailRepository.getPostWithDetails(postId).fold(
                onSuccess = { postDetail ->
                    _uiState.update { it.copy(isLoading = false, post = postDetail) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to load post") }
                }
            )
            postDetailRepository.incrementViewCount(postId)
        }
    }

    fun refreshComments() {
        val postId = currentPostId ?: return
        // For backwards compatibility or full reload if needed, but prefer refreshCommentsList()
        // Here we just reload post details and refresh list, without full screen loader
        viewModelScope.launch {
            postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPost ->
                _uiState.update { it.copy(post = updatedPost) }
            }
        }
        refreshCommentsList()
    }

    private fun refreshCommentsList() {
        _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
    }

    fun toggleReaction(reactionType: ReactionType) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            reactionRepository.toggleReaction(postId, "post", reactionType).onSuccess {
                 postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPost ->
                     _uiState.update { it.copy(post = updatedPost) }
                 }
            }
        }
    }

    fun toggleCommentReaction(commentId: String, reactionType: ReactionType) {
        val postId = currentPostId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading + commentId) }

            reactionRepository.toggleReaction(commentId, "comment", reactionType).onSuccess {
                 refreshCommentsList()
                 // We also need to refresh post details for reaction summary if needed, but comments list is main priority.
            }.also {
                _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading - commentId) }
            }
        }
    }

    // Overload for optimistic update if we had the comment object
    // For now, sticking to the loading indicator plan for "Like" to be safe and consistent with constraints.

    private var isSubmittingComment = false
    
    fun addComment(content: String) {
        if (isSubmittingComment) return
        val postId = currentPostId ?: return
        val parentId = _uiState.value.replyToComment?.id
        
        isSubmittingComment = true
        viewModelScope.launch {
            commentRepository.createComment(postId, content, null, parentId).onSuccess {
                refreshComments() // This refreshes post (for comment count) and list
                setReplyTo(null)
            }.also {
                isSubmittingComment = false
            }
        }
    }

    fun setReplyTo(comment: CommentWithUser?) {
        _uiState.update { it.copy(replyToComment = comment, editingComment = null) }
    }

    fun setEditingComment(comment: CommentWithUser?) {
        _uiState.update { it.copy(editingComment = comment, replyToComment = null) }
    }

    fun deleteComment(commentId: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
             _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading + commentId) }
            commentRepository.deleteComment(commentId).onSuccess {
                refreshComments()
            }.also {
                _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading - commentId) }
            }
        }
    }

    fun editComment(commentId: String, content: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading + commentId) }
            commentRepository.editComment(commentId, content).onSuccess {
                refreshCommentsList()
                setEditingComment(null)
            }.also {
                _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading - commentId) }
            }
        }
    }

    fun votePoll(optionIndex: Int) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            pollRepository.submitVote(postId, optionIndex).onSuccess {
                postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPost ->
                     _uiState.update { it.copy(post = updatedPost) }
                 }
            }
        }
    }

    fun revokeVote() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            pollRepository.revokeVote(postId).onSuccess {
                postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPost ->
                     _uiState.update { it.copy(post = updatedPost) }
                 }
            }
        }
    }

    fun toggleBookmark() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            bookmarkRepository.toggleBookmark(postId, null).onSuccess {
                postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPost ->
                     _uiState.update { it.copy(post = updatedPost) }
                 }
            }
        }
    }

    fun createReshare(commentary: String?) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            reshareRepository.createReshare(postId, commentary).onSuccess {
                postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPost ->
                    _uiState.update { it.copy(post = updatedPost) }
                }
            }
        }
    }

    fun reportPost(reason: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch { reportRepository.createReport(postId, reason, null) }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postDetailRepository.deletePost(postId)
        }
    }

    fun toggleComments() {
        // ...
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
             // Block user logic
        }
    }

    fun hideComment(commentId: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading + commentId) }
            commentRepository.hideComment(commentId).onSuccess {
                refreshCommentsList()
            }.also {
                _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading - commentId) }
            }
        }
    }

    fun pinComment(commentId: String, postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading + commentId) }
            commentRepository.pinComment(commentId, postId).onSuccess {
                refreshCommentsList()
            }.also {
                _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading - commentId) }
            }
        }
    }

    fun reportComment(commentId: String, reason: String, description: String?) {
        viewModelScope.launch {
            commentRepository.reportComment(commentId, reason)
        }
    }

    fun copyLink(postId: String, context: Context) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText("Post Link", "synapse://post/$postId")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
    }

    fun loadReplies(commentId: String) {
        if (_uiState.value.replyLoading.contains(commentId)) return

        viewModelScope.launch {
            _uiState.update { it.copy(replyLoading = it.replyLoading + commentId) }
            commentRepository.getReplies(commentId).fold(
                onSuccess = { replies ->
                    _uiState.update {
                        it.copy(
                            replies = it.replies + (commentId to replies),
                            replyLoading = it.replyLoading - commentId
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(replyLoading = it.replyLoading - commentId) }
                }
            )
        }
    }
}
