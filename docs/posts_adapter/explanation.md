**Data Flow:**
*   Receives list of `Post` objects (See `/docs/post_model/explanation.md`) via `submitList`.
*   `PostDiffCallback` calculates difference on background thread to update UI efficiently.
*   `onBindViewHolder` binds `Post` data to `PostViewHolder`.

**Critical Functions:**
*   `onCreateViewHolder`: Inflates `item_post_md3` layout.
*   `bind`: Populates views. Handles Markdown rendering (`updatePostContent`). Loads avatar via `ImageLoader`. Sets up Accessibility content descriptions.
*   `PostCardAnimations`: Used for entrance animations, like state changes, and button clicks.

**Error Handling:**
*   `ImageLoader.loadImage` handles image loading failures (placeholder).
*   Markdown rendering falls back to plain text if `markwon` instance is null.

**State Management:**
*   `PostViewHolder` maintains local state (`previousPostText`, `previousLikesCount`, `isLiked`) to determine when to trigger specific animations (e.g., counting up likes).
*   `hasAnimatedEntrance` flag prevents re-animating items when scrolling back up.
