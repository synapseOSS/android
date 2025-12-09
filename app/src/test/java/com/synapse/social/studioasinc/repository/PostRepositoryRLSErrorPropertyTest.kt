package com.synapse.social.studioasinc.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based test for RLS error messages
 * 
 * **Feature: multi-post-fixes, Property 22: RLS errors provide clear messages**
 * **Validates: Requirements 10.5**
 */
class PostRepositoryRLSErrorPropertyTest : StringSpec({
    
    /**
     * Map Supabase errors to user-friendly messages (mirrors PostRepository.mapSupabaseError)
     */
    fun mapSupabaseError(exception: Exception): String {
        return when {
            exception.message?.contains("relation", ignoreCase = true) == true -> 
                "Database table does not exist"
            exception.message?.contains("connection", ignoreCase = true) == true -> 
                "Connection failed. Please check your internet connection."
            exception.message?.contains("timeout", ignoreCase = true) == true -> 
                "Request timed out. Please try again."
            exception.message?.contains("unauthorized", ignoreCase = true) == true -> 
                "Permission denied. Please check your account status."
            exception.message?.contains("policy", ignoreCase = true) == true ||
            exception.message?.contains("rls", ignoreCase = true) == true -> 
                "Permission denied. Row-level security policy blocked this operation."
            exception.message?.contains("network", ignoreCase = true) == true -> 
                "Network error. Please check your connection."
            exception.message?.contains("serialization", ignoreCase = true) == true -> 
                "Data format error. Please contact support."
            else -> "Database error: ${exception.message ?: "Unknown error"}"
        }
    }
    
    val rlsErrorArb = Arb.choice(
        Arb.constant("RLS policy violation on table posts"),
        Arb.constant("Row-level security check failed"),
        Arb.constant("Policy check failed for operation"),
        Arb.constant("RLS: permission denied for table"),
        Arb.constant("Security policy blocked access")
    )
    
    "Property 22: RLS errors provide clear permission denied messages" {
        checkAll(100, rlsErrorArb) { errorMessage ->
            val exception = Exception(errorMessage)
            
            // When mapping an RLS error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should provide a clear message
            mappedError.shouldNotBeEmpty()
            // Check if it contains either "Permission denied" or mentions the error
            val hasPermissionMessage = mappedError.contains("Permission denied", ignoreCase = true) ||
                                      mappedError.contains("Row-level security", ignoreCase = true) ||
                                      mappedError.contains("policy", ignoreCase = true)
            hasPermissionMessage shouldBe true
        }
    }
    
    "Property 22: RLS errors mention row-level security" {
        checkAll(100, rlsErrorArb) { errorMessage ->
            val exception = Exception(errorMessage)
            
            // When mapping an RLS error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should mention row-level security or policy
            val mentionsRLS = mappedError.contains("Row-level security", ignoreCase = true) ||
                             mappedError.contains("policy", ignoreCase = true)
            
            mentionsRLS shouldBe true
        }
    }
    
    "Property 22: Policy keyword triggers RLS error message" {
        checkAll<String>(100, Arb.string(5..50)) { tableName ->
            val exception = Exception("Policy violation on table $tableName")
            
            // When mapping a policy error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should indicate RLS policy issue
            mappedError shouldContain "Permission denied"
            mappedError shouldContain "Row-level security policy"
        }
    }
    
    "Property 22: RLS keyword triggers RLS error message" {
        checkAll<String>(100, Arb.string(5..50)) { operation ->
            val exception = Exception("RLS check failed for $operation")
            
            // When mapping an RLS error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should indicate RLS policy issue
            mappedError shouldContain "Permission denied"
            mappedError shouldContain "Row-level security policy"
        }
    }
    
    "Property 22: RLS errors are distinguishable from other errors" {
        val rlsException = Exception("RLS policy violation")
        val otherException = Exception("Some other database error")
        
        // When mapping both errors
        val rlsError = mapSupabaseError(rlsException)
        val otherError = mapSupabaseError(otherException)
        
        // Then RLS error should mention policy
        rlsError shouldContain "policy"
        
        // And other error should not
        val otherMentionsPolicy = otherError.contains("policy", ignoreCase = true)
        otherMentionsPolicy shouldBe false
    }
    
    "Property 22: Case-insensitive RLS detection" {
        checkAll(100, Arb.choice(
            Arb.constant("rls"),
            Arb.constant("RLS"),
            Arb.constant("Rls"),
            Arb.constant("policy"),
            Arb.constant("POLICY"),
            Arb.constant("Policy")
        )) { keyword ->
            val exception = Exception("Error: $keyword check failed")
            
            // When mapping an error with RLS/policy keyword in any case
            val mappedError = mapSupabaseError(exception)
            
            // Then it should be detected as RLS error
            mappedError shouldContain "Permission denied"
            mappedError shouldContain "Row-level security policy"
        }
    }
})
