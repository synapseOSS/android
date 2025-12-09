package com.synapse.social.studioasinc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.*
import com.synapse.social.studioasinc.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PostDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val postDetailRepository = PostDetailRepository()
    private val commentRepository = CommentRepository(AppDatabase.getDatabase(application).commentDao())
    private val reactionRepository = ReactionRepository()
    private val pollRepository = PollRepository()
    private val bookmarkRepository = BookmarkRepository()
    private val reshareRepository = ReshareRepository()
    private val reportRepository = ReportRepository()

    private val _postState = MutableStateFlow<PostDetailState>(PostDetailState.Loading)
    val postState: StateFlow<PostDetailState> = _postState.asStateFlow()

    private val _commentsState = MutableStateFlow<CommentsState>(CommentsState.Loading)
    val commentsState: StateFlow<CommentsState> = _commentsState.asStateFlow()

    private var currentPostId: String? = null

    fun loadPost(postId: String) {
        currentPostId = postId
        viewModelScope.launch {
            _postState.value = PostDetailState.Loading
            postDetailRepository.getPostWithDetails(postId).fold(
                onSuccess = { _postState.value = PostDetailState.Success(it) },
                onFailure = { _postState.value = PostDetailState.Error(it.message ?: "Failed to load") }
            )
            postDetailRepository.incrementViewCount(postId)
        }
    }

    fun loadComments(postId: String, limit: Int = 20, offset: Int = 0) {
        viewModelScope.launch {
            _commentsState.value = CommentsState.Loading
            commentRepository.getComments(postId).collect { result ->
                result.fold(
                    onSuccess = { comments ->
                        val commentsWithUser = comments.map { comment ->
                            CommentWithUser(
                                id = comment.key,
                                postId = comment.postKey,
                                userId = comment.uid,
                                parentCommentId = comment.replyCommentKey,
                                content = comment.comment,
                                createdAt = comment.push_time,
                                user = null
                            )
                        }
                        _commentsState.value = CommentsState.Success(commentsWithUser, commentsWithUser.size >= limit)
                    },
                    onFailure = { _commentsState.value = CommentsState.Error(it.message ?: "Failed") }
                )
            }
        }
    }

    fun toggleReaction(reactionType: ReactionType) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            reactionRepository.togglePostReaction(postId, reactionType).onSuccess { loadPost(postId) }
        }
    }

    fun toggleCommentReaction(commentId: String, reactionType: ReactionType) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            reactionRepository.toggleCommentReaction(commentId, reactionType).onSuccess { loadComments(postId) }
        }
    }

    fun addComment(content: String, parentCommentId: String? = null) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            commentRepository.createComment(postId, content, null, parentCommentId).onSuccess { loadComments(postId) }
        }
    }

    fun deleteComment(commentId: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            commentRepository.deleteComment(commentId).onSuccess { loadComments(postId) }
        }
    }

    fun editComment(commentId: String, content: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            commentRepository.editComment(commentId, content).onSuccess { loadComments(postId) }
        }
    }

    fun votePoll(optionIndex: Int) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            pollRepository.submitVote(postId, optionIndex).onSuccess { loadPost(postId) }
        }
    }

    fun toggleBookmark() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            bookmarkRepository.toggleBookmark(postId, null).onSuccess { loadPost(postId) }
        }
    }

    fun createReshare(commentary: String?) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            reshareRepository.createReshare(postId, commentary).onSuccess { loadPost(postId) }
        }
    }

    fun reportPost(reason: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch { reportRepository.createReport(postId, reason, null) }
    }
    
    fun pinComment(commentId: String, postId: String) {
        viewModelScope.launch {
            commentRepository.pinComment(commentId, postId).onSuccess { loadComments(postId) }
        }
    }
    
    fun hideComment(commentId: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            commentRepository.hideComment(commentId).onSuccess { loadComments(postId) }
        }
    }
    
    fun reportComment(commentId: String, reason: String, description: String?) {
        viewModelScope.launch {
            commentRepository.reportComment(commentId, reason)
        }
    }
}
