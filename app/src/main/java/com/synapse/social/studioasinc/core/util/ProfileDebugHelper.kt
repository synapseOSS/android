package com.synapse.social.studioasinc.core.util

import android.util.Log
import com.synapse.social.studioasinc.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObject

/**
 * Helper class for debugging profile loading issues
 */
object ProfileDebugHelper {
    
    private const val TAG = "ProfileDebugHelper"
    
    /**
     * Debug current authentication state and user data
     */
    suspend fun debugCurrentUser() {
        try {
            val client = SupabaseClient.client
            val authUser = client.auth.currentUserOrNull()
            
            Log.d(TAG, "=== Profile Debug Info ===")
            
            if (authUser == null) {
                Log.e(TAG, "No authenticated user found")
                return
            }
            
            Log.d(TAG, "Auth User ID: ${authUser.id}")
            Log.d(TAG, "Auth User Email: ${authUser.email}")
            Log.d(TAG, "Auth User Created At: ${authUser.createdAt}")
            
            // Check if user exists in users table by auth ID
            val userByAuthId = client.from("users")
                .select(columns = Columns.raw("uid, username, email")) {
                    filter { eq("uid", authUser.id) }
                }
                .decodeSingleOrNull<JsonObject>()
            
            if (userByAuthId != null) {
                Log.d(TAG, "User found by auth ID: $userByAuthId")
            } else {
                Log.w(TAG, "User NOT found by auth ID in users table")
                
                // Try to find by email
                if (authUser.email != null) {
                    val userByEmail = client.from("users")
                        .select(columns = Columns.raw("uid, username, email")) {
                            filter { eq("email", authUser.email!!) }
                        }
                        .decodeSingleOrNull<JsonObject>()
                    
                    if (userByEmail != null) {
                        Log.d(TAG, "User found by email: $userByEmail")
                        Log.w(TAG, "UID mismatch detected! Auth ID: ${authUser.id}, DB UID: ${userByEmail["uid"]}")
                    } else {
                        Log.e(TAG, "User NOT found by email either")
                    }
                }
            }
            
            Log.d(TAG, "=== End Debug Info ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during debug", e)
        }
    }
    
    /**
     * Debug specific user profile loading
     */
    suspend fun debugUserProfile(uid: String) {
        try {
            val client = SupabaseClient.client
            
            Log.d(TAG, "=== Profile Debug for UID: $uid ===")
            
            val result = client.from("users")
                .select(columns = Columns.raw("*")) {
                    filter { eq("uid", uid) }
                }
                .decodeSingleOrNull<JsonObject>()
            
            if (result != null) {
                Log.d(TAG, "Profile found: $result")
                
                // Check for null/missing fields
                val requiredFields = listOf("uid", "username", "email")
                requiredFields.forEach { field ->
                    val value = result[field]
                    if (value == null) {
                        Log.w(TAG, "Required field '$field' is null")
                    } else {
                        Log.d(TAG, "$field: $value")
                    }
                }
            } else {
                Log.e(TAG, "Profile NOT found for UID: $uid")
            }
            
            Log.d(TAG, "=== End Profile Debug ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during profile debug", e)
        }
    }
}
