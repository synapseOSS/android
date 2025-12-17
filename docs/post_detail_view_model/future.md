*   `currentPostId` is a mutable var; race conditions possible if `loadPost` called concurrently for different IDs (though unlikely in current UI flow).
*   `loadReplies` appends to `_repliesState` map without clearing; potentially unbounded growth for long-lived ViewModel instances.
*   Direct instantiation of Repositories (`PostDetailRepository()`, etc.) inside ViewModel creates tight coupling and hinders testing. Use Dependency Injection (Hilt/Koin).
*   Re-fetching entire post/comments list after every action (like/comment) is inefficient; implement optimistic local state updates.
