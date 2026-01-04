package com.synapse.social.studioasinc.chat

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.widget.TypingAnimationView

/**
 * Custom view component for displaying typing indicators in chat
 */
class TypingIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val typingAnimation: TypingAnimationView
    private val typingText: TextView
    private val container: View

    private var typingUsers = mutableListOf<String>()
    private var userDisplayNames = mutableMapOf<String, String>()

    init {
        LayoutInflater.from(context).inflate(R.layout.view_typing_indicator, this, true)
        
        typingAnimation = findViewById(R.id.typing_animation)
        typingText = findViewById(R.id.typing_text)
        container = findViewById(R.id.typing_container)
        
        // Initially hidden
        visibility = View.GONE
        
        setupView()
    }

    private fun setupView() {
        orientation = HORIZONTAL
        
        // Set up container styling
        container.background = context.getDrawable(R.drawable.typing_indicator_background)
        
        // Set up text styling
        typingText.setTextColor(context.getColor(R.color.md_theme_onSurfaceVariant))
        typingText.textSize = 14f
    }

    /**
     * Update typing users list
     */
    fun updateTypingUsers(users: List<String>, displayNames: Map<String, String> = emptyMap()) {
        typingUsers.clear()
        typingUsers.addAll(users)
        userDisplayNames.putAll(displayNames)
        
        updateDisplay()
    }

    /**
     * Add a typing user
     */
    fun addTypingUser(userId: String, displayName: String? = null) {
        if (!typingUsers.contains(userId)) {
            typingUsers.add(userId)
            displayName?.let { userDisplayNames[userId] = it }
            updateDisplay()
        }
    }

    /**
     * Remove a typing user
     */
    fun removeTypingUser(userId: String) {
        if (typingUsers.remove(userId)) {
            userDisplayNames.remove(userId)
            updateDisplay()
        }
    }

    /**
     * Clear all typing users
     */
    fun clearTypingUsers() {
        typingUsers.clear()
        userDisplayNames.clear()
        updateDisplay()
    }

    /**
     * Update the display based on current typing users
     */
    private fun updateDisplay() {
        if (typingUsers.isEmpty()) {
            hideTypingIndicator()
        } else {
            showTypingIndicator()
            updateTypingText()
        }
    }

    /**
     * Show typing indicator with animation
     */
    private fun showTypingIndicator() {
        if (!isVisible) {
            visibility = View.VISIBLE
            typingAnimation.startAnimation()
            
            // Animate in
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    /**
     * Hide typing indicator with animation
     */
    private fun hideTypingIndicator() {
        if (isVisible) {
            animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    visibility = View.GONE
                    typingAnimation.stopAnimation()
                }
                .start()
        }
    }

    /**
     * Update typing text based on number of users
     */
    private fun updateTypingText() {
        val text = when (typingUsers.size) {
            0 -> ""
            1 -> {
                val displayName = userDisplayNames[typingUsers[0]] ?: "Someone"
                "$displayName is typing..."
            }
            2 -> {
                val name1 = userDisplayNames[typingUsers[0]] ?: "Someone"
                val name2 = userDisplayNames[typingUsers[1]] ?: "Someone"
                "$name1 and $name2 are typing..."
            }
            else -> {
                val name1 = userDisplayNames[typingUsers[0]] ?: "Someone"
                val othersCount = typingUsers.size - 1
                "$name1 and $othersCount others are typing..."
            }
        }
        
        typingText.text = text
    }

    /**
     * Set custom typing message
     */
    fun setCustomTypingMessage(message: String) {
        typingText.text = message
        if (message.isNotEmpty()) {
            showTypingIndicator()
        } else {
            hideTypingIndicator()
        }
    }

    /**
     * Check if currently showing typing indicator
     */
    fun isShowingTyping(): Boolean {
        return isVisible && typingUsers.isNotEmpty()
    }

    /**
     * Get current typing users count
     */
    fun getTypingUsersCount(): Int {
        return typingUsers.size
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        typingAnimation.stopAnimation()
    }
}
