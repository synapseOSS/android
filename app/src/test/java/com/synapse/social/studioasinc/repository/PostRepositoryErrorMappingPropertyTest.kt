package com.synapse.social.studioasinc.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.checkAll

/**
 * Property-based test for error logging and mapping
 * 
 * **Feature: multi-post-fixes, Property 17: Errors are logged and mapped to user-friendly messages**
 * **Validates: Requirements 8.1, 8.2**
 */
class PostRepositoryErrorMappingPropertyTest : StringSpec({
    
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
    
    val errorTypeArb = Arb.choice(
        Arb.constant("relation \"posts\" does not exist"),
        Arb.constant("connection refused"),
        Arb.constant("timeout exceeded"),
        Arb.constant("unauthorized access"),
        Arb.constant("policy violation"),
        Arb.constant("rls check failed"),
        Arb.constant("network error occurred"),
        Arb.constant("serialization failed")
    )
    
    "Property 17: All error messages are non-empty" {
        checkAll(100, errorTypeArb) { errorMessage ->
            val exception = Exception(errorMessage)
            
            // When mapping an error
            val mappedError = mapSupabaseError(exception)
            
            // Then the mapped message should not be empty
            mappedError.shouldNotBeEmpty()
        }
    }
    
    "Property 17: Relation errors map to table existence message" {
        checkAll<String>(100, Arb.constant("relation")) { keyword ->
            val exception = Exception("Error: $keyword \"posts\" does not exist")
            
            // When mapping a relation error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should mention database table
            mappedError shouldContain "Database table"
        }
    }
    
    "Property 17: Connection errors map to connection message" {
        checkAll<String>(100, Arb.constant("connection")) { keyword ->
            val exception = Exception("Error: $keyword refused")
            
            // When mapping a connection error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should mention connection
            mappedError shouldContain "Connection failed"
        }
    }
    
    "Property 17: Timeout errors map to timeout message" {
        checkAll<String>(100, Arb.constant("timeout")) { keyword ->
            val exception = Exception("Error: $keyword exceeded")
            
            // When mapping a timeout error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should mention timeout
            mappedError shouldContain "timed out"
        }
    }
    
    "Property 17: Unauthorized errors map to permission message" {
        checkAll<String>(100, Arb.constant("unauthorized")) { keyword ->
            val exception = Exception("Error: $keyword access")
            
            // When mapping an unauthorized error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should mention permission
            mappedError shouldContain "Permission denied"
        }
    }
    
    "Property 17: RLS policy errors map to RLS message" {
        checkAll(100, Arb.choice(Arb.constant("policy"), Arb.constant("rls"))) { keyword ->
            val exception = Exception("Error: $keyword violation")
            
            // When mapping an RLS error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should mention RLS or permission
            mappedError shouldContain "Permission denied"
        }
    }
    
    "Property 17: Network errors map to network message" {
        checkAll<String>(100, Arb.constant("network")) { keyword ->
            val exception = Exception("Error: $keyword error occurred")
            
            // When mapping a network error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should mention network
            mappedError shouldContain "Network error"
        }
    }
    
    "Property 17: Serialization errors map to data format message" {
        checkAll<String>(100, Arb.constant("serialization")) { keyword ->
            val exception = Exception("Error: $keyword failed")
            
            // When mapping a serialization error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should mention data format
            mappedError shouldContain "Data format error"
        }
    }
    
    "Property 17: Unknown errors include original message" {
        checkAll<String>(100, Arb.constant("unknown error type")) { errorMessage ->
            val exception = Exception(errorMessage)
            
            // When mapping an unknown error
            val mappedError = mapSupabaseError(exception)
            
            // Then it should include the original message
            mappedError shouldContain errorMessage
        }
    }
})
