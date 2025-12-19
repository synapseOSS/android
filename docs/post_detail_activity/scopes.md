*   **Visibility:**
    *   Public class `PostDetailActivity` extending `BaseActivity`.
    *   Private internals: `binding`, `commentsAdapter`, `currentPost` state variables.
    *   Private helper methods: `setupToolbar`, `setupMedia`, `displayPost`, `setReplyMode`.
*   **Lifecycle:**
    *   `onCreate`: Initializes `PostDetailViewModel`, sets up UI, starts `lifecycleScope` observers.
    *   `lifecycleScope`: Coroutines collecting flows are bound to Activity lifecycle (cancelled on destroy).
    *   `Glide`: RequestManager tied to Activity context.
*   **Threading:**
    *   `collectLatest`: Runs on Main thread (UI updates).
    *   `viewModel.loadPost/loadComments`: Dispatched to ViewModel scope (likely IO).
    *   `loadCurrentUserAvatar`: Runs in `lifecycleScope` (Main) but calls suspend functions.
