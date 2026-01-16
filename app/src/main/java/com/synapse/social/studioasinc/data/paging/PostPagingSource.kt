package com.synapse.social.studioasinc.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.repository.ReactionRepository
import com.synapse.social.studioasinc.domain.model.Post
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
    private val pollRepository = com.synapse.social.studioasinc.data.repository.PollRepository()

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
                            latest_comments:comments(id, content, user_id, created_at, users(username))
                        """.trimIndent())
                    ) {
                        order("timestamp", order = Order.DESCENDING)
                        range(position.toLong(), (position + pageSize - 1).toLong())
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
                    // Sort comments by createdAt descending to find the latest
                    val latestComment = commentsArray.map { it.jsonObject }
                        .maxByOrNull { it["created_at"]?.jsonPrimitive?.contentOrNull ?: "" }

                    if (latestComment != null) {
                        // Fixed: Map 'content' column to latestCommentText
                        post.latestCommentText = latestComment["content"]?.jsonPrimitive?.contentOrNull
                        val commentUser = latestComment["users"]?.jsonObject
                        post.latestCommentAuthor = commentUser?.get("username")?.jsonPrimitive?.contentOrNull
                    }
                }

                post
            }

            // Populate reactions
            val postsWithReactions = reactionRepository.populatePostReactions(parsedPosts)
            val postsWithPolls = populatePostPolls(postsWithReactions)

            LoadResult.Page(
                data = postsWithPolls,
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

    private suspend fun populatePostPolls(posts: List<Post>): List<Post> {
        val pollPosts = posts.filter { it.hasPoll == true }
        if (pollPosts.isEmpty()) return posts

        val postIds = pollPosts.map { it.id }

        // Fetch user votes
        val userVotesResult = pollRepository.getBatchUserVotes(postIds)
        val userVotes = userVotesResult.getOrNull() ?: emptyMap()

        // Fetch poll vote counts
        val pollCountsResult = pollRepository.getBatchPollVotes(postIds)
        val pollCounts = pollCountsResult.getOrNull() ?: emptyMap()

        return posts.map { post ->
            if (post.hasPoll == true) {
                val userVote = userVotes[post.id]
                val counts = pollCounts[post.id] ?: emptyMap()

                // Update poll options with new counts
                val updatedOptions = post.pollOptions?.mapIndexed { index, option ->
                    option.copy(votes = counts[index] ?: 0)
                }

                val updatedPost = post.copy(
                    pollOptions = updatedOptions
                )
                updatedPost.userPollVote = userVote
                updatedPost
            } else {
                post
            }
        }
    }
}
