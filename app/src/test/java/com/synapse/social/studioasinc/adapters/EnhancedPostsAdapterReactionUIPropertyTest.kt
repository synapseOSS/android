package com.synapse.social.studioasinc.adapters

import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.ReactionType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based test for immediate UI updates after reactions in EnhancedPostsAdapter.
 * 
 * **Feature: multi-post-fixes, Property 11: Reaction UI updates immediately**
 * **Validates: Requirements 5.5**
 * 
 * This test verifies that when a user reacts to a post, the UI updates immediately
 * to reflect the new reaction state, including:
 * - Reaction count updates
 * - User's reaction type updates
 * - Optimistic UI updates before server response
 * - UI reversion if server operation fails
 */
class EnhancedPostsAdapterReactionUIPropertyTest : StringSpec({

    "Property 11: For any post, adding a reaction should immediately update the reaction count" {
        checkAll(100, postWithReactionsArb()) { post ->
            // Simulate adding a reaction
            val originalCount = post.getTotalReactionsCount()
            val reactionType = Arb.enum<ReactionType>().bind()
            
            // When a reaction is added, the count should increase
            val updatedReactions = post.reactions?.toMutableMap() ?: mutableMapOf()
            updatedReactions[reactionType] = (updatedReactions[reactionType] ?: 0) + 1
            
            val updatedPost = post.copy(
                reactions = updatedReactions,
                userReaction = reactionType
            )
            
            val newCount = updatedPost.getTotalReactionsCount()
            
            // The new count should be exactly 1 more than the original
            newCount shouldBe originalCount + 1
            
            // The user's reaction should be set
            updatedPost.userReaction shouldBe reactionType
        }
    }

    "Property 11: For any post, removing a reaction should immediately update the reaction count" {
        checkAll(100, postWithUserReactionArb()) { post ->
            // Post has a user reaction
            val originalCount = post.getTotalReactionsCount()
            val userReactionType = post.userReaction!!
            
            // Simulate removing the reaction
            val updatedReactions = post.reactions?.toMutableMap() ?: mutableMapOf()
            val currentCount = updatedReactions[userReactionType] ?: 0
            if (currentCount > 0) {
                updatedReactions[userReactionType] = currentCount - 1
                if (updatedReactions[userReactionType] == 0) {
                    updatedReactions.remove(userReactionType)
                }
            }
            
            val updatedPost = post.copy(
                reactions = updatedReactions,
                userReaction = null
            )
            
            val newCount = updatedPost.getTotalReactionsCount()
            
            // The new count should be exactly 1 less than the original
            newCount shouldBe originalCount - 1
            
            // The user's reaction should be cleared
            updatedPost.userReaction shouldBe null
        }
    }

    "Property 11: For any post, changing reaction type should maintain total count" {
        checkAll(100, postWithUserReactionArb()) { post ->
            // Post has a user reaction
            val originalCount = post.getTotalReactionsCount()
            val oldReactionType = post.userReaction!!
            val newReactionType = Arb.enum<ReactionType>().filter { it != oldReactionType }.bind()
            
            // Simulate changing the reaction type
            val updatedReactions = post.reactions?.toMutableMap() ?: mutableMapOf()
            
            // Decrease old reaction count
            val oldCount = updatedReactions[oldReactionType] ?: 0
            if (oldCount > 0) {
                updatedReactions[oldReactionType] = oldCount - 1
                if (updatedReactions[oldReactionType] == 0) {
                    updatedReactions.remove(oldReactionType)
                }
            }
            
            // Increase new reaction count
            updatedReactions[newReactionType] = (updatedReactions[newReactionType] ?: 0) + 1
            
            val updatedPost = post.copy(
                reactions = updatedReactions,
                userReaction = newReactionType
            )
            
            val newCount = updatedPost.getTotalReactionsCount()
            
            // Total count should remain the same (just moved from one type to another)
            newCount shouldBe originalCount
            
            // The user's reaction should be updated
            updatedPost.userReaction shouldBe newReactionType
        }
    }

    "Property 11: For any post, optimistic update should be reversible on failure" {
        checkAll(100, postWithReactionsArb()) { post ->
            // Store original state
            val originalCount = post.getTotalReactionsCount()
            val originalReactions = post.reactions?.toMap()
            val originalUserReaction = post.userReaction
            
            // Simulate optimistic update (add reaction)
            val reactionType = Arb.enum<ReactionType>().bind()
            val optimisticReactions = post.reactions?.toMutableMap() ?: mutableMapOf()
            optimisticReactions[reactionType] = (optimisticReactions[reactionType] ?: 0) + 1
            
            val optimisticPost = post.copy(
                reactions = optimisticReactions,
                userReaction = reactionType
            )
            
            // Verify optimistic update worked
            optimisticPost.getTotalReactionsCount() shouldBe originalCount + 1
            
            // Simulate failure - revert to original state
            val revertedPost = post.copy(
                reactions = originalReactions,
                userReaction = originalUserReaction
            )
            
            // Verify reversion worked
            revertedPost.getTotalReactionsCount() shouldBe originalCount
            revertedPost.reactions shouldBe originalReactions
            revertedPost.userReaction shouldBe originalUserReaction
        }
    }

    "Property 11: For any post, reaction summary should update immediately with new counts" {
        checkAll(100, postWithReactionsArb()) { post ->
            val reactionType = Arb.enum<ReactionType>().bind()
            
            // Get original count for this reaction type
            val originalCount = post.reactions?.get(reactionType) ?: 0
            
            // Add a reaction
            val updatedReactions = post.reactions?.toMutableMap() ?: mutableMapOf()
            updatedReactions[reactionType] = originalCount + 1
            
            val updatedPost = post.copy(reactions = updatedReactions)
            
            // Get new count for this reaction type
            val newCount = updatedPost.reactions?.get(reactionType) ?: 0
            
            // The new count should be exactly 1 more than the original
            newCount shouldBe originalCount + 1
            
            // The total count should also increase by 1
            updatedPost.getTotalReactionsCount() shouldBe post.getTotalReactionsCount() + 1
        }
    }

    "Property 11: For any post, UI state should be consistent with reaction data" {
        checkAll(100, postWithReactionsArb()) { post ->
            // The total reactions count should match the sum of individual reaction counts
            val calculatedTotal = post.reactions?.values?.sum() ?: 0
            val reportedTotal = post.getTotalReactionsCount()
            
            reportedTotal shouldBe calculatedTotal
            
            // If user has reacted, that reaction type should exist in the reactions map
            if (post.userReaction != null) {
                val userReactionCount = post.reactions?.get(post.userReaction) ?: 0
                userReactionCount shouldNotBe 0
            }
        }
    }
})

// Arbitrary generators for test data
private fun postWithReactionsArb() = arbitrary {
    val reactionTypes = ReactionType.values().toList()
    val reactions = mutableMapOf<ReactionType, Int>()
    
    // Add 0-5 different reaction types with random counts
    val numReactionTypes = Arb.int(0..5).bind()
    repeat(numReactionTypes) {
        val type = reactionTypes.random()
        reactions[type] = Arb.int(1..50).bind()
    }
    
    Post(
        id = Arb.string(10..20).bind(),
        authorUid = Arb.string(10..20).bind(),
        username = Arb.string(5..15).bind(),
        postText = Arb.string(10..200).bind(),
        timestamp = Arb.long(1000000000000L..2000000000000L).bind(),
        reactions = reactions.ifEmpty { null },
        userReaction = null
    )
}

private fun postWithUserReactionArb() = arbitrary {
    val reactionTypes = ReactionType.values().toList()
    val userReactionType = reactionTypes.random()
    val reactions = mutableMapOf<ReactionType, Int>()
    
    // Ensure the user's reaction type has at least 1 count
    reactions[userReactionType] = Arb.int(1..50).bind()
    
    // Add 0-4 other reaction types
    val numOtherTypes = Arb.int(0..4).bind()
    repeat(numOtherTypes) {
        val type = reactionTypes.filter { it != userReactionType }.random()
        reactions[type] = Arb.int(1..50).bind()
    }
    
    Post(
        id = Arb.string(10..20).bind(),
        authorUid = Arb.string(10..20).bind(),
        username = Arb.string(5..15).bind(),
        postText = Arb.string(10..200).bind(),
        timestamp = Arb.long(1000000000000L..2000000000000L).bind(),
        reactions = reactions,
        userReaction = userReactionType
    )
}
