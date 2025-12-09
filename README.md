**Synapse** is a modern, feature-rich, cross-platform social media. This repository contains the source code for the Android client of Synapse, serving for keeping the project transparent.

## 📝 Overview

**Synapse** is designed to provide a seamless mult-platform social experience, integrating real-time communication, media sharing, and AI-driven interactions with vast features and customizations.

## ✨ Key highlights

* **Social Feed & Discovery:**
    * Rich media posts (Text, Images, Videos, Polls, audio and more).
    * Reels and Stories integration.
    * Global and User Search functionality.
* **Real-time Communication:**
    * Direct Messaging and Group Chats.
    * Multimedia sharing (Photos, Videos, Audio, Documents, Raw Binary, ).
    * Voice and Video call capabilities.
    * Typing indicators and read receipts.
* **AI Integration:**
    * Powered by **Google Gemini** (via Firebase Genkit) and other providers (soon).
    * AI-generated content summaries and smart interactions suggestion, Generate photo send videos directly from Synapse, suggest reply, smart content suggestion, AI powered search and more.
* **User Profile & Privacy:**
    * Comprehensive profile management and customization.
    * Granular privacy settings (Block, Mute, Profile Locking).
    * Follower/Following management.
    * QR Code profile sharing.
* **Notifications:**
    * Real-time notifications

## 🛠 Technology Stack

For nerds, the application is built using a modern Android tech stack, prioritizing performance and maintainability:

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3 Design System) & XML (Legacy support)
* **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles
* **Asynchronous Processing:** Kotlin Coroutines & Flow
* **Backend Services:**
    * **Supabase:** Authentication, Database (PostgreSQL), Realtime subscriptions, and Storage.
    * **Firebase Vertex AI:** Generative AI integration.
* **Local Data:** Room Database & DataStore
* **Media Handling:**
    * **Image Loading:** Coil & Glide
    * **Video Playback:** AndroidX Media3 (ExoPlayer)
    * **Image Manipulation:** uCrop / Android Image Cropper
* **Networking:** Ktor & OkHttp

## ⚖️ License and Usage

**Source Available License**

This source code is made available for viewing and reference purposes only.

* **No Contributions:** We are not accepting public PR. This project is maintained via internal team only.
* **No Modification:** You may not modify, fork, or build upon this source code. the source code is available only for keeping this project transparent.
* **No Commercial Use:** You may not use this source code for commercial purposes.

All rights reserved by the **StudioAs Inc. 2025**