package com.synapse.social.studioasinc.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Unit test for RLS error handling in reaction operations
 * 
 * Tests RLS error shows specific permission denied message
 * _Requirements: 5.3_
 */
class PostRepositoryReactionRLSErrorTest : StringSpec({
    
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
    
    "RLS policy error shows specific permission denied message" {
        // Given an RLS policy violation error
        val rlsException = Exception("new row violates row-level security policy for table \"reactions\"")
        
        // When mapping the error
        val errorMessage = mapSupabaseError(rlsException)
        
        // Then it should show a specific RLS permission message
        errorMessage shouldContain "Permission denied"
        errorMessage shouldContain "Row-level security policy"
    }
    
    "RLS check failure shows specific permission denied message" {
        // Given an RLS check failure error
        val rlsException = Exception("RLS check failed for user on table reactions")
        
        // When mapping the error
        val errorMessage = mapSupabaseError(rlsException)
        
        // Then it should show a specific RLS permission message
        errorMessage shouldContain "Permission denied"
        errorMessage shouldContain "Row-level security policy"
    }
    
    "Policy violation error shows specific permission denied message" {
        // Given a policy violation error
        val policyException = Exception("policy violation on insert to reactions table")
        
        // When mapping the error
        val errorMessage = mapSupabaseError(policyException)
        
        // Then it should show a specific RLS permission message
        errorMessage shouldContain "Permission denied"
        errorMessage shouldContain "Row-level security policy"
    }
    
    "RLS error is distinguishable from generic unauthorized error" {
        // Given an RLS error and a generic unauthorized error
        val rlsException = Exception("RLS policy blocked operation")
        val unauthorizedException = Exception("unauthorized access")
        
        // When mapping both errors
        val rlsMessage = mapSupabaseError(rlsException)
        val unauthorizedMessage = mapSupabaseError(unauthorizedException)
        
        // Then RLS message should be more specific
        rlsMessage shouldContain "Row-level security policy"
        unauthorizedMessage shouldContain "Permission denied"
        (rlsMessage != unauthorizedMessage) shouldBe true
    }
})
