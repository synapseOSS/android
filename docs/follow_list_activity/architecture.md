### Pattern: MVC (Model-View-Controller) / Monolith Activity

This component operates as a **Controller** in an MVC-like structure (or "Massive View Controller" in Android terms), rather than the intended MVVM.

*   **Violations**:
    *   **Data Fetching in UI**: The Activity directly calls `SupabaseFollowService` and `AuthRepository`.
    *   **State Management**: UI state (loading, error, content) is managed manually within the Activity, not observed from a ViewModel.
    *   **Business Logic**: Logic for determining which list to fetch and how to start a chat resides in the UI layer.

*   **Intended Pattern (MVVM)**:
    *   A `FollowListViewModel` exists but is unused. The Activity *should* observe `FollowListViewModel.uiState` and delegate actions to it.
