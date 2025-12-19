**Data Flow:**
*   **Initialization:** Instantiates multiple repositories (`PostDetailRepository`, `CommentRepository`, etc.) directly.
*   **Loading:**
    *   `loadPost(postId)`: Sets `_postState` to `Loading`. Calls `postDetailRepository.getPostWithDetails`. On success -> `Success(PostDetail)`. On failure -> `Error`. Also increments view count.
    *   `loadComments(postId)`: Sets `_commentsState` to `Loading`. Calls `commentRepository.fetchComments`. On success -> `Success(List<CommentWithUser>)`.
*   **Actions:**
    *   `toggleReaction`: Calls `reactionRepository.togglePostReaction`. Refreshes post on success.
    *   `addComment`: Calls `commentRepository.createComment`. Refreshes comments on success.
    *   `loadReplies`: Fetches replies for a specific comment and updates `_repliesState` map.

**Critical Functions:**
*   `loadPost`, `loadComments`: Primary data fetchers.
*   `toggleReaction`, `toggleCommentReaction`: Optimistic updates not implemented; relies on re-fetching data (`loadPost`/`loadComments`) on success.
*   `loadReplies`: Manages nested comment data in a separate `_repliesState` map.

**Error Handling:**
*   Uses `fold` on `Result` types returned by repositories.
*   Maps exceptions to `PostDetailState.Error` or `CommentsState.Error` with messages.
*   Logs errors via `Logger` and `loge`.

**State Management:**
*   `_postState` (MutableStateFlow<PostDetailState>)
*   `_commentsState` (MutableStateFlow<CommentsState>)
*   `_repliesState` (MutableStateFlow<Map<String, List<CommentWithUser>>>): Stores replies keyed by parent comment ID.
*   `_replyLoadingState`: Tracks which comments are currently loading replies.

**Dependencies:**
*   Uses `Post` model (See `/docs/post_model/summary.md`) within `PostDetail` wrapper.
