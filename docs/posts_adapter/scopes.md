*   **Visibility:**
    *   Public class `PostsAdapter`.
    *   Inner class `PostViewHolder`.
*   **Lifecycle:**
    *   `onViewRecycled`: Cancels running animations to prevent memory leaks.
    *   `lifecycleOwner` passed in constructor but not actively used in provided snippet (likely for `ImageLoader` or future extensions).
*   **Threading:**
    *   `DiffUtil` calculation happens on background thread (via `ListAdapter` defaults).
    *   `onBindViewHolder` and animations run on Main thread.
