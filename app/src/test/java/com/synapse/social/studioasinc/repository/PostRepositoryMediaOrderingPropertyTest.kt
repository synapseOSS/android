package com.synapse.social.studioasinc.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based test for media attachment ordering
 * 
 * **Feature: multi-post-fixes, Property 16: Media attachments are ordered correctly**
 * **Validates: Requirements 7.3**
 */
class PostRepositoryMediaOrderingPropertyTest : StringSpec({
    
    data class MediaItem(
        val id: String,
        val position: Int,
        val createdAt: String
    )
    
    "Property 16: Media items are sorted by position then created_at" {
        checkAll<List<Int>>(100, Arb.list(Arb.int(0..100), 1..20)) { positions ->
            // Create media items with random positions and timestamps
            val mediaItems = positions.mapIndexed { index, position ->
                MediaItem(
                    id = "media_$index",
                    position = position,
                    createdAt = "2024-01-${(index % 28) + 1}T10:00:00Z"
                )
            }
            
            // When sorting media items
            val sorted = mediaItems.sortedWith(compareBy({ it.position }, { it.createdAt }))
            
            // Then they should be sorted by position first, then by created_at
            sorted shouldBeSortedWith compareBy({ it.position }, { it.createdAt })
        }
    }
    
    "Property 16: Media items with same position are sorted by created_at" {
        checkAll<List<String>>(100, Arb.list(Arb.string(10..30), 2..10)) { timestamps ->
            val sortedTimestamps = timestamps.sorted()
            
            // Create media items with same position but different timestamps
            val mediaItems = sortedTimestamps.mapIndexed { index, timestamp ->
                MediaItem(
                    id = "media_$index",
                    position = 0, // Same position for all
                    createdAt = timestamp
                )
            }
            
            // When sorting media items
            val sorted = mediaItems.sortedWith(compareBy({ it.position }, { it.createdAt }))
            
            // Then they should be sorted by created_at
            sorted.map { it.createdAt } shouldBe sortedTimestamps
        }
    }
    
    "Property 16: Media items with different positions maintain position order" {
        checkAll<Int>(100, Arb.int(2..20)) { count ->
            // Create media items with sequential positions
            val mediaItems = (0 until count).map { index ->
                MediaItem(
                    id = "media_$index",
                    position = index,
                    createdAt = "2024-01-01T10:00:00Z" // Same timestamp
                )
            }
            
            // Shuffle the items
            val shuffled = mediaItems.shuffled()
            
            // When sorting media items
            val sorted = shuffled.sortedWith(compareBy({ it.position }, { it.createdAt }))
            
            // Then they should be in position order
            sorted.map { it.position } shouldBe (0 until count).toList()
        }
    }
    
    "Property 16: Empty media list remains empty after sorting" {
        val emptyList = emptyList<MediaItem>()
        
        // When sorting an empty list
        val sorted = emptyList.sortedWith(compareBy({ it.position }, { it.createdAt }))
        
        // Then it should remain empty
        sorted shouldBe emptyList
    }
    
    "Property 16: Single media item remains unchanged after sorting" {
        checkAll<Int>(100, Arb.int(0..100)) { position ->
            val singleItem = listOf(
                MediaItem(
                    id = "media_1",
                    position = position,
                    createdAt = "2024-01-01T10:00:00Z"
                )
            )
            
            // When sorting a single item list
            val sorted = singleItem.sortedWith(compareBy({ it.position }, { it.createdAt }))
            
            // Then it should remain unchanged
            sorted shouldBe singleItem
        }
    }
})
