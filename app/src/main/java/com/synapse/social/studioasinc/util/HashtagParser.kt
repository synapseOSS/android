package com.synapse.social.studioasinc.util

/**
 * Utility for extracting hashtags from text.
 * Requirement: 9.1
 */
object HashtagParser {
    private val HASHTAG_REGEX = "#(\\w+)".toRegex()
    
    /**
     * Extract hashtags from text without # prefix.
     */
    fun extractHashtags(text: String): List<String> =
        HASHTAG_REGEX.findAll(text)
            .map { it.groupValues[1] }
            .toList()
}
