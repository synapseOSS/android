*   **Visibility**
    *   `FollowListActivity`: Public class, accessible via Intent.
    *   `usersList`, `adapter`: Private properties, internal to the Activity.
    *   `followService`: Private instance of `SupabaseFollowService`.

*   **Lifecycle**
    *   `onCreate`: Initialization of UI, RecyclerView, and initial data load.
    *   `lifecycleScope`: Tied to the Activity's lifecycle. Coroutines launched here are cancelled when the Activity is destroyed.

*   **Threading**
    *   `lifecycleScope.launch`: Executes on `Dispatchers.Main` by default, safe for UI updates (View visibility, Toast, RecyclerView updates).
    *   `SupabaseFollowService` and `SupabaseChatService` calls: Suspended functions that internally switch to `Dispatchers.IO` (as seen in `SupabaseFollowService` source).
