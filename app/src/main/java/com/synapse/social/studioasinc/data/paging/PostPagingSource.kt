package com.synapse.social.studioasinc.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
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
                            users!posts_author_uid_fkey(username, avatar, verify)
                        """.trimIndent())
                    ) {
                        order("timestamp", order = Order.DESCENDING)
                        range(position.toLong(), (position + pageSize - 1).toLong())
                    }
                    .decodeList<JsonObject>()
            }

            Log.d("PostPagingSource", "Loaded ${response.size} posts")

            val posts = response.map { jsonElement ->
                val post = json.decodeFromJsonElement<Post>(jsonElement)
                val userData = jsonElement["users"]?.jsonObject
                post.username = userData?.get("username")?.jsonPrimitive?.contentOrNull
                post.avatarUrl = userData?.get("avatar")?.jsonPrimitive?.contentOrNull
                post.isVerified = userData?.get("verify")?.jsonPrimitive?.booleanOrNull ?: false
                post
            }

            LoadResult.Page(
                data = posts,
                prevKey = if (position == 0) null else (position - pageSize).coerceAtLeast(0),
                nextKey = if (posts.isEmpty()) null else position + pageSize
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
