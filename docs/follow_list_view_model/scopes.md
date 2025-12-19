*   **Visibility**
    *   `FollowListViewModel`: Public class extending `ViewModel`.
    *   `_uiState`: Private mutable state.
    *   `uiState`: Public immutable state stream.

*   **Lifecycle**
    *   `viewModelScope`: Tied to the ViewModel's lifecycle. Coroutines persist across configuration changes (e.g., screen rotation) and are cancelled when the ViewModel is cleared (e.g., Activity finish).

*   **Threading**
    *   `viewModelScope.launch`: Executes on `Dispatchers.Main.immediate` by default.
    *   Service calls (`followService.getFollowers`) suspend and offload to `Dispatchers.IO` internally.
