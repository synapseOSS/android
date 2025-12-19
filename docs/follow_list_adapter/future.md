*   **ListAdapter**: Implement `ListAdapter` with `DiffUtil` for efficient updates and animations.
*   **Data Class**: Accept `List<User>` instead of `List<Map<String, Any?>>` for type safety.
*   **Decouple Auth**: Remove direct dependency on `SupabaseClient.client.auth` inside the `bind` method. Pass current user ID into the adapter constructor or `bind` method.
*   **ViewHolder Optimization**: `onCreateViewHolder` inflates the view, but logic inside `bind` (like `currentUserOrNull`) runs for every item scroll. This should be optimized.
