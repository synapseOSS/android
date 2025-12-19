### Pattern: Repository / Data Source

This component functions as a **Remote Data Source** in Clean Architecture.

*   **Adherence**:
    *   **Abstraction**: Hides the complexity of raw Supabase queries from the domain/UI layer.
    *   **Thread Safety**: Manages its own threading (`Dispatchers.IO`), fulfilling the contract that data sources should be main-safe.

*   **Violations**:
    *   **Business Logic Leak**: Contains business logic (updating counts manually, filtering suggested users client-side) that belongs in a Domain UseCase or Backend Trigger.
    *   **Return Types**: Returning `Map<String, Any?>` leaks implementation details (JSON structure) to the consumer. It should map to Domain Entities.
