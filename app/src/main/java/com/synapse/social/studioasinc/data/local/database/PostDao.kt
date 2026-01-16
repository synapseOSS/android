package com.synapse.social.studioasinc.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPostsPaged(): androidx.paging.PagingSource<Int, PostEntity>

    @Query("SELECT * FROM posts WHERE postType = 'VIDEO' ORDER BY timestamp DESC")
    fun getVideoPostsPaged(): androidx.paging.PagingSource<Int, PostEntity>

    @Query("SELECT id FROM posts")
    suspend fun getAllPostIds(): List<String>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostById(postId: String): PostEntity?

    @Query("SELECT * FROM posts WHERE authorUid = :userId ORDER BY timestamp DESC")
    suspend fun getPostsByUser(userId: String): List<PostEntity>

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("DELETE FROM posts WHERE id IN (:ids)")
    suspend fun deletePosts(ids: List<String>)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()
}
