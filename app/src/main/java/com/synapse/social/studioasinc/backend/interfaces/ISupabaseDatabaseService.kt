package com.synapse.social.studioasinc.backend.interfaces

/**
 * Defines the contract for Supabase database operations.
 */
interface ISupabaseDatabaseService {
    /**
     * Select multiple records from a table.
     */
    suspend fun <T> select(table: String, columns: String = "*"): List<T>

    /**
     * Select a single record from a table.
     */
    suspend fun <T> selectSingle(table: String, columns: String = "*"): T?

    /**
     * Insert a new record into a table.
     */
    suspend fun insert(table: String, data: Map<String, Any?>): Map<String, Any?>

    /**
     * Update records in a table.
     */
    suspend fun update(table: String, data: Map<String, Any?>): Map<String, Any?>

    /**
     * Delete records from a table.
     */
    suspend fun delete(table: String): Boolean

    /**
     * Upsert (insert or update) a record in a table.
     */
    suspend fun upsert(table: String, data: Map<String, Any?>): Map<String, Any?>

    /**
     * Execute a custom query with filters.
     */
    suspend fun <T> selectWithFilter(
        table: String, 
        columns: String = "*",
        filter: (query: Any) -> Any
    ): List<T>
}
