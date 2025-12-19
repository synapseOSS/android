**Pattern:** Domain Model / DTO (Data Transfer Object)

**Adherence:**
*   Uses `@Serializable` for JSON mapping.
*   Encapsulates data structure.

**Violation:**
*   **Mixed Concerns:** Contains serialization logic (`@SerialName`), UI formatting logic (`getReactionSummary`), and business logic (`determinePostType`).
*   **Active Record Anti-Pattern:** The `toPost` helper suggests manual mapping often seen in Active Record styles, rather than using a proper Serializer/Mapper layer.
