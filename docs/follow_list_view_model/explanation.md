### Data Flow
1.  **Trigger**: `loadUsers(userId, listType)` is called (hypothetically, as it's currently unused).
2.  **State Update**: `_uiState` is updated to set `isLoading = true` and clear previous errors.
3.  **Service Call**: `SupabaseFollowService.getFollowers` or `getFollowing` is invoked based on `listType`.
4.  **Transformation**:
    *   **Success**: The raw `Map<String, Any?>` list from the service is mapped to a list of `User` domain objects. Safe casting (`toString`, `toBoolean`) handles potential type mismatches.
    *   **Failure**: Error message is captured.
5.  **State Emission**: `_uiState` emits a new `FollowListUiState` with either the list of `users` or an `error` message, and `isLoading = false`.

### Critical Functions
*   `loadUsers`: The primary entry point for fetching data.
*   `uiState`: The public read-only `StateFlow` consumed by the UI.

### Error Handling
*   `try-catch` block wraps the coroutine execution.
*   `Result.fold` handles the service response.
*   Errors are exposed via the `error` property in `FollowListUiState`, allowing the UI to react (e.g., show a Toast or Snackbar).

### State Management
*   **StateFlow**: Uses `MutableStateFlow` (private) and `asStateFlow` (public) to ensure unidirectional data flow.
*   **Immutability**: `FollowListUiState` is a data class, ensuring state updates are atomic via `.copy()`.
