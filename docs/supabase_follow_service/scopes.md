*   **Visibility**
    *   Public class.
    *   `client`, `databaseService`: Private dependencies.
    *   `updateFollowerCounts`: Private helper function.

*   **Lifecycle**
    *   Singleton-like behavior (via `SupabaseClient` singleton), though the class itself is instantiated per use.
    *   No Android lifecycle dependencies.

*   **Threading**
    *   `withContext(Dispatchers.IO)`: **Explicitly enforces IO thread** for all database operations. This ensures main thread safety regardless of where it's called from.
