**Data Flow:**
*   Serialized/Deserialized via `kotlinx.serialization` (`@Serializable`).
*   Hydrated from JSON (network) or `HashMap` (manual parsing via `toPost`).
*   Used by Repositories to populate `PostDetailState`.

**Critical Functions:**
*   `determinePostType`: logic to classify post as VIDEO, IMAGE, or TEXT.
*   `toDetailItems`: Converts flattened post object into a heterogeneous list of `PostDetailItem` (Caption, Image, Video) for `PostDetailActivity`'s adapter.
*   `getReactionSummary`: Formats reaction counts and emojis for UI display.

**Error Handling:**
*   `toPost` extension function manually parses `HashMap` with safe casts (`as?`). Returns default values (empty strings/0) if parsing fails, preventing crashes but potentially hiding data issues.

**State Management:**
*   Immutable `val`s for DB fields (`id`, `authorUid`).
*   Mutable `var`s (`reactions`, `userReaction`, `mediaItems`) allow in-place updates by ViewModels/Repositories without full object copy.
