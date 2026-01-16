package com.synapse.social.studioasinc.ui.components.mentions

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class MentionQuery(
    val query: String,
    val range: TextRange // Range to replace, e.g. "@ab"
)

object MentionHelper {
    /**
     * detects if the cursor is currently composing a mention (starts with @).
     * Returns the query string (without @) and the range of text to replace (including @).
     */
    fun getMentionQuery(value: TextFieldValue): MentionQuery? {
        val text = value.text
        val cursor = value.selection.start

        if (cursor < 0) return null
        
        // Look backwards from cursor for '@'
        var matchStart = -1
        for (i in (cursor - 1) downTo 0) {
            val c = text[i]
            if (c == '@') {
                matchStart = i
                break
            }
            // If we hit whitespace/newline before @, it's not a mention being typed at this cursor
            // (Assuming usernames are contiguous)
            if (c.isWhitespace()) {
                return null
            }
        }

        if (matchStart != -1) {
            // Ensure @ is at start or preceded by whitespace
            if (matchStart == 0 || text[matchStart - 1].isWhitespace()) {
                val query = text.substring(matchStart + 1, cursor)
                // Optional: Limit query length or allowed characters
                return MentionQuery(query, TextRange(matchStart, cursor))
            }
        }

        return null
    }
}
