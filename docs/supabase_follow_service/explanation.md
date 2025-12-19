### Data Flow
1.  **Input**: Functions accept `userId` strings and operation parameters.
2.  **Configuration Check**: Verifies `SupabaseClient.isConfigured()`.
3.  **Supabase Interaction**:
    *   **Queries**: Uses `client.from("table").select(...)` to fetch data. Filters (`filter`, `eq`, `isIn`) are applied here.
    *   **Mutations**: Uses `databaseService.insert`, `update`, or `client.from("follows").delete` for writing.
4.  **Complex Logic**:
    *   `getFollowers/Following`: Performs a two-step query. First, fetch relationship IDs from `follows`. Second, fetch user details from `users` where `uid` is in the ID list.
    *   `followUser/unfollowUser`: Updates the `follows` table and then triggers `updateFollowerCounts` to increment/decrement counts in the `users` table.
5.  **Output**: Returns `Result<T>` wrapping the success data or exception.

### Critical Functions
*   `followUser`/`unfollowUser`: Transaction-like operations updating both relationship and count.
*   `getFollowers`/`getFollowing`: Aggregates data from two tables to return complete user profiles.
*   `updateFollowerCounts`: Helper to maintain denormalized count data in `users` table.
*   `getSuggestedUsers`: Filters potential users by excluding those already followed.

### Error Handling
*   **Result Pattern**: All public functions return `Result<T>`.
*   **Supabase Config**: Early return `Result.failure` if not configured.
*   **Try-Catch**: Wraps network calls. Exceptions are logged and wrapped in `Result.failure`.
*   **Idempotency**: `followUser` checks if a relationship exists before inserting to prevent duplicate key errors (logic handled manually, though database constraints usually handle this).

### State Management
*   Stateless. Each method call is independent.
