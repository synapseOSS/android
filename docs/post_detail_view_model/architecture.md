**Pattern:** MVVM (Model-View-ViewModel)

**Adherence:**
*   Exposes state via `StateFlow` (Observable streams).
*   Encapsulates business logic and data transformation.
*   Survives configuration changes.

**Violation:**
*   **Dependency Injection:** Manually instantiates Repositories (`PostDetailRepository()`, etc.) instead of receiving them via constructor. This violates Inversion of Control principle, making the ViewModel hard to unit test with mock repositories.
