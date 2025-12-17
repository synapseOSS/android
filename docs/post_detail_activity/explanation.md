**Data Flow:**
*   Receives `EXTRA_POST_ID` and optional `EXTRA_AUTHOR_UID` via `Intent` extras in `onCreate`.
*   Triggers `viewModel.loadPost(postId)` and `viewModel.loadComments(postId)` immediately.
*   Observes `viewModel.postState` (StateFlow) (See `/docs/post_detail_view_model/explanation.md`):
    *   `PostDetailState.Loading` -> Shows progress bar.
    *   `PostDetailState.Success` -> Calls `displayPost(PostDetail)` to populate UI fields (text, author info, timestamps) and initializes media viewers (`setupMedia`).
    *   `PostDetailState.Error`/`NotFound` -> Shows `Toast` messages.
*   Observes `viewModel.commentsState` (StateFlow):
    *   Updates `commentsAdapter` via `submitList` with `CommentWithUser` objects.
    *   Toggles visibility of `tvNoComments` based on list emptiness.

**Critical Functions:**
*   `onCreate()`: Initializes ViewBinding, sets up RecyclerViews/listeners, and kicks off data loading.
*   `displayPost(PostDetail)`: Maps `PostDetail` object to UI views. Configures `MediaPagerAdapter` if `mediaItems` exist. Sets up Polls if `hasPoll` is true.
*   `setupCommentsRecyclerView()`: Initializes `CommentDetailAdapter` with callbacks for `onReplyClick`, `onLikeClick`, etc.
*   `setupCommentInput()`: Attaches `TextWatcher` to `etComment` for send button state. Handles `ivSend` click to trigger `viewModel.addComment`.

**Error Handling:**
*   UI displays `Toast` on `PostDetailState.Error` and `CommentsState.Error`.
*   `loadCurrentUserAvatar` uses a `try-catch` block that silently swallows exceptions during Supabase user fetching.
*   `Glide` loading errors fall back to `R.drawable.avatar`.

**State Management:**
*   `lifecycleScope` collects `StateFlow`s from ViewModel.
*   Local UI state: `replyToCommentId`, `replyToUsername` tracks current reply target. `currentPost` (See `/docs/post_model/summary.md`) holds reference to loaded post for sharing/reporting.
