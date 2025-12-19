### Data Flow
1.  **Initialization**: `onCreate` extracts `EXTRA_USER_ID` and `EXTRA_LIST_TYPE` from the intent. It calls `initialize()` (UI setup), `setupRecyclerView()` (Adapter setup), and `loadData()`.
2.  **Data Fetching**: `loadData()` triggers `lifecycleScope.launch`.
3.  **Service Call**: Inside the coroutine, it calls `followService.getFollowers` or `followService.getFollowing` (from `SupabaseFollowService`) based on `listType`.
4.  **Result Handling**:
    *   **Success**: The returned `List<Map<String, Any?>>` clears and populates the local `usersList` ArrayList. `adapter?.notifyDataSetChanged()` updates the UI.
    *   **Failure**: Logs error and displays an empty view with an error message.
5.  **User Interaction**:
    *   **Profile Navigation**: Clicking a user triggers `onUserClick` callback in adapter, creating an Intent for `ProfileComposeActivity`.
    *   **Direct Chat**: Clicking the message button triggers `startDirectChat`. This function fetches the current user's UID via `AuthRepository`, creates a progress dialog, and calls `SupabaseChatService.getOrCreateDirectChat`. On success, it navigates to `ChatActivity`.

### Critical Functions
*   `loadData()`: Orchestrates the data fetching process from `SupabaseFollowService`.
*   `startDirectChat(user)`: Complex interaction involving `AuthRepository`, `SupabaseChatService`, and navigation to `ChatActivity`.
*   `setupRecyclerView()`: Initializes `FollowListAdapter` with callbacks for user and message clicks.

### Error Handling
*   `try-catch` blocks wrap coroutine executions in `loadData` and `startDirectChat`.
*   `Result.fold` is used to handle the outcome of `SupabaseFollowService` and `SupabaseChatService` calls.
*   `Toast` messages display user-facing errors (e.g., "Failed to get user info").
*   Errors are logged using `android.util.Log`.

### State Management
*   **Local State**: `usersList` (ArrayList) holds the mutable data source for the adapter.
*   **UI State**: Managed imperatively via `showLoading(Boolean)` and `showEmpty(String)`, toggling visibility of `progressBar`, `recyclerView`, and `emptyView`.
*   **No ViewModel**: The Activity manages its own state and data persistence during its lifecycle, bypassing `FollowListViewModel`.
