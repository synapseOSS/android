package com.synapse.social.studioasinc

/**
 * Chat Interaction Listener Interface
 * Handles chat-level interactions and events
 * 
 * Note: This is a legacy interface. New code should use
 * com.synapse.social.studioasinc.chat.interfaces.ChatInteractionListener
 */
interface ChatInteractionListener {
    fun onReplySelected(messageId: String)
    fun onDeleteMessage(messageData: HashMap<String, Any?>)
}
