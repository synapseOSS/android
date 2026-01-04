package com.synapse.social.studioasinc.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<CommentEntity>)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp DESC")
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>
    
    @Query("SELECT * FROM comments WHERE parent_comment_id = :commentId ORDER BY timestamp ASC")
    fun getRepliesForComment(commentId: String): Flow<List<CommentEntity>>
}
