*   **Integration**: Connect this ViewModel to `FollowListActivity` to fix the architectural violation.
*   **Dependency Injection**: Inject `SupabaseFollowService` via constructor (e.g., using Hilt/Koin) instead of instantiating it directly. This facilitates testing.
*   **Enum for List Type**: Replace string literals ("followers", "following") with an Enum or Sealed Class to prevent runtime errors.
*   **User Mapping**: Move the `Map` -> `User` transformation logic to a Mapper class or the Repository layer to keep the ViewModel clean.
