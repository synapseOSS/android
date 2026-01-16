package com.synapse.social.studioasinc.core.domain.parser

/**
 * Utility for extracting mentions from text.
 * Requirement: 9.3
 */
object MentionParser {
    private val MENTION_REGEX = "@(\\w+)".toRegex()
    
    /**
     * Extract mentions from text without @ prefix.
     */
    fun extractMentions(text: String): List<String> =
        MENTION_REGEX.findAll(text)
            .map { it.groupValues[1] }
            .toList()
}
