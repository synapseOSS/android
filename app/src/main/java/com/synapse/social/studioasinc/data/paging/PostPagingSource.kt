package com.synapse.social.studioasinc.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.data.repository.ReactionRepository
import com.synapse.social.studioasinc.model.Post
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true }

class PostPagingSource(
    private val queryBuilder: PostgrestQueryBuilder
) : PagingSource<Int, Post>() {

    private val reactionRepository = ReactionRepository()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val position = params.key ?: 0
        val pageSize = params.loadSize
        return try {
            Log.d("PostPagingSource", "Loading posts at position: $position, pageSize: $pageSize")
            
            val response = withContext(Dispatchers.IO) {
                queryBuilder
                    .select(
                        columns = Columns.raw("""
                            *,
                            users!posts_author_uid_fkey(username, avatar, verify),
                            latest_comments:comments(id, comment, user_id, created_at, users(username))
                        """.trimIndent())
                    ) {
                        order("timestamp", order = Order.DESCENDING)
                        range(position.toLong(), (position + pageSize - 1).toLong())
                        // Limit the embedded resource "latest_comments" to 1 and order by created_at desc
                        param("latest_comments.limit", "1")
                        param("latest_comments.order", "created_at.desc")
                    }
                    .decodeList<JsonObject>()
            }

            Log.d("PostPagingSource", "Loaded ${response.size} posts")

            val parsedPosts = response.map { jsonElement ->
                val post = json.decodeFromJsonElement<Post>(jsonElement)
                val userData = jsonElement["users"]?.jsonObject
                post.username = userData?.get("username")?.jsonPrimitive?.contentOrNull
                post.avatarUrl = userData?.get("avatar")?.jsonPrimitive?.contentOrNull?.let { avatarPath ->
                    SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_USER_AVATARS, avatarPath)
                }
                post.isVerified = userData?.get("verify")?.jsonPrimitive?.booleanOrNull ?: false

                // Manually parse the latest comment since decoding strictly to Post won't catch the embedded list
                val commentsArray = jsonElement["latest_comments"]?.jsonArray
                if (!commentsArray.isNullOrEmpty()) {
                    val firstComment = commentsArray[0].jsonObject
                    post.latestCommentText = firstComment["comment"]?.jsonPrimitive?.contentOrNull
                    val commentUser = firstComment["users"]?.jsonObject
                    post.latestCommentAuthor = commentUser?.get("username")?.jsonPrimitive?.contentOrNull
                }

                post
            }

            // Populate reactions
            val postsWithReactions = reactionRepository.populatePostReactions(parsedPosts)

            LoadResult.Page(
                data = postsWithReactions,
                prevKey = if (position == 0) null else (position - pageSize).coerceAtLeast(0),
                nextKey = if (postsWithReactions.isEmpty()) null else position + pageSize
            )
        } catch (e: Exception) {
            Log.e("PostPagingSource", "Error loading posts", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
