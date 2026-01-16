package com.synapse.social.studioasinc.data.remote.services.interfaces

/**
 * Database Service Interface
 */
interface IDatabaseService {
    suspend fun update(table: String, data: Map<String, Any?>, filter: String, value: Any): Result<Unit>
    suspend fun select(table: String, columns: String = "*"): Result<List<Map<String, Any?>>>
    suspend fun selectWhere(table: String, columns: String = "*", filter: String, value: Any): Result<List<Map<String, Any?>>>
    suspend fun selectWhereIn(table: String, columns: String = "*", filter: String, values: List<Any>): Result<List<Map<String, Any?>>>
    suspend fun delete(table: String, filter: String, value: Any): Result<Unit>
    suspend fun deleteWhere(table: String, filter: String, value: Any): Result<Unit>
    suspend fun count(table: String): Result<Long>
    suspend fun exists(table: String, filter: String, value: Any): Result<Boolean>
    suspend fun upsert(table: String, data: Map<String, Any?>): Result<Unit>
}
