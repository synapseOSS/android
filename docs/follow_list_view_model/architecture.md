### Pattern: MVVM (Model-View-ViewModel)

This component correctly implements the **ViewModel** part of MVVM.

*   **Adherence**:
    *   **State Encapsulation**: Exposes state via `StateFlow`, not exposing mutable variables.
    *   **Lifecycle Awareness**: Uses `viewModelScope` to manage coroutines independent of UI lifecycle.
    *   **UI Independence**: Contains no references to Android View classes (no `Context`, `View`, etc.).

*   **Deviation (Contextual)**:
    *   While the class itself follows MVVM, it is effectively "orphaned" code because the corresponding View (`FollowListActivity`) does not use it.
    *   **Repository Pattern**: It instantiates `SupabaseFollowService` directly, which acts as a data source/repository. Ideally, this should be an interface-based dependency.
