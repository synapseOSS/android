package com.synapse.social.studioasinc

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.model.Comment
import com.synapse.social.studioasinc.model.Reply
import com.synapse.social.studioasinc.model.User
import com.synapse.social.studioasinc.util.CommentMediaUploader
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.launch

class PostCommentsViewModel : ViewModel() {

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val _replies = MutableLiveData<List<Reply>>()
    val replies: LiveData<List<Reply>> = _replies

    private val _commentCount = MutableLiveData<Long>()
    val commentCount: LiveData<Long> = _commentCount

    private val _userAvatar = MutableLiveData<String?>()
    val userAvatar: LiveData<String?> = _userAvatar

    private val _userData = MutableLiveData<Map<String, User>>()
    val userData: LiveData<Map<String, User>> = _userData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val dbService = SupabaseDatabaseService()
    private val authService = SupabaseAuthenticationService()
    private var commentsLimit = 20

    fun getComments(postKey: String, increaseLimit: Boolean = false) {
        if (increaseLimit) {
            commentsLimit += 20
        }

        viewModelScope.launch {
            try {
                // Fetch all comments (including replies) for the post
                val commentsResult = dbService.selectWithFilter(
                    table = "comments",
                    columns = "*",
                    filter = "post_id",
                    value = postKey
                )
                
                val commentsList = mutableListOf<Comment>()
                val repliesList = mutableListOf<Reply>()
                val uids = mutableListOf<String>()
                
                commentsResult.getOrNull()?.forEach { commentData ->
                    // Skip deleted comments
                    if (commentData["deleted_at"] != null) return@forEach
                    
                    val parentCommentId = commentData["parent_comment_id"] as? String
                    
                    if (parentCommentId == null) {
                        // This is a top-level comment
                        val comment = Comment(
                            uid = commentData["user_id"] as? String ?: "",
                            comment = commentData["content"] as? String ?: "",
                            push_time = commentData["created_at"] as? String ?: "",
                            key = commentData["id"] as? String ?: "",
                            like = (commentData["likes_count"] as? Number)?.toLong() ?: 0L,
                            postKey = commentData["post_id"] as? String ?: ""
                        )
                        commentsList.add(comment)
                        comment.uid.let { uids.add(it) }
                    } else {
                        // This is a reply
                        val reply = Reply(
                            uid = commentData["user_id"] as? String ?: "",
                            comment = commentData["content"] as? String ?: "",
                            push_time = commentData["created_at"] as? String ?: "",
                            key = commentData["id"] as? String ?: "",
                            like = (commentData["likes_count"] as? Number)?.toLong() ?: 0L,
                            replyCommentkey = parentCommentId
                        )
                        repliesList.add(reply)
                        reply.uid.let { uids.add(it) }
                    }
                }
                
                _comments.postValue(commentsList.sortedByDescending { it.like ?: 0L })
                _replies.postValue(repliesList.sortedByDescending { it.like ?: 0L })
                fetchUsersData(uids)
            } catch (e: Exception) {
                _error.postValue("Failed to load comments: ${e.message}")
                _comments.postValue(emptyList())
            }
        }

    }

    fun getReplies(postKey: String, commentKey: String) {
        viewModelScope.launch {
            try {
                // Fetch replies from comments table where parent_comment_id matches
                val repliesResult = dbService.selectWithFilter(
                    table = "comments",
                    columns = "*",
                    filter = "parent_comment_id",
                    value = commentKey
                )
                
                val repliesList = mutableListOf<Reply>()
                val uids = mutableListOf<String>()
                
                repliesResult.getOrNull()?.forEach { replyData ->
                    // Skip deleted replies
                    if (replyData["deleted_at"] != null) return@forEach
                    
                    // Convert map to Reply object from comments table
                    val reply = Reply(
                        uid = replyData["user_id"] as? String ?: "",
                        comment = replyData["content"] as? String ?: "",
                        push_time = replyData["created_at"] as? String ?: "",
                        key = replyData["id"] as? String ?: "",
                        like = (replyData["likes_count"] as? Number)?.toLong() ?: 0L,
                        replyCommentkey = replyData["parent_comment_id"] as? String ?: ""
                    )
                    repliesList.add(reply)
                    reply.uid.let { uids.add(it) }
                }
                
                _replies.postValue(repliesList.sortedByDescending { it.like ?: 0L })
                fetchUsersData(uids)
            } catch (e: Exception) {
                _error.postValue("Failed to load replies: ${e.message}")
                _replies.postValue(emptyList())
            }
        }

    }

    private fun fetchUsersData(uids: List<String>) {
        viewModelScope.launch {
            try {
                val userMap = mutableMapOf<String, User>()
                uids.distinct().forEach { uid ->
                    val usersResult = dbService.selectWithFilter(
                        table = "users",
                        columns = "*",
                        filter = "uid",
                        value = uid
                    )
                    
                    usersResult.getOrNull()?.firstOrNull()?.let { userData ->
                        val user = User(
                            id = userData["id"] as? String,
                            uid = userData["uid"] as? String ?: uid,
                            email = userData["email"] as? String,
                            username = userData["username"] as? String,
                            nickname = userData["nickname"] as? String,
                            avatar = userData["avatar"] as? String
                        )
                        userMap[uid] = user
                    }
                }
                _userData.postValue(userMap)
            } catch (e: Exception) {
                _error.postValue("Failed to fetch user data: ${e.message}")
            }
        }
    }

    fun getCommentCount(postKey: String) {
        viewModelScope.launch {
            try {
                val postResult = dbService.selectWithFilter(
                    table = "posts",
                    columns = "comments_count",
                    filter = "id",
                    value = postKey
                )
                val count = postResult.getOrNull()?.firstOrNull()?.get("comments_count") as? Number
                _commentCount.postValue(count?.toLong() ?: 0L)
            } catch (e: Exception) {
                _error.postValue("Failed to get comment count: ${e.message}")
            }
        }
    }

    fun getUserData(uid: String) {
        viewModelScope.launch {
            try {
                val usersResult = dbService.selectWithFilter(
                    table = "users",
                    columns = "avatar",
                    filter = "uid",
                    value = uid
                )
                
                val avatar = usersResult.getOrNull()?.firstOrNull()?.get("avatar") as? String
                _userAvatar.postValue(avatar)
            } catch (e: Exception) {
                _error.postValue("Failed to get user data: ${e.message}")
                _userAvatar.postValue(null)
            }
        }
    }

    private val _uploadProgress = MutableLiveData<Boolean>()
    val uploadProgress: LiveData<Boolean> = _uploadProgress

    fun postComment(
        context: Context,
        postKey: String,
        commentText: String,
        isReply: Boolean,
        replyToCommentKey: String? = null,
        mediaUri: Uri? = null,
        mediaType: String? = null
    ) {
        viewModelScope.launch {
            try {
                _uploadProgress.postValue(true)
                val currentUid = authService.getCurrentUserId() ?: return@launch
                
                var photoUrl: String? = null
                var videoUrl: String? = null
                var audioUrl: String? = null
                var finalMediaType: String? = null
                
                // Upload media if provided
                mediaUri?.let { uri ->
                    when (mediaType) {
                        "photo" -> {
                            when (val result = CommentMediaUploader.uploadPhoto(context, uri)) {
                                is CommentMediaUploader.UploadResult.Success -> {
                                    photoUrl = result.url
                                    finalMediaType = "photo"
                                }
                                is CommentMediaUploader.UploadResult.Error -> {
                                    _error.postValue(result.message)
                                    _uploadProgress.postValue(false)
                                    return@launch
                                }
                            }
                        }
                        "video" -> {
                            when (val result = CommentMediaUploader.uploadVideo(context, uri)) {
                                is CommentMediaUploader.UploadResult.Success -> {
                                    videoUrl = result.url
                                    finalMediaType = "video"
                                }
                                is CommentMediaUploader.UploadResult.Error -> {
                                    _error.postValue(result.message)
                                    _uploadProgress.postValue(false)
                                    return@launch
                                }
                            }
                        }
                        "audio" -> {
                            when (val result = CommentMediaUploader.uploadAudio(context, uri)) {
                                is CommentMediaUploader.UploadResult.Success -> {
                                    audioUrl = result.url
                                    finalMediaType = "audio"
                                }
                                is CommentMediaUploader.UploadResult.Error -> {
                                    _error.postValue(result.message)
                                    _uploadProgress.postValue(false)
                                    return@launch
                                }
                            }
                        }
                    }
                }
                
                val commentData = mutableMapOf(
                    "user_id" to currentUid,
                    "content" to commentText,
                    "post_id" to postKey,
                    "parent_comment_id" to replyToCommentKey
                )
                
                // Add media fields if present
                photoUrl?.let { commentData["photo_url"] = it }
                videoUrl?.let { commentData["video_url"] = it }
                audioUrl?.let { commentData["audio_url"] = it }
                finalMediaType?.let { commentData["media_type"] = it }
                
                dbService.insert("comments", commentData)
                _uploadProgress.postValue(false)
            } catch (e: Exception) {
                _error.postValue("Failed to post comment: ${e.message}")
                _uploadProgress.postValue(false)
            }
        }
    }

    fun likeComment(postKey: String, commentKey: String) {
        viewModelScope.launch {
            try {
                val currentUid = authService.getCurrentUserId() ?: return@launch
                
                // Check if already liked by THIS user
                val reactionsResult = dbService.selectWithFilter(
                    table = "comment_reactions",
                    columns = "*",
                    filter = "comment_id",
                    value = commentKey
                )
                
                // Filter results to check if current user has liked
                val userReaction = reactionsResult.getOrNull()?.find { 
                    it["user_id"] == currentUid && it["reaction_type"] == "like"
                }
                
                if (userReaction != null) {
                    // Unlike - remove the reaction
                    val reactionId = userReaction["id"] as? String
                    if (reactionId != null) {
                        dbService.delete("comment_reactions", "id", reactionId)
                    }
                } else {
                    // Like - add the reaction
                    val reactionData = mapOf(
                        "comment_id" to commentKey,
                        "user_id" to currentUid,
                        "reaction_type" to "like"
                    )
                    dbService.insert("comment_reactions", reactionData)
                }
            } catch (e: Exception) {
                _error.postValue("Failed to like comment: ${e.message}")
            }
        }
    }

    fun deleteComment(postKey: String, commentKey: String) {
        viewModelScope.launch {
            try {
                // Soft delete by setting deleted_at timestamp
                val updateData = mapOf("deleted_at" to System.currentTimeMillis().toString())
                dbService.update("comments", updateData, "id", commentKey)
            } catch (e: Exception) {
                _error.postValue("Failed to delete comment: ${e.message}")
            }
        }
    }

    fun editComment(postKey: String, commentKey: String, newComment: String) {
        viewModelScope.launch {
            try {
                val updateData = mapOf(
                    "content" to newComment,
                    "edited_at" to System.currentTimeMillis().toString(),
                    "is_edited" to true
                )
                dbService.update("comments", updateData, "id", commentKey)
            } catch (e: Exception) {
                _error.postValue("Failed to edit comment: ${e.message}")
            }
        }
    }

    fun likeReply(postKey: String, commentKey: String, replyKey: String) {
        viewModelScope.launch {
            try {
                val currentUid = authService.getCurrentUserId() ?: return@launch
                
                // Check if already liked by THIS user (replies use comment_reactions too)
                val reactionsResult = dbService.selectWithFilter(
                    table = "comment_reactions",
                    columns = "*",
                    filter = "comment_id",
                    value = replyKey
                )
                
                // Filter results to check if current user has liked
                val userReaction = reactionsResult.getOrNull()?.find { 
                    it["user_id"] == currentUid && it["reaction_type"] == "like"
                }
                
                if (userReaction != null) {
                    // Unlike - remove the reaction
                    val reactionId = userReaction["id"] as? String
                    if (reactionId != null) {
                        dbService.delete("comment_reactions", "id", reactionId)
                    }
                } else {
                    // Like
                    val reactionData = mapOf(
                        "comment_id" to replyKey,
                        "user_id" to currentUid,
                        "reaction_type" to "like"
                    )
                    dbService.insert("comment_reactions", reactionData)
                }
            } catch (e: Exception) {
                _error.postValue("Failed to like reply: ${e.message}")
            }
        }
    }

    fun deleteReply(postKey: String, commentKey: String, replyKey: String) {
        viewModelScope.launch {
            try {
                // Soft delete (replies are in comments table)
                val updateData = mapOf("deleted_at" to System.currentTimeMillis().toString())
                dbService.update("comments", updateData, "id", replyKey)
            } catch (e: Exception) {
                _error.postValue("Failed to delete reply: ${e.message}")
            }
        }
    }

    fun editReply(postKey: String, commentKey: String, replyKey: String, newReply: String) {
        viewModelScope.launch {
            try {
                val updateData = mapOf(
                    "content" to newReply,
                    "edited_at" to System.currentTimeMillis().toString(),
                    "is_edited" to true
                )
                dbService.update("comments", updateData, "id", replyKey)
            } catch (e: Exception) {
                _error.postValue("Failed to edit reply: ${e.message}")
            }
        }
    }
}
