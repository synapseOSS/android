### Pattern: Adapter (View)

Standard Android RecyclerView Adapter.

*   **Violations**:
    *   **Logic in View**: Performs logic to determine if the user is the current user (`auth.currentUserOrNull()`). This logic belongs in the ViewModel/Controller, passing a boolean `isCurrentUser` to the adapter.
    *   **Direct Dependency**: Directly accesses `SupabaseClient` singleton, coupling the View component to the backend SDK.
    *   **Context Casting**: Casts context to `LifecycleOwner`, which assumes the adapter is used in a specific environment, limiting reusability.
