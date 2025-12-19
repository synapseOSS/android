*   **Visibility**
    *   Public class.
    *   `UserViewHolder`: Inner class.

*   **Lifecycle**
    *   Bound to `RecyclerView` lifecycle.
    *   Depends on `itemView.context` being a `LifecycleOwner` (Activity/Fragment) for `FollowButton` coroutines.

*   **Threading**
    *   `onBindViewHolder`: Executes on Main Thread.
    *   `Glide`: Loads images on background threads, callbacks on Main.
    *   `followButton.setup`: Uses passed `lifecycleScope` (Main thread).
