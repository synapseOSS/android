*   **Visibility:**
    *   Public class `PostDetailViewModel` extending `AndroidViewModel`.
    *   Exposes immutable `StateFlow`s (`postState`, `commentsState`, `repliesState`).
*   **Lifecycle:**
    *   Scoped to the Activity/Fragment lifecycle via `viewModelScope`.
    *   Coroutines launched in `viewModelScope` are cancelled when ViewModel is cleared.
*   **Threading:**
    *   `viewModelScope.launch` executes on Main thread by default.
    *   Repository calls (suspend functions) are expected to be main-safe (likely switching to IO dispatcher internally).
