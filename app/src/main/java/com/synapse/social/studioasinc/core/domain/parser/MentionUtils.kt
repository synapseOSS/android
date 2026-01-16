package com.synapse.social.studioasinc.core.domain.parser

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.ProfileActivity
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import com.synapse.social.studioasinc.NotificationHelper
import com.synapse.social.studioasinc.core.config.NotificationConfig
import com.synapse.social.studioasinc.data.local.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Utility class for handling user mentions in text with Supabase backend.
 * Provides functionality for clickable mentions and mention notifications.
 */
object MentionUtils {

    /**
     * Make mentions in text clickable and navigate to user profiles
     */
    fun handleMentions(context: Context, textView: TextView, text: String) {
        val spannableString = SpannableString(text)
        val pattern = Pattern.compile("@(\\w+)")
        val matcher = pattern.matcher(text)

        while (matcher.find()) {
            val username = matcher.group(1)
            if (username != null) {
                val start = matcher.start()
                val end = matcher.end()

                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        // Use Supabase to find user by username
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(AppDatabase.getDatabase(context.applicationContext).userDao())
                                val userResult = userRepository.getUserByUsername(username)
                                
                                userResult.fold(
                                    onSuccess = { user ->
                                        if (user != null) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                val intent = Intent(context, ProfileActivity::class.java)
                                                intent.putExtra("uid", user.uid)
                                                context.startActivity(intent)
                                            }
                                        }
                                    },
                                    onFailure = { error ->
                                        android.util.Log.e("MentionUtils", "Error finding user: ${error.message}")
                                    }
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("MentionUtils", "Error finding user: ${e.message}")
                            }
                        }
                    }

                    override fun updateDrawState(ds: android.text.TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        ds.color = ContextCompat.getColor(context, R.color.md_theme_primary)
                    }
                }
                spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        
        textView.text = spannableString
        textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    /**
     * Send notifications to mentioned users
     */
    fun sendMentionNotifications(
        context: Context,
        text: String, 
        postKey: String, 
        commentKey: String?, 
        contentType: String,
        coroutineScope: CoroutineScope
    ) {
        if (text.isBlank()) return

        val pattern = Pattern.compile("@(\\w+)")
        val matcher = pattern.matcher(text)

        val mentionedUsernames = mutableSetOf<String>()
        while (matcher.find()) {
            val username = matcher.group(1)
            if (username != null) {
                mentionedUsernames.add(username)
            }
        }

        if (mentionedUsernames.isEmpty()) return

        // Send notifications using Supabase
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(AppDatabase.getDatabase(context.applicationContext).userDao())
                
                for (username in mentionedUsernames) {
                    val userResult = userRepository.getUserByUsername(username)
                    userResult.fold(
                        onSuccess = { user ->
                            if (user != null) {
                                sendMentionNotification(context, user.uid, postKey, commentKey, contentType)
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("MentionUtils", "Error finding user $username: ${error.message}")
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("MentionUtils", "Error sending mention notifications: ${e.message}")
            }
        }
    }

    /**
     * Send a mention notification to a specific user
     */
    private suspend fun sendMentionNotification(
        context: Context,
        mentionedUid: String, 
        postKey: String, 
        commentKey: String?, 
        contentType: String
    ) {
        try {
            val authService = com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService()
            val userRepository = com.synapse.social.studioasinc.data.repository.UserRepository(AppDatabase.getDatabase(context.applicationContext).userDao())
            
            val currentUser = authService.getCurrentUser()
            if (currentUser == null || currentUser.id == mentionedUid) {
                return
            }

            val currentUserData = userRepository.getUserById(currentUser.id).getOrNull()
            val senderName = currentUserData?.username ?: "Someone"
            val message = "$senderName mentioned you in a $contentType"

            val data = hashMapOf<String, String>().apply {
                put("postId", postKey)
                commentKey?.let { put("commentId", it) }
            }

            NotificationHelper.sendNotification(
                mentionedUid,
                currentUser.id,
                message,
                NotificationConfig.NOTIFICATION_TYPE_MENTION,
                data
            )
        } catch (e: Exception) {
            android.util.Log.e("MentionUtils", "Failed to send mention notification: ${e.message}")
        }
    }

    /**
     * Extract mentioned usernames from text
     */
    fun extractMentions(text: String): List<String> {
        val pattern = Pattern.compile("@(\\w+)")
        val matcher = pattern.matcher(text)
        val mentions = mutableListOf<String>()
        
        while (matcher.find()) {
            val username = matcher.group(1)
            if (username != null && !mentions.contains(username)) {
                mentions.add(username)
            }
        }
        
        return mentions
    }
}
