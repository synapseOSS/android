# XML Migration Status

This document tracks the status of XML layout files in the Synapse project as of the migration to Jetpack Compose.

## Migrated & Deleted

The following XML files have been confirmed unused (replaced by Compose) and were deleted:

*   **Activities:**
    *   `activity_auth.xml` (Replaced by `AuthActivity` + `AuthScreen`)
    *   `activity_main.xml` (Replaced by `MainActivity` + `MainScreen`)
    *   `activity_inbox.xml` (Replaced by `InboxActivity` + `InboxScreen`)
    *   `activity_profile_cover_photo_history.xml` (Replaced by `PhotoHistoryScreen`)
    *   `activity_profile_photo_history.xml` (Replaced by `PhotoHistoryScreen`)

*   **Fragments:**
    *   `fragment_home.xml` (Replaced by `HomeScreen`)
    *   `fragment_reels.xml` (Replaced by `ReelsScreen`)
    *   `fragment_inbox_chats.xml` (Replaced by `ChatListFragment` -> `InboxScreen`)
    *   `fragment_notifications.xml` (Replaced by `NotificationsScreen`)

*   **Items & Dialogs:**
    *   `item_chat_list.xml`
    *   `notification_item.xml`
    *   `dialog_update.xml`
    *   `dialog_error.xml`
    *   `message_input_layout.xml`
    *   `chat_reply_layout.xml`
    *   `dialog_profile_cover_image_history_add.xml`
    *   `profile_cover_image_history_list.xml`
    *   `dp_history_cv.xml`
    *   `report_requests_bind.xml`
    *   `single_et.xml`
    *   `synapse_post_cv.xml`
    *   `synapse_story_cv.xml`
    *   `users_list.xml`
    *   `chat_msg_cv_synapse.xml`

## Pending Migration (Still in Use)

The following XML files are still referenced by the codebase and require migration to Jetpack Compose:

### Activities
*   `activity_edit_post.xml` (Used by `EditPostActivity`)
*   `activity_post_detail.xml` (Used by `PostDetailActivity` via `ActivityPostDetailBinding`)
*   `activity_user_follows_list.xml` (Used by `UserFollowsListActivity`)
*   `activity_debug.xml` (Used by `DebugActivity`)
*   `activity_disappearing_message_settings.xml` (Used by `DisappearingMessageSettingsActivity`)
*   `activity_media_preview.xml`
*   `activity_image_gallery.xml`

### Adapters & Items
*   `item_post.xml` (Used by legacy `PostAdapter` - likely dead but kept for safety)
*   `item_post_enhanced.xml` (Used by `FeedPostsAdapter` - likely dead but kept for safety)
*   `item_post_md3.xml` (Used by `PostsAdapter`)
*   `item_search_post.xml` (Used by `SearchResultsAdapter`)
*   `item_comment_detail.xml` (Used by `CommentDetailAdapter` in `PostDetailActivity`)
*   `item_story.xml` (Used by `StoryAdapter`)
*   `item_media_pager.xml` (Used by `MediaPagerAdapter`)

### Dialogs & Bottom Sheets
*   `bottom_sheet_post_options.xml`
*   `bottom_sheet_post_statistics.xml`
*   `create_post_settings_bottom_sheet.xml`
*   `dialog_synapse_bg_view.xml`

## Next Steps
1.  Migrate `EditPostActivity` to a Compose `EditPostScreen`.
2.  Migrate `PostDetailActivity` to a Compose `PostDetailScreen`.
3.  Migrate `UserFollowsListActivity` to a Compose screen.
4.  Remove `FeedPostsAdapter`, `PostsAdapter`, and `PostAdapter` once verified completely unused.
