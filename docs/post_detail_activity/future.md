*   `loadCurrentUserAvatar` swallows all exceptions; add logging or user feedback.
*   `animateSendButton` instantiates `OvershootInterpolator` on every click; cache this instance.
*   Direct `SupabaseClient` usage in `loadCurrentUserAvatar` bypasses `ViewModel`/`Repository` architecture; move logic to `PostDetailViewModel`.
*   Hardcoded strings in `Toast` messages (e.g., "Post not found"); move to `strings.xml`.
*   `replyToCommentId`/`replyToUsername` is local mutable state; consider moving to `ViewModel` to survive configuration changes.
