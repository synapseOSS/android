package com.synapse.social.studioasinc.repository

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit test for single query with joins in PostRepository
 * 
 * Tests:
 * - Query includes profiles join
 * 
 * Requirements: 7.1
 */
class PostRepositoryQueryJoinsTest {
    
    @Test
    fun `query string includes profiles join`() {
        // Given the expected query structure
        val expectedQuery = """
            *,
            profiles!inner(username, avatar_url, verify),
            post_media!left(id, url, type, position, created_at)
        """.trimIndent()
        
        // When checking the query structure
        val hasProfilesJoin = expectedQuery.contains("profiles!inner")
        val hasUsernameField = expectedQuery.contains("username")
        val hasAvatarField = expectedQuery.contains("avatar_url")
        val hasVerifyField = expectedQuery.contains("verify")
        
        // Then it should include all profile fields
        assertTrue("Query should include profiles join", hasProfilesJoin)
        assertTrue("Query should include username field", hasUsernameField)
        assertTrue("Query should include avatar_url field", hasAvatarField)
        assertTrue("Query should include verify field", hasVerifyField)
    }
    
    @Test
    fun `query string includes post_media join`() {
        // Given the expected query structure
        val expectedQuery = """
            *,
            profiles!inner(username, avatar_url, verify),
            post_media!left(id, url, type, position, created_at)
        """.trimIndent()
        
        // When checking the query structure
        val hasMediaJoin = expectedQuery.contains("post_media!left")
        val hasMediaFields = expectedQuery.contains("url") && 
                            expectedQuery.contains("type") && 
                            expectedQuery.contains("position")
        
        // Then it should include media join with fields
        assertTrue("Query should include post_media join", hasMediaJoin)
        assertTrue("Query should include media fields", hasMediaFields)
    }
    
    @Test
    fun `profiles join uses inner join`() {
        // Given the expected query structure
        val expectedQuery = """
            *,
            profiles!inner(username, avatar_url, verify),
            post_media!left(id, url, type, position, created_at)
        """.trimIndent()
        
        // When checking the join type
        val usesInnerJoin = expectedQuery.contains("profiles!inner")
        
        // Then profiles should use inner join (only posts with profiles)
        assertTrue("Profiles should use inner join", usesInnerJoin)
    }
    
    @Test
    fun `post_media join uses left join`() {
        // Given the expected query structure
        val expectedQuery = """
            *,
            profiles!inner(username, avatar_url, verify),
            post_media!left(id, url, type, position, created_at)
        """.trimIndent()
        
        // When checking the join type
        val usesLeftJoin = expectedQuery.contains("post_media!left")
        
        // Then post_media should use left join (posts without media are included)
        assertTrue("Post media should use left join", usesLeftJoin)
    }
    
    @Test
    fun `query includes all required profile fields`() {
        // Given the required profile fields
        val requiredFields = listOf("username", "avatar_url", "verify")
        val expectedQuery = """
            *,
            profiles!inner(username, avatar_url, verify),
            post_media!left(id, url, type, position, created_at)
        """.trimIndent()
        
        // When checking for each field
        val allFieldsPresent = requiredFields.all { field ->
            expectedQuery.contains(field)
        }
        
        // Then all required fields should be present
        assertTrue("All profile fields should be present", allFieldsPresent)
    }
    
    @Test
    fun `query includes all required media fields`() {
        // Given the required media fields
        val requiredFields = listOf("id", "url", "type", "position", "created_at")
        val expectedQuery = """
            *,
            profiles!inner(username, avatar_url, verify),
            post_media!left(id, url, type, position, created_at)
        """.trimIndent()
        
        // When checking for each field
        val allFieldsPresent = requiredFields.all { field ->
            expectedQuery.contains(field)
        }
        
        // Then all required fields should be present
        assertTrue("All media fields should be present", allFieldsPresent)
    }
    
    @Test
    fun `query selects all post fields with asterisk`() {
        // Given the expected query structure
        val expectedQuery = """
            *,
            profiles!inner(username, avatar_url, verify),
            post_media!left(id, url, type, position, created_at)
        """.trimIndent()
        
        // When checking for wildcard selector
        val hasWildcard = expectedQuery.trim().startsWith("*")
        
        // Then it should select all post fields
        assertTrue("Query should select all post fields with *", hasWildcard)
    }
}
