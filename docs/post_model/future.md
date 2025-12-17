*   `HashMap.toPost` manual mapping is brittle; use `kotlinx.serialization` for network responses directly.
*   `var` fields (`reactions`, `mediaItems`) allow mutability; prefer `copy()` for immutability to ensure thread safety and predictable state updates in Flows.
*   Business logic (`determinePostType`, `getReactionSummary`) inside data class; consider extension functions or domain wrappers to keep the DTO pure.
*   `Post` mixes DB schema (`@SerialName`) with UI concerns (`getReactionSummary`). Separate Domain Model from Network DTO.
