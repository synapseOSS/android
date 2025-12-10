package com.synapse.social.studioasinc.util

import java.util.regex.Pattern

object LinkPreviewUtil {
    
    private val URL_PATTERN = Pattern.compile(
        "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)" +
        "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*" +
        "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
        Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL
    )
    
    /**
     * Extract URL from message text
     */
    fun extractUrl(text: String?): String? {
        if (text.isNullOrEmpty()) return null
        
        val matcher = URL_PATTERN.matcher(text)
        return if (matcher.find()) {
            var url = matcher.group()
            if (!url.startsWith("http")) {
                url = "https://$url"
            }
            url
        } else {
            null
        }
    }
    
    /**
     * Check if text contains a URL
     */
    fun containsUrl(text: String?): Boolean {
        return extractUrl(text) != null
    }
    
    /**
     * Extract domain from URL
     */
    fun extractDomain(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        
        return try {
            val uri = java.net.URI(url)
            uri.host
        } catch (e: Exception) {
            null
        }
    }
}
