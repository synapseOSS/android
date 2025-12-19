### Data Flow
1.  **Input**: Receives `List<Map<String, Any?>>` and callbacks (`onUserClick`, `onMessageClick`) in constructor.
2.  **Binding**: `onBindViewHolder` calls `holder.bind(user)`.
3.  **UI Updates**:
    *   Text: Sets username, display name.
    *   Image: Uses `Glide` to load avatar URL. Handles null/empty checks and placeholders.
    *   Visibility: Toggles `verifyIcon` and `displayNameText` based on data.
4.  **Logic**:
    *   **Self-check**: Checks `SupabaseClient.client.auth.currentUserOrNull()?.id` against the item's `userId`.
    *   **Button State**: Hides follow/message buttons if the user is the current user.
    *   **Follow Button**: Calls `followButton.setup` which likely handles its own internal state and API calls via `lifecycleScope`.

### Critical Functions
*   `bind(user)`: Maps data map to UI views.
*   `Glide.load()`: Image loading.
*   `followButton.setup()`: Delegated logic for follow status.

### Error Handling
*   **Safe Casting**: Uses `?.toString()` and `toBoolean()` to prevent ClassCastExceptions from the untyped Map.
*   **Image Loading**: Glide `error()` placeholder handles failed image loads.
*   **Null Safety**: Checks for null `currentUserUid`.

### State Management
*   **Stateless Adapter**: The adapter itself holds no mutable state besides the `users` list reference.
*   **LifecycleOwner**: Requires `itemView.context` to be a `LifecycleOwner` to pass `lifecycleScope` to `FollowButton`.
