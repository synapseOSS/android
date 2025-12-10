package com.synapse.social.studioasinc

import android.view.View

/**
 * Chat Adapter Listener Interface
 * Handles interactions with chat messages in the adapter
 * 
 * Note: This is a legacy interface. New code should use
 * com.synapse.social.studioasinc.chat.interfaces.ChatAdapterListener
 */
interface ChatAdapterListener {
    fun scrollToMessage(messageId: String)
    fun performHapticFeedback()
    fun showMessageOverviewPopup(anchor: View, position: Int, data: ArrayList<HashMap<String, Any?>>)
    fun openUrl(url: String)
    fun getRecipientUid(): String
}
