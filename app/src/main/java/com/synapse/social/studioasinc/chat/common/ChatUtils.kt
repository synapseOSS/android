package com.synapse.social.studioasinc.chat.common

import android.content.Context
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for chat features
 */
object ChatUtils {
    
    /**
     * Format timestamp for message display
     */
    fun formatMessageTime(timestamp: Long, context: Context? = null): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < DateUtils.MINUTE_IN_MILLIS -> "Just now"
            diff < DateUtils.HOUR_IN_MILLIS -> {
                val minutes = (diff / DateUtils.MINUTE_IN_MILLIS).toInt()
                "$minutes min ago"
            }
            isToday(timestamp) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            isYesterday(timestamp) -> {
                "Yesterday ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
            }
            isThisWeek(timestamp) -> {
                SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            isThisYear(timestamp) -> {
                SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
    
    /**
     * Format timestamp for chat list display
     */
    fun formatChatListTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < DateUtils.MINUTE_IN_MILLIS -> "Now"
            diff < DateUtils.HOUR_IN_MILLIS -> {
                val minutes = (diff / DateUtils.MINUTE_IN_MILLIS).toInt()
                "${minutes}m"
            }
            isToday(timestamp) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            isYesterday(timestamp) -> "Yesterday"
            isThisWeek(timestamp) -> {
                SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
            }
            isThisYear(timestamp) -> {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
    
    /**
     * Check if timestamp is today
     */
    fun isToday(timestamp: Long): Boolean {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        
        return messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                messageDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Check if timestamp is yesterday
     */
    fun isYesterday(timestamp: Long): Boolean {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        
        return messageDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                messageDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Check if timestamp is this week
     */
    fun isThisWeek(timestamp: Long): Boolean {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        
        val weekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        
        return messageDate.after(weekAgo) && messageDate.before(today)
    }
    
    /**
     * Check if timestamp is this year
     */
    fun isThisYear(timestamp: Long): Boolean {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        
        return messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    }
    
    /**
     * Truncate message text for preview
     */
    fun truncateMessage(message: String, maxLength: Int = 50): String {
        return if (message.length > maxLength) {
            "${message.substring(0, maxLength)}..."
        } else {
            message
        }
    }
    
    /**
     * Get message type display text
     */
    fun getMessageTypeDisplay(messageType: String): String {
        return when (messageType) {
            "text" -> ""
            "image" -> "📷 Photo"
            "video" -> "🎥 Video"
            "audio" -> "🎵 Audio"
            "voice" -> "🎤 Voice message"
            "file" -> "📎 File"
            "location" -> "📍 Location"
            else -> ""
        }
    }
    
    /**
     * Validate message content
     */
    fun isValidMessage(content: String?): Boolean {
        return !content.isNullOrBlank() && content.trim().isNotEmpty()
    }
    
    /**
     * Generate chat ID for direct messages
     */
    fun generateDirectChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "dm_${userId1}_${userId2}"
        } else {
            "dm_${userId2}_${userId1}"
        }
    }
    
    /**
     * Extract URLs from message text
     */
    fun extractUrls(text: String): List<String> {
        val urlPattern = Regex(
            "https?://[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*?"
        )
        return urlPattern.findAll(text).map { it.value }.toList()
    }
    
    /**
     * Check if message contains URL
     */
    fun containsUrl(text: String): Boolean {
        return extractUrls(text).isNotEmpty()
    }
    
    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * Format duration for audio/video
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = durationMs / (1000 * 60 * 60)
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Get unread count display text
     */
    fun getUnreadCountDisplay(count: Int): String {
        return when {
            count == 0 -> ""
            count < 100 -> count.toString()
            else -> "99+"
        }
    }
    
    /**
     * Sanitize message content
     */
    fun sanitizeMessage(content: String): String {
        return content.trim()
            .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
            .take(4000) // Limit message length
    }
}
