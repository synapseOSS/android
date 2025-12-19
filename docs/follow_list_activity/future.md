*   **Adoption of MVVM**: The Activity currently ignores `FollowListViewModel`. Logic for fetching users should be moved to the ViewModel to survive configuration changes and separate concerns.
*   **Remove Service Dependency**: Direct instantiation of `SupabaseFollowService` inside the Activity couples the UI to the data source.
*   **Adapter Optimization**: Replace `notifyDataSetChanged()` with `DiffUtil` or `ListAdapter` for efficient UI updates.
*   **Hardcoded Strings**: Extract string literals ("Followers", "Following", error messages) to `strings.xml`.
*   **ProgressDialog Deprecation**: `android.app.ProgressDialog` is deprecated. Use a `ProgressBar` in the layout or a `DialogFragment`.
*   **Data Type Safety**: Usage of `Map<String, Any?>` is fragile. Transition to the `User` data class used in `FollowListViewModel`.
