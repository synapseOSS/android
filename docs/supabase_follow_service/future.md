*   **RPC Calls**: Replace client-side multi-step logic (get IDs -> get Users; update table -> update count) with Supabase Database Functions (RPC) or Triggers. This ensures atomicity and reduces network round trips.
*   **Dependency Injection**: Should be a Singleton managed by DI (Hilt) rather than instantiated via `new SupabaseFollowService()`.
*   **Pagination**: `limit` is supported, but offset/cursor-based pagination is missing for infinite scrolling.
*   **DTOs**: Return strongly typed objects (e.g., `UserDto`, `FollowDto`) instead of `Map<String, Any?>` or `JsonObject`.
*   **Hardcoded Strings**: Table names ("follows", "users") and column names should be constants.
