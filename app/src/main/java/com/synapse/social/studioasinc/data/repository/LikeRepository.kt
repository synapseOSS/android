package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.local.database.PostDao

/**
 * @deprecated Use [PostRepository.toggleReaction] instead.
 * 
 * The `likes` table uses a different schema (target_id, target_type) than the 
 * `reactions` table (post_id, user_id, reaction_type). The reactions table is 
 * now the single source of truth for all post reactions.
 * 
 * Migration guide:
 * - Replace `likeRepository.toggleLike(userId, postId)` with 
 *   `postRepository.toggleReaction(postId, userId, ReactionType.LIKE)`
 * - Replace `likeRepository.isLiked(userId, postId)` with
 *   `postRepository.getUserReaction(postId, userId)`
 * - Replace `likeRepository.getLikeCount(postId)` with
 *   `postRepository.getReactionSummary(postId)`
 */
@Deprecated(
    message = "Use PostRepository.toggleReaction() instead. The reactions table is now the single source of truth.",
    replaceWith = ReplaceWith("PostRepository(postDao).toggleReaction(postId, userId, ReactionType.LIKE)")
)
class LikeRepository(
    private val postDao: PostDao,
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) {
    
    private val postRepository = PostRepository(postDao, client)
    
    /**
     * @deprecated Use [PostRepository.toggleReaction] with [ReactionType.LIKE]
     */
    @Deprecated("Use PostRepository.toggleReaction()", ReplaceWith("postRepository.toggleReaction(targetId, userId, ReactionType.LIKE)"))
    suspend fun toggleLike(userId: String, targetId: String, targetType: String = "post"): Result<Boolean> {
        if (targetType != "post") {
            android.util.Log.w("LikeRepository", "Non-post likes not supported in reactions table")
            return Result.failure(Exception("Only post reactions are supported"))
        }
        
        val result = postRepository.toggleReaction(targetId, userId, ReactionType.LIKE)
        return result.map { true }
    }
    
    /**
     * @deprecated Use [PostRepository.getUserReaction]
     */
    @Deprecated("Use PostRepository.getUserReaction()", ReplaceWith("postRepository.getUserReaction(targetId, userId)"))
    suspend fun isLiked(userId: String, targetId: String, targetType: String = "post"): Result<Boolean> {
        if (targetType != "post") return Result.success(false)
        
        val result = postRepository.getUserReaction(targetId, userId)
        return result.map { it != null }
    }
    
    /**
     * @deprecated Use [PostRepository.getReactionSummary]
     */
    @Deprecated("Use PostRepository.getReactionSummary()", ReplaceWith("postRepository.getReactionSummary(targetId)"))
    suspend fun getLikeCount(targetId: String, targetType: String = "post"): Result<Int> {
        if (targetType != "post") return Result.success(0)
        
        val result = postRepository.getReactionSummary(targetId)
        return result.map { summary -> summary.values.sum() }
    }
    
    /**
     * @deprecated Not supported - reactions table doesn't track by user
     */
    @Deprecated("Not supported in new schema")
    suspend fun getUserLikes(userId: String): Result<List<Nothing>> {
        android.util.Log.w("LikeRepository", "getUserLikes() not supported - use reactions table queries")
        return Result.success(emptyList())
    }
}
