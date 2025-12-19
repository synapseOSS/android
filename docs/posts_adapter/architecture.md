**Pattern:** Adapter (View Layer)

**Adherence:**
*   Separates data binding from business logic.
*   Uses callbacks (`((Post) -> Unit)`) to delegate user actions back to the Activity/Fragment.
*   Uses `ListAdapter` + `DiffUtil` for efficient UI updates (standard Android practice).

**Violation:**
*   N/A (Standard implementation).
