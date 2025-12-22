# Backend Structure Documentation

This document outlines the backend structure of the Synapse project, including database tables, relationships, and storage buckets. This information was retrieved directly from the live Supabase project.

## Storage Buckets

| ID | Name | Public |
|----|------|--------|
| public_storage | public_storage | True |
| avatars | avatars | True |
| covers | covers | True |
| posts | posts | True |
| media | media | True |
| story-media | story-media | True |
| story-thumbnails | story-thumbnails | True |

## Database Schema

### Table: `access_requests`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| email | text | No |  |  |
| requestedAt | bigint | No |  |  |
| status | text | Yes |  |  |
| userAgent | text | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `ai_chat_context`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| session_id | uuid | Yes |  | `ai_chat_sessions.id` |
| context_type | text | No |  |  |
| context_content | jsonb | No |  |  |
| relevance_score | numeric | Yes |  |  |
| expires_at | timestamp with time zone | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `ai_chat_responses`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| session_id | uuid | Yes |  | `ai_chat_sessions.id` |
| user_message | text | No |  |  |
| ai_response | text | No |  |  |
| response_type | text | Yes |  |  |
| confidence_score | numeric | Yes |  |  |
| tokens_used | integer | Yes |  |  |
| response_time_ms | integer | Yes |  |  |
| user_feedback | integer | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `ai_chat_sessions`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | uuid | Yes |  | `users.id` |
| chat_id | uuid | Yes |  |  |
| session_type | text | Yes |  |  |
| context_data | jsonb | Yes |  |  |
| is_active | boolean | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |

---

### Table: `ai_persona_config`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| persona_user_id | text | Yes |  | `users.uid` |
| personality_traits | jsonb | Yes |  |  |
| posting_schedule | jsonb | Yes |  |  |
| interaction_rules | jsonb | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| updated_at | timestamp without time zone | Yes |  |  |

---

### Table: `ai_summaries`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| message_id | uuid | No |  | `messages.id` |
| summary_text | text | No |  |  |
| generated_at | bigint | No |  |  |
| generated_by | text | No |  | `users.uid` |
| model_version | text | Yes |  |  |
| character_count | integer | No |  |  |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `blocked_users`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| blocker_id | text | No |  |  |
| blocked_id | text | No |  |  |
| reason | text | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `blocks`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| blocker_id | text | No |  |  |
| blocked_id | text | No |  |  |
| reason | text | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `bookmark_collections`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  | `users.uid` |
| name | text | No |  |  |
| description | text | Yes |  |  |
| icon | text | No |  |  |
| color | text | No |  |  |
| created_at | timestamp with time zone | No |  |  |
| updated_at | timestamp with time zone | No |  |  |

---

### Table: `changelogs`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| version | text | No |  |  |
| date | text | No |  |  |
| platform | text | No |  |  |
| changes | jsonb | No |  |  |
| createdAt | bigint | No |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `chat_participants`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| chat_id | text | No |  |  |
| user_id | text | No |  |  |
| role | text | Yes |  |  |
| joined_at | timestamp without time zone | Yes |  |  |
| added_by | text | Yes |  |  |
| is_admin | boolean | Yes |  |  |
| can_send_messages | boolean | Yes |  |  |
| last_read_message_id | uuid | Yes |  |  |
| last_read_at | timestamp without time zone | Yes |  |  |
| notification_settings | jsonb | Yes |  |  |
| is_archived | boolean | Yes |  |  |
| is_pinned | boolean | Yes |  |  |
| is_muted | boolean | Yes |  |  |
| muted_until | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |

---

### Table: `chats`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| chat_id | text | No |  |  |
| is_group | boolean | Yes |  |  |
| chat_name | text | Yes |  |  |
| chat_description | text | Yes |  |  |
| chat_avatar | text | Yes |  |  |
| created_by | text | No |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| updated_at | timestamp without time zone | Yes |  |  |
| last_message | text | Yes |  |  |
| last_message_time | timestamp with time zone | Yes |  |  |
| last_message_sender | text | Yes |  |  |
| participants_count | integer | Yes |  |  |
| is_active | boolean | Yes |  |  |

---

### Table: `close_friends`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | uuid | No |  |  |
| friend_id | uuid | No |  |  |
| created_at | timestamp with time zone | No |  |  |

---

### Table: `comment_hashtags`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| comment_id | uuid | No |  | `comments.id` |
| hashtag_id | uuid | No |  | `hashtags.id` |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `comment_reactions`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| comment_id | uuid | No |  | `comments.id` |
| user_id | text | No |  | `users.uid` |
| reaction_type | text | No |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |

---

### Table: `comment_reports`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| comment_id | uuid | No |  | `comments.id` |
| reporter_id | text | No |  |  |
| reason | text | No |  |  |
| description | text | Yes |  |  |
| status | text | Yes |  |  |
| reviewed_by | text | Yes |  |  |
| reviewed_at | timestamp with time zone | Yes |  |  |
| action_taken | text | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `comments`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| post_id | text | No |  | `posts.id` |
| user_id | text | No |  | `users.uid` |
| parent_comment_id | uuid | Yes |  | `comments.id` |
| content | text | No |  |  |
| media_url | text | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| updated_at | timestamp without time zone | Yes |  |  |
| likes_count | integer | Yes |  |  |
| replies_count | integer | Yes |  |  |
| is_deleted | boolean | Yes |  |  |
| is_edited | boolean | Yes |  |  |
| is_pinned | boolean | Yes |  |  |
| pinned_at | timestamp with time zone | Yes |  |  |
| pinned_by | text | Yes |  |  |
| edited_at | timestamp with time zone | Yes |  |  |
| report_count | integer | Yes |  |  |
| photo_url | text | Yes |  |  |
| video_url | text | Yes |  |  |
| audio_url | text | Yes |  |  |
| media_type | text | Yes |  |  |

---

### Table: `cover_image_history`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| key | text | No |  |  |
| user_id | text | No |  |  |
| image_url | text | No |  |  |
| upload_date | text | No |  |  |
| type | text | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| updated_at | timestamp without time zone | Yes |  |  |

---

### Table: `favorites`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  | `users.uid` |
| post_id | text | No |  | `posts.id` |
| created_at | timestamp without time zone | Yes |  |  |
| collection_id | uuid | Yes |  | `bookmark_collections.id` |

---

### Table: `flagged_content`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| content_id | uuid | No |  |  |
| content_type | text | No |  |  |
| user_id | uuid | Yes |  | `users.id` |
| flagged_by | text | Yes |  |  |
| rule_id | uuid | Yes |  | `moderation_rules.id` |
| confidence_score | numeric | No |  |  |
| status | text | Yes |  |  |
| moderator_id | uuid | Yes |  | `users.id` |
| moderator_action | text | Yes |  |  |
| moderator_notes | text | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| reviewed_at | timestamp with time zone | Yes |  |  |

---

### Table: `follows`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| follower_id | text | No |  |  |
| following_id | text | No |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `hashtags`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| tag | text | No |  |  |
| usage_count | integer | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |

---

### Table: `hidden_comments`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| comment_id | uuid | No |  | `comments.id` |
| user_id | text | No |  |  |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `hidden_posts`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| post_id | text | No |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `likes`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| target_id | text | No |  |  |
| target_type | text | No |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `media_comments`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| media_id | text | No |  |  |
| post_id | text | No |  | `posts.id` |
| user_id | text | No |  |  |
| content | text | No |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |
| is_deleted | boolean | Yes |  |  |

---

### Table: `media_files`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| file_name | text | No |  |  |
| file_path | text | No |  |  |
| file_size | bigint | No |  |  |
| mime_type | text | No |  |  |
| file_type | text | No |  |  |
| bucket_name | text | No |  |  |
| is_public | boolean | Yes |  |  |
| metadata | jsonb | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| expires_at | timestamp without time zone | Yes |  |  |

---

### Table: `media_interactions`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| post_id | text | No |  | `posts.id` |
| media_id | text | No |  |  |
| media_index | integer | No |  |  |
| likes_count | integer | Yes |  |  |
| comments_count | integer | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |

---

### Table: `media_likes`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| media_id | text | No |  |  |
| post_id | text | No |  | `posts.id` |
| user_id | text | No |  |  |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `mentions`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| post_id | text | Yes |  |  |
| comment_id | uuid | Yes |  |  |
| mentioned_user_id | text | No |  | `users.uid` |
| mentioned_by_user_id | text | No |  | `users.uid` |
| mention_type | text | No |  |  |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `message_actions_history`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| action_type | text | Yes |  |  |
| message_ids | ARRAY | No |  |  |
| chat_id | text | Yes |  |  |
| action_data | jsonb | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `message_edit_history`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| message_id | uuid | No |  | `messages.id` |
| previous_content | text | No |  |  |
| edited_by | text | No |  |  |
| edited_at | bigint | No |  |  |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `message_forwards`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| original_message_id | uuid | No |  | `messages.id` |
| original_chat_id | text | No |  | `chats.chat_id` |
| forwarded_message_id | uuid | No |  | `messages.id` |
| forwarded_chat_id | text | No |  | `chats.chat_id` |
| forwarded_by | text | No |  | `users.uid` |
| forwarded_at | bigint | No |  |  |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `message_reactions`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| message_id | uuid | No |  |  |
| user_id | text | No |  |  |
| emoji | text | No |  |  |
| created_at | bigint | No |  |  |

---

### Table: `message_selection_sessions`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| chat_id | text | No |  |  |
| selected_message_ids | ARRAY | Yes |  |  |
| selection_mode | boolean | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| updated_at | timestamp without time zone | Yes |  |  |

---

### Table: `message_state_stats`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| chat_id | text | Yes |  |  |
| message_state | text | Yes |  |  |
| count | bigint | Yes |  |  |
| avg_delivery_time_ms | numeric | Yes |  |  |
| avg_read_time_ms | numeric | Yes |  |  |

---

### Table: `messages`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| chat_id | text | No |  |  |
| sender_id | text | No |  |  |
| content | text | No |  |  |
| message_type | text | Yes |  |  |
| media_url | text | Yes |  |  |
| media_type | text | Yes |  |  |
| media_size | bigint | Yes |  |  |
| media_duration | integer | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |
| is_deleted | boolean | Yes |  |  |
| is_edited | boolean | Yes |  |  |
| edit_history | jsonb | Yes |  |  |
| reply_to_id | uuid | Yes |  | `messages.id` |
| forwarded_from | uuid | Yes |  | `messages.id` |
| delivery_status | text | Yes |  |  |
| read_by | jsonb | Yes |  |  |
| reactions | jsonb | Yes |  |  |
| forwarded_from_message_id | uuid | Yes |  |  |
| forwarded_from_chat_id | text | Yes |  |  |
| delete_for_everyone | boolean | Yes |  |  |
| delivered_at | bigint | Yes |  |  |
| read_at | bigint | Yes |  |  |
| message_state | text | Yes |  |  |
| edited_at | bigint | Yes |  |  |
| attachments | jsonb | Yes |  |  |
| encrypted_content | jsonb | Yes |  |  |
| is_encrypted | boolean | Yes |  |  |
| nonce | text | Yes |  |  |
| ephemeral_public_key | text | Yes |  |  |
| deleted_for_users | jsonb | Yes |  |  |
| is_starred | boolean | Yes |  |  |
| starred_at | timestamp without time zone | Yes |  |  |
| starred_by | text | Yes |  |  |

---

### Table: `moderation_history`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| flagged_content_id | uuid | Yes |  | `flagged_content.id` |
| action_taken | text | No |  |  |
| performed_by | uuid | Yes |  | `users.id` |
| reason | text | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `moderation_rules`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| rule_name | text | No |  |  |
| rule_type | text | No |  |  |
| severity_level | integer | No |  |  |
| threshold_score | numeric | No |  |  |
| action | text | No |  |  |
| is_active | boolean | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |

---

### Table: `notifications`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| sender_id | text | Yes |  |  |
| type | text | No |  |  |
| title | text | Yes |  |  |
| message | text | No |  |  |
| data | jsonb | Yes |  |  |
| read | boolean | Yes |  |  |
| action_url | text | Yes |  |  |
| priority | text | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| read_at | timestamp without time zone | Yes |  |  |
| expires_at | timestamp without time zone | Yes |  |  |

---

### Table: `poll_votes`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| post_id | text | No |  | `posts.id` |
| user_id | text | No |  | `users.uid` |
| option_index | integer | No |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |

---

### Table: `post_hashtags`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| post_id | text | No |  |  |
| hashtag_id | uuid | No |  | `hashtags.id` |
| created_at | timestamp with time zone | Yes |  |  |

---

### Table: `post_likes`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| post_id | text | No |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `post_reports`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| reporter_id | text | No |  |  |
| post_id | text | No |  |  |
| reason | text | No |  |  |
| description | text | Yes |  |  |
| status | text | Yes |  |  |
| reviewed_by | text | Yes |  |  |
| reviewed_at | timestamp without time zone | Yes |  |  |
| action_taken | text | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `posts`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | text | No | Yes |  |
| key | text | Yes |  |  |
| author_uid | text | No |  | `users.uid` |
| post_text | text | Yes |  |  |
| post_image | text | Yes |  |  |
| post_type | text | Yes |  |  |
| post_hide_views_count | text | Yes |  |  |
| post_hide_like_count | text | Yes |  |  |
| post_hide_comments_count | text | Yes |  |  |
| post_disable_comments | text | Yes |  |  |
| post_visibility | text | Yes |  |  |
| publish_date | timestamp with time zone | Yes |  |  |
| timestamp | bigint | Yes |  |  |
| likes_count | integer | Yes |  |  |
| comments_count | integer | Yes |  |  |
| views_count | integer | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| updated_at | timestamp without time zone | Yes |  |  |
| media_items | jsonb | Yes |  |  |
| encrypted_content | jsonb | Yes |  |  |
| is_encrypted | boolean | Yes |  |  |
| nonce | text | Yes |  |  |
| encryption_key_id | text | Yes |  |  |
| is_deleted | boolean | Yes |  |  |
| is_edited | boolean | Yes |  |  |
| edited_at | timestamp with time zone | Yes |  |  |
| deleted_at | timestamp with time zone | Yes |  |  |
| has_poll | boolean | Yes |  |  |
| poll_question | text | Yes |  |  |
| poll_options | jsonb | Yes |  |  |
| poll_end_time | timestamp with time zone | Yes |  |  |
| poll_allow_multiple | boolean | Yes |  |  |
| has_location | boolean | Yes |  |  |
| location_name | text | Yes |  |  |
| location_address | text | Yes |  |  |
| location_latitude | double precision | Yes |  |  |
| location_longitude | double precision | Yes |  |  |
| location_place_id | text | Yes |  |  |
| reshares_count | integer | Yes |  |  |
| youtube_url | text | Yes |  |  |

---

### Table: `profile_history`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| key | text | No |  |  |
| user_id | text | No |  |  |
| image_url | text | No |  |  |
| upload_date | text | No |  |  |
| type | text | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| updated_at | timestamp without time zone | Yes |  |  |

---

### Table: `profile_likes`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| liker_uid | text | No |  |  |
| profile_uid | text | No |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `push_tokens`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| token | text | No |  |  |
| platform | text | No |  |  |
| device_id | text | Yes |  |  |
| is_active | boolean | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| updated_at | timestamp without time zone | Yes |  |  |

---

### Table: `reactions`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| post_id | text | No |  |  |
| user_id | text | No |  |  |
| reaction_type | text | No |  |  |
| created_at | timestamp with time zone | No |  |  |
| updated_at | timestamp with time zone | No |  |  |

---

### Table: `reports`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| reporter_id | text | No |  |  |
| reported_user_id | text | Yes |  |  |
| target_id | uuid | Yes |  |  |
| target_type | text | Yes |  |  |
| reason | text | No |  |  |
| description | text | Yes |  |  |
| status | text | Yes |  |  |
| reviewed_by | text | Yes |  |  |
| reviewed_at | timestamp without time zone | Yes |  |  |
| action_taken | text | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `reshares`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| post_id | text | No |  | `posts.id` |
| user_id | text | No |  | `users.uid` |
| reshare_text | text | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |

---

### Table: `stories`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| media_url | text | No |  |  |
| media_type | text | No |  |  |
| content | text | Yes |  |  |
| duration | integer | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| expires_at | timestamp without time zone | Yes |  |  |
| views_count | integer | Yes |  |  |
| is_active | boolean | Yes |  |  |
| thumbnail_url | text | Yes |  |  |
| duration_hours | integer | No |  |  |
| privacy_setting | character varying | No |  |  |
| media_width | integer | Yes |  |  |
| media_height | integer | Yes |  |  |
| media_duration_seconds | integer | Yes |  |  |
| file_size_bytes | bigint | Yes |  |  |
| reactions_count | integer | No |  |  |
| replies_count | integer | No |  |  |
| is_reported | boolean | No |  |  |
| moderation_status | character varying | Yes |  |  |

---

### Table: `story_archive`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | uuid | No |  |  |
| original_story_id | uuid | No |  |  |
| media_url | text | No |  |  |
| thumbnail_url | text | Yes |  |  |
| content | text | Yes |  |  |
| created_at | timestamp with time zone | No |  |  |
| archived_at | timestamp with time zone | No |  |  |
| views_count | integer | No |  |  |
| reactions_count | integer | No |  |  |

---

### Table: `story_custom_privacy`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| story_id | uuid | No |  | `stories.id` |
| allowed_user_id | uuid | No |  |  |

---

### Table: `story_hidden_from`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| story_id | uuid | No |  | `stories.id` |
| hidden_user_id | uuid | No |  |  |

---

### Table: `story_highlight_items`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| highlight_id | uuid | No |  | `story_highlights.id` |
| story_id | uuid | No |  | `stories.id` |
| display_order | integer | No |  |  |
| added_at | timestamp with time zone | No |  |  |

---

### Table: `story_highlights`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | uuid | No |  |  |
| title | character varying | No |  |  |
| cover_image_url | text | Yes |  |  |
| display_order | integer | No |  |  |
| created_at | timestamp with time zone | No |  |  |
| updated_at | timestamp with time zone | No |  |  |

---

### Table: `story_interactive_elements`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| story_id | uuid | No |  | `stories.id` |
| element_type | character varying | No |  |  |
| element_data | jsonb | No |  |  |
| position_x | double precision | Yes |  |  |
| position_y | double precision | Yes |  |  |
| created_at | timestamp with time zone | No |  |  |

---

### Table: `story_interactive_responses`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| element_id | uuid | No |  | `story_interactive_elements.id` |
| user_id | uuid | No |  |  |
| response_data | jsonb | No |  |  |
| created_at | timestamp with time zone | No |  |  |

---

### Table: `story_mentions`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| story_id | uuid | No |  | `stories.id` |
| mentioned_user_id | uuid | No |  |  |
| position_x | double precision | Yes |  |  |
| position_y | double precision | Yes |  |  |
| created_at | timestamp with time zone | No |  |  |

---

### Table: `story_reactions`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| story_id | uuid | No |  | `stories.id` |
| user_id | uuid | No |  |  |
| reaction_type | character varying | No |  |  |
| created_at | timestamp with time zone | No |  |  |

---

### Table: `story_replies`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| story_id | uuid | No |  | `stories.id` |
| sender_id | uuid | No |  |  |
| message | text | No |  |  |
| created_at | timestamp with time zone | No |  |  |
| is_read | boolean | No |  |  |

---

### Table: `story_views`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| story_id | uuid | No |  | `stories.id` |
| viewer_id | text | No |  |  |
| viewed_at | timestamp without time zone | Yes |  |  |

---

### Table: `syra_content_templates`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| template_type | text | Yes |  |  |
| content_template | text | No |  |  |
| variables | jsonb | Yes |  |  |
| usage_count | integer | Yes |  |  |
| success_rate | double precision | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `syra_engagement_metrics`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| interaction_type | text | Yes |  |  |
| user_id | text | Yes |  | `users.uid` |
| content_id | text | Yes |  |  |
| engagement_score | double precision | Yes |  |  |
| sentiment | text | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `tag_requests`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| post_id | text | No |  | `posts.id` |
| tagged_user_id | text | No |  | `users.uid` |
| tagged_by_user_id | text | No |  | `users.uid` |
| status | text | No |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| responded_at | timestamp with time zone | Yes |  |  |

---

### Table: `typing_activity_stats`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| chat_id | text | Yes |  |  |
| total_typing_events | bigint | Yes |  |  |
| active_typing_count | bigint | Yes |  |  |
| last_activity_timestamp | bigint | Yes |  |  |
| avg_timestamp | numeric | Yes |  |  |

---

### Table: `typing_status`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| chat_id | text | No |  |  |
| user_id | text | No |  |  |
| is_typing | boolean | No |  |  |
| timestamp | bigint | No |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| updated_at | timestamp without time zone | Yes |  |  |

---

### Table: `user_api_keys`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | uuid | No |  |  |
| key_hash | text | No |  |  |
| key_prefix | text | No |  |  |
| name | text | Yes |  |  |
| status | text | No |  |  |
| last_used_at | timestamp with time zone | Yes |  |  |
| created_at | timestamp with time zone | No |  |  |
| revoked_at | timestamp with time zone | Yes |  |  |

---

### Table: `user_deleted_messages`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| message_id | uuid | No |  | `messages.id` |
| deleted_at | timestamp with time zone | No |  |  |

---

### Table: `user_presence`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| is_online | boolean | Yes |  |  |
| last_seen | timestamp with time zone | Yes |  |  |
| activity_status | text | Yes |  |  |
| current_chat_id | text | Yes |  |  |
| device_info | jsonb | Yes |  |  |
| updated_at | timestamp without time zone | Yes |  |  |

---

### Table: `user_public_keys`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| user_id | uuid | No | Yes |  |
| public_key | text | No |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |
| key_version | integer | Yes |  |  |

---

### Table: `user_reports`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| reporter_id | text | No |  |  |
| reported_user_id | text | No |  |  |
| reason | text | No |  |  |
| description | text | Yes |  |  |
| status | text | Yes |  |  |
| reviewed_by | text | Yes |  |  |
| reviewed_at | timestamp without time zone | Yes |  |  |
| action_taken | text | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |

---

### Table: `user_settings`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| user_id | text | No |  |  |
| privacy_settings | jsonb | Yes |  |  |
| notification_settings | jsonb | Yes |  |  |
| app_settings | jsonb | Yes |  |  |
| created_at | timestamp without time zone | Yes |  |  |
| updated_at | timestamp without time zone | Yes |  |  |

---

### Table: `usernames`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| username | text | No |  |  |
| user_id | text | Yes |  | `users.uid` |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |
| uid | text | Yes |  |  |
| email | text | Yes |  |  |

---

### Table: `users`

#### Columns
| Name | Type | Nullable | Primary Key | Foreign Key |
|------|------|----------|-------------|-------------|
| id | uuid | No | Yes |  |
| uid | text | No |  |  |
| email | text | Yes |  |  |
| username | text | Yes |  |  |
| nickname | text | Yes |  |  |
| display_name | text | Yes |  |  |
| bio | text | Yes |  |  |
| avatar | text | Yes |  |  |
| avatar_history_type | text | Yes |  |  |
| profile_cover_image | text | Yes |  |  |
| account_premium | boolean | Yes |  |  |
| user_level_xp | integer | Yes |  |  |
| verify | boolean | Yes |  |  |
| account_type | text | Yes |  |  |
| gender | text | Yes |  |  |
| banned | boolean | Yes |  |  |
| status | text | Yes |  |  |
| join_date | timestamp with time zone | Yes |  |  |
| one_signal_player_id | text | Yes |  |  |
| last_seen | timestamp with time zone | Yes |  |  |
| chatting_with | text | Yes |  |  |
| created_at | timestamp with time zone | Yes |  |  |
| updated_at | timestamp with time zone | Yes |  |  |
| followers_count | integer | Yes |  |  |
| following_count | integer | Yes |  |  |
| posts_count | integer | Yes |  |  |
| region | text | Yes |  |  |
| is_admin | boolean | Yes |  |  |
| is_ai_persona | boolean | Yes |  |  |

---
