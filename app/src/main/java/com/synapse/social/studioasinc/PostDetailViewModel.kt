package com.synapse.social.studioasinc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.*
import com.synapse.social.studioasinc.model.*
import com.synapse.social.studioasinc.util.Logger
import com.synapse.social.studioasinc.util.logd
import com.synapse.social.studioasinc.util.loge
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

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

    private val _repliesState = MutableStateFlow<Map<String, List<CommentWithUser>>>(emptyMap())
    val repliesState: StateFlow<Map<String, List<CommentWithUser>>> = _repliesState.asStateFlow()

    private val _replyLoadingState = MutableStateFlow<Set<String>>(emptySet())
    val replyLoadingState: StateFlow<Set<String>> = _replyLoadingState.asStateFlow()

    private var currentPostId: String? = null

    fun loadPost(postId: String) {
        currentPostId = postId
        logd("Loading post: $postId")
        viewModelScope.launch {
            _postState.value = PostDetailState.Loading
            postDetailRepository.getPostWithDetails(postId).fold(
                onSuccess = { 
                    logd("Successfully loaded post: $postId")
                    _postState.value = PostDetailState.Success(it) 
                },
                onFailure = { 
                    loge("Failed to load post: $postId - ${it.message}", it)
                    _postState.value = PostDetailState.Error(it.message ?: "Failed to load") 
                }
            )
            postDetailRepository.incrementViewCount(postId)
        }
    }

    fun loadComments(postId: String, limit: Int = 20, offset: Int = 0) {
        viewModelScope.launch {
            _commentsState.value = CommentsState.Loading
            commentRepository.fetchComments(postId, limit, offset).fold(
                onSuccess = { comments ->
                    _commentsState.value = CommentsState.Success(comments, comments.size >= limit)
                },
                onFailure = { _commentsState.value = CommentsState.Error(it.message ?: "Failed to load comments") }
            )
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
        Logger.d("Deleting comment: $commentId", "PostDetailViewModel")
        viewModelScope.launch {
            commentRepository.deleteComment(commentId).fold(
                onSuccess = { 
                    Logger.d("Successfully deleted comment: $commentId", "PostDetailViewModel")
                    loadComments(postId) 
                },
                onFailure = { Logger.e("Failed to delete comment: $commentId - ${it.message}", it, "PostDetailViewModel") }
            )
        }
    }

    fun editComment(commentId: String, content: String) {
        val postId = currentPostId ?: return
        Logger.d("Editing comment: $commentId", "PostDetailViewModel")
        viewModelScope.launch {
            commentRepository.editComment(commentId, content).fold(
                onSuccess = { 
                    Logger.d("Successfully edited comment: $commentId", "PostDetailViewModel")
                    loadComments(postId) 
                },
                onFailure = { Logger.e("Failed to edit comment: $commentId - ${it.message}", it, "PostDetailViewModel") }
            )
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
    
    fun loadReplies(commentId: String) {
        viewModelScope.launch {
            // Add to loading state
            _replyLoadingState.value = _replyLoadingState.value + commentId
            
            logd("Loading replies for comment: $commentId")
            commentRepository.getReplies(commentId).fold(
                onSuccess = { replies -> 
                    logd("Successfully loaded ${replies.size} replies")
                    // Update replies state
                    _repliesState.value = _repliesState.value + (commentId to replies)
                    // Remove from loading state
                    _replyLoadingState.value = _replyLoadingState.value - commentId
                },
                onFailure = { error ->
                    loge("Failed to load replies: ${error.message}", error)
                    // Remove from loading state even on failure
                    _replyLoadingState.value = _replyLoadingState.value - commentId
                }
            )
        }
    }
}
