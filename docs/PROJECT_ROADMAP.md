# Synapse Project - Comprehensive Task & Roadmap

## 1. Project Overview & Analysis

Synapse is a feature-rich social media application built with Android (Kotlin) and Supabase. The project is currently in a transitional phase, migrating from a legacy XML/Activity-based architecture to a modern Jetpack Compose/MVVM architecture.

### Current State
*   **Architecture:** Clean Architecture with MVVM, Hilt for Dependency Injection, and Repository pattern.
*   **UI Toolkit:** Hybrid. New features are built with Jetpack Compose (Material 3 Expressive), while legacy features still use XML Layouts and ViewBinding.
*   **Backend:** Supabase (Auth, Database, Storage, Realtime, Edge Functions). The database schema is very comprehensive, supporting features like Stories, AI Chat, Moderation, and Encrypted Messaging.
*   **Testing:** Basic unit tests (`test` folder) exist, but instrumentation tests (`androidTest` folder) are missing or not properly configured.

### Key Strengths
*   **Feature Set:** The backend supports a vast array of features (AI, Stories, Polls, Encryption) that put it on par with major social networks.
*   **Modernization:** The shift to Compose is well underway in key areas like Chat and Inbox.
*   **Design:** Adoption of Material 3 Expressive design patterns.

### Key Weaknesses & Gaps
*   **Tech Debt:** Split UI logic (Legacy vs Compose) creates maintenance overhead.
*   **Testing Gap:** Lack of UI/Integration tests poses a risk for regressions during refactoring.
*   **Directory Structure:** The root package `com.synapse.social.studioasinc` is cluttered with Activities and Utils that should be modularized.
*   **Feature Parity:** Some backend features (e.g., AI Personas, detailed moderation flows) might not be fully exposed in the UI yet.

---

## 2. Refactoring & Improvement Tasks (Immediate & Ongoing)

These tasks focus on code quality, stability, and developer experience.

### 2.1 Codebase Organization
*   **Task:** Clean up the root package. Move top-level Activities (`MainActivity`, `ChatActivity`) into a `presentation` or `ui` sub-package.
*   **Task:** Modularize features. Group related classes (data, domain, presentation) by feature (e.g., `feature/auth`, `feature/chat`, `feature/feed`) rather than by layer.

### 2.2 Complete Compose Migration
*   **Task:** Deprecate and replace `InboxActivity` (XML) with `InboxScreen` (Compose). Ensure `InboxScreen` is the entry point for messages.
*   **Task:** Migrate `ProfileActivity` to use `ProfileScreen` entirely.
*   **Goal:** Move towards a Single-Activity Architecture (`MainActivity` hosting a Navigation Graph) to reduce context switching overhead and improve navigation transitions.

### 2.3 Testing Infrastructure
*   **Task:** Create `androidTest` source set.
*   **Task:** Add Hilt testing dependencies.
*   **Task:** Write critical UI tests for `LoginFlow` and `PostCreationFlow` using Compose Test Rule.

### 2.4 Performance
*   **Task:** Review `HorizontalPager` usage in `InboxScreen`. Ensure lazy loading of tabs.
*   **Task:** Optimize image loading. Verify `Coil` usage respects image resizing to prevent OOM errors on large feeds.

---

## 3. New Features Implementation (Short-term & Future)

These features utilize existing backend capabilities to deliver high user value.

### 3.1 AI Integration (High Priority)
*   **Context:** `ai_chat_sessions` and `ai_persona_config` tables exist.
*   **Task:** Implement **AI Chat Interface**.
    *   Create a specific UI for chatting with AI assistants.
    *   Implement "Smart Replies" or "Message Summarization" in `DirectChatScreen` using the `ai_summaries` table.
*   **Task:** **Persona Editor**. Allow users to configure their AI Persona (personality traits, posting schedule) via a new Settings screen.

### 3.2 Enhanced Stories & Reels
*   **Context:** `stories` and `story_interactive_elements` tables exist.
*   **Task:** Implement **Interactive Elements** in Stories.
    *   Add UI for Polls, Questions, and Mention stickers in the Story Editor.
    *   Render these elements in `StoryViewer`.
*   **Task:** **Story Archives**. Implement a "Memories" view using `story_archive` data.

### 3.3 End-to-End Encryption (Security)
*   **Context:** `encrypted_content` columns exist in `messages` and `posts`.
*   **Task:** Implement client-side key generation and exchange.
*   **Task:** Add a visual indicator (lock icon) in the Chat UI for E2EE chats.

### 3.4 Monetization & Gamification
*   **Context:** `user_level_xp` and `account_premium` columns exist.
*   **Task:** **XP Progress Bar**. Show user level/XP in the Profile UI.
*   **Task:** **Premium Features**. Gate certain themes or custom app icons behind the `account_premium` flag.

---

## 4. Implementation Roadmap (Next Steps)

1.  **Phase 1 (Cleanup):** Reorganize file structure, fix `androidTest` setup.
2.  **Phase 2 (Migration):** Fully replace `InboxActivity` with the Compose version.
3.  **Phase 3 (Feature - AI):** Launch the AI Chat interface.
4.  **Phase 4 (Feature - Stories):** Add interactive stickers to Stories.

This document serves as a living guide for the development team. Update it as tasks are completed or new priorities emerge.
