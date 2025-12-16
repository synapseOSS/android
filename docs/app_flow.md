# App Flow

This document outlines the user flow and technical mechanisms of the application.

## 1. App Launch

- The application starts with the `SplashActivity`.
- The `SplashActivity` checks if the user is already logged in.
- If the user is logged in, they are redirected to the `MainActivity`.
- If the user is not logged in, they are redirected to the `LoginActivity`.

## 2. Authentication Flow

### 2.1. Login

- The `LoginActivity` presents the user with options to log in using:
  - Email and Password
  - Google
  - Facebook
  - Apple
- **Email and Password:**
  - The user enters their credentials.
  - The "Login" button triggers a validation check on the input fields.
  - If validation is successful, an API call is made to authenticate the user.
  - On successful authentication, the user is redirected to the `MainActivity`.
  - On failure, an error message is displayed.
- **Social Login (Google, Facebook, Apple):**
  - Tapping on a social login button initiates the respective SDK's authentication flow.
  - On successful authentication with the social provider, our backend is notified to create or log in the user.
  - On successful backend authentication, the user is redirected to the `MainActivity`.

### 2.2. Registration

- From the `LoginActivity`, the user can navigate to the `RegisterActivity`.
- The `RegisterActivity` presents a form for creating a new account.
- The user provides necessary information (e.g., name, email, password).
- The "Register" button validates the input and makes an API call to create the account.
- Upon successful registration, the user is redirected to the `MainActivity`.

## 3. Main Application Flow

### 3.1. MainActivity

- `MainActivity` is the central hub of the application.
- It contains a Bottom Navigation View with the following tabs:
  - **Home:** Displays the main feed of posts.
  - **Explore:** Allows users to discover new content and users.
  - **Create Post:** Opens the `CreatePostActivity`.
  - **Notifications:** Shows user notifications.
  - **Profile:** Displays the user's own profile.

### 3.2. Post Creation

- The user taps the "Create Post" button, which starts `CreatePostActivity`.
- In `CreatePostActivity`, the user can:
  - Write a text post.
  - Attach images or videos.
- The "Post" button uploads the content and creates the post via an API call.
- After the post is created, the user is redirected back to the `MainActivity`'s Home feed.

### 3.3. User Profile

- The `ProfileActivity` displays a user's profile information, including:
  - Profile picture and bio.
  - A grid of their posts.
  - Follower and following counts.
- From another user's profile, the current user can choose to "Follow" or "Unfollow" them.
- The "Edit Profile" button on the current user's own profile allows them to modify their information.

### 3.4. Settings

- The `SettingsActivity` can be accessed from the user's profile.
- It provides options for:
  - Account management.
  - Notification preferences.
  - Privacy settings.
  - Logging out.

## 4. Key Mechanisms

### 4.1. Data Persistence

- User data and posts are fetched from the remote API.
- `Room` database is used to cache data locally for offline access and improved performance.
- `Repository` pattern is used to abstract the data sources (network and local).

### 4.2. UI and Navigation

- The application uses a single-activity architecture with Fragments for most screens.
- Navigation between fragments is handled by the Android Navigation Component.
- `ViewModels` are used to hold and manage UI-related data in a lifecycle-conscious way.
- `DataBinding` and `ViewBinding` are used to connect layouts to UI logic.
- Asynchronous operations are handled using Kotlin Coroutines.
- Dependency injection is managed by Dagger/Hilt.
