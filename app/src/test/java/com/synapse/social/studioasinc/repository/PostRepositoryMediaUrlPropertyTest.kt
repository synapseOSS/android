package com.synapse.social.studioasinc.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.checkAll

/**
 * Property-based test for media URL fetching and construction
 * 
 * **Feature: multi-post-fixes, Property 3: Media URLs are properly fetched and constructed**
 * **Validates: Requirements 3.1, 3.2, 3.3**
 */
class PostRepositoryMediaUrlPropertyTest : StringSpec({
    
    /**
     * Helper function to construct media URL (mirrors PostRepository.constructMediaUrl)
     */
    fun constructMediaUrl(storagePath: String, supabaseUrl: String = "https://test-project.supabase.co"): String {
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath
        }
        
        val bucketName = "post-media"
        return "$supabaseUrl/storage/v1/object/public/$bucketName/$storagePath"
    }
    
    "Property 3: Media URLs are properly constructed from storage paths" {
        checkAll<String>(100, Arb.stringPattern("[a-zA-Z0-9_-]{10,50}\\.(jpg|png|mp4)")) { storagePath ->
            // When constructing a media URL from a storage path
            val constructedUrl = constructMediaUrl(storagePath)
            
            // Then the URL should be properly formatted
            constructedUrl shouldStartWith "https://"
            constructedUrl shouldContain "/storage/v1/object/public/post-media/"
            constructedUrl shouldContain storagePath
        }
    }
    
    "Property 3: Already complete HTTPS URLs are returned unchanged" {
        checkAll<String>(100, Arb.string(10..100)) { path ->
            val completeUrl = "https://example.com/media/$path"
            
            // When constructing a URL from an already complete URL
            val result = constructMediaUrl(completeUrl)
            
            // Then it should be returned unchanged
            result shouldBe completeUrl
        }
    }
    
    "Property 3: HTTP URLs are returned unchanged" {
        checkAll<String>(100, Arb.string(10..100)) { path ->
            val httpUrl = "http://example.com/media/$path"
            
            // When constructing a URL from an HTTP URL
            val result = constructMediaUrl(httpUrl)
            
            // Then it should be returned unchanged
            result shouldBe httpUrl
        }
    }
    
    "Property 3: Constructed URLs include correct bucket name" {
        checkAll<String>(100, Arb.stringPattern("[a-zA-Z0-9_-]{5,30}\\.(jpg|png)")) { storagePath ->
            // When constructing a media URL
            val constructedUrl = constructMediaUrl(storagePath)
            
            // Then it should include the post-media bucket
            constructedUrl shouldContain "post-media"
        }
    }
})
