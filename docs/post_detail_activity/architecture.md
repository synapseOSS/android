**Pattern:** MVVM (Model-View-ViewModel)

**Adherence:**
*   **View:** `PostDetailActivity` is a passive view. It observes state from `PostDetailViewModel` (See `/docs/post_detail_view_model/architecture.md`) and renders it. User inputs are forwarded to ViewModel methods.
*   **ViewModel:** `PostDetailViewModel` exposes state via `StateFlow` and handles business logic (repository calls).

**Violation:**
*   `loadCurrentUserAvatar` method directly accesses `SupabaseClient` and performs network requests within the Activity, violating separation of concerns. This logic belongs in a User Repository accessed via ViewModel.
*   Formatting logic (e.g., `TimeUtils.getTimeAgo`) and some display logic (e.g., formatting "1.2M") resides in Activity; could be moved to a Presentation/UI Model mapper.
