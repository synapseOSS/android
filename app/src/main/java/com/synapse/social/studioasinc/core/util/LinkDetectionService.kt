package com.synapse.social.studioasinc.core.util

import android.util.Patterns
import java.util.regex.Pattern

/**
 * Service for detecting and extracting URLs from text
 */
object LinkDetectionService {
    
    // URL pattern that matches common web URLs
    private val urlPattern: Pattern = Patterns.WEB_URL
    
    /**
     * Extract all URLs from the given text
     * @param text The text to search for URLs
     * @return List of found URLs
     */
    fun extractUrls(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        
        val urls = mutableListOf<String>()
        val matcher = urlPattern.matcher(text)
        
        while (matcher.find()) {
            val url = matcher.group()
            if (url != null && isValidUrl(url)) {
                urls.add(normalizeUrl(url))
            }
        }
        
        return urls.distinct()
    }
    
    /**
     * Extract the first URL from the given text
     * @param text The text to search for URLs
     * @return The first found URL, or null if none found
     */
    fun extractFirstUrl(text: String): String? {
        if (text.isBlank()) return null
        
        val matcher = urlPattern.matcher(text)
        
        if (matcher.find()) {
            val url = matcher.group()
            if (url != null && isValidUrl(url)) {
                return normalizeUrl(url)
            }
        }
        
        return null
    }
    
    /**
     * Check if text contains any URLs
     * @param text The text to check
     * @return true if the text contains at least one URL
     */
    fun containsUrl(text: String): Boolean {
        if (text.isBlank()) return false
        return urlPattern.matcher(text).find()
    }
    
    /**
     * Validate that a string is a properly formatted URL
     */
    private fun isValidUrl(url: String): Boolean {
        // Filter out very short matches that are likely false positives
        if (url.length < 4) return false
        
        // Must contain a dot (for domain)
        if (!url.contains(".")) return false
        
        // Filter out domain-only matches without TLD
        val parts = url.replace("https://", "").replace("http://", "").split(".")
        if (parts.isEmpty()) return false
        
        // The TLD should be at least 2 characters
        val tld = parts.lastOrNull()?.split("/")?.firstOrNull() ?: ""
        if (tld.length < 2) return false
        
        return true
    }
    
    /**
     * Normalize URL by ensuring it has a protocol
     */
    private fun normalizeUrl(url: String): String {
        return if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
    }
    
    /**
     * Extract the domain from a URL
     * @param url The URL to extract domain from
     * @return The domain (e.g., "github.com")
     */
    fun extractDomain(url: String): String {
        return try {
            val normalized = normalizeUrl(url)
            val withoutProtocol = normalized
                .replace("https://", "")
                .replace("http://", "")
            
            // Get the domain part (before first /)
            val domain = withoutProtocol.split("/").firstOrNull() ?: withoutProtocol
            
            // Remove www. prefix if present
            domain.removePrefix("www.")
        } catch (e: Exception) {
            url
        }
    }
}
