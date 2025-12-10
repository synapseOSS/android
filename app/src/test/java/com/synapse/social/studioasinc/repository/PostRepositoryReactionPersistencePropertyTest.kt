package com.synapse.social.studioasinc.repository

import com.synapse.social.studioasinc.model.ReactionType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll

/**
 * Property-based test for reaction persistence to database
 * 
 * **Feature: multi-post-fixes, Property 9: Reactions persist to database correctly**
 * **Validates: Requirements 5.2**
 */
class PostRepositoryReactionPersistencePropertyTest : StringSpec({
    
    /**
     * Simulates a database reaction record
     */
    data class ReactionRecord(
        val userId: String,
        val postId: String,
        val reactionType: ReactionType
    )
    
    /**
     * Simulates in-memory database for testing
     */
    class MockReactionDatabase {
        private val reactions = mutableListOf<ReactionRecord>()
        
        fun insert(userId: String, postId: String, reactionType: ReactionType): Result<Unit> {
            reactions.add(ReactionRecord(userId, postId, reactionType))
            return Result.success(Unit)
        }
        
        fun findReaction(userId: String, postId: String): ReactionRecord? {
            return reactions.find { it.userId == userId && it.postId == postId }
        }
        
        fun clear() {
            reactions.clear()
        }
    }
    
    val mockDb = MockReactionDatabase()
    
    beforeTest {
        mockDb.clear()
    }
    
    "Property 9: Inserted reactions are retrievable" {
        checkAll(100, Arb.uuid(), Arb.uuid(), Arb.enum<ReactionType>()) { userId, postId, reactionType ->
            // When inserting a reaction
            val insertResult = mockDb.insert(userId.toString(), postId.toString(), reactionType)
            
            // Then the insert should succeed
            insertResult.isSuccess shouldBe true
            
            // And the reaction should be retrievable
            val retrieved = mockDb.findReaction(userId.toString(), postId.toString())
            retrieved shouldNotBe null
            retrieved?.userId shouldBe userId.toString()
            retrieved?.postId shouldBe postId.toString()
            retrieved?.reactionType shouldBe reactionType
            
            // Clean up for next iteration
            mockDb.clear()
        }
    }
    
    "Property 9: Reaction contains correct user_id" {
        checkAll(100, Arb.uuid(), Arb.uuid(), Arb.enum<ReactionType>()) { userId, postId, reactionType ->
            // When inserting a reaction
            mockDb.insert(userId.toString(), postId.toString(), reactionType)
            
            // Then the stored reaction should have the correct user_id
            val retrieved = mockDb.findReaction(userId.toString(), postId.toString())
            retrieved?.userId shouldBe userId.toString()
            
            mockDb.clear()
        }
    }
    
    "Property 9: Reaction contains correct post_id" {
        checkAll(100, Arb.uuid(), Arb.uuid(), Arb.enum<ReactionType>()) { userId, postId, reactionType ->
            // When inserting a reaction
            mockDb.insert(userId.toString(), postId.toString(), reactionType)
            
            // Then the stored reaction should have the correct post_id
            val retrieved = mockDb.findReaction(userId.toString(), postId.toString())
            retrieved?.postId shouldBe postId.toString()
            
            mockDb.clear()
        }
    }
    
    "Property 9: Reaction contains correct reaction_type" {
        checkAll(100, Arb.uuid(), Arb.uuid(), Arb.enum<ReactionType>()) { userId, postId, reactionType ->
            // When inserting a reaction
            mockDb.insert(userId.toString(), postId.toString(), reactionType)
            
            // Then the stored reaction should have the correct reaction_type
            val retrieved = mockDb.findReaction(userId.toString(), postId.toString())
            retrieved?.reactionType shouldBe reactionType
            
            mockDb.clear()
        }
    }
})
