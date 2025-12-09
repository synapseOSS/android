package com.synapse.social.studioasinc

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.auth.auth
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple message adapter for basic chat display
 * Supports sent/received messages with timestamps and delivery status
 */
class MessageAdapter(
    private val messages: ArrayList<HashMap<String, Any?>>,
    private val onMessageClick: ((String, Int) -> Unit)? = null,
    private val onMessageLongClick: ((String, Int) -> Boolean)? = null,
    private val onReplyClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val editedIndicator: TextView? = itemView.findViewById(R.id.editedIndicator)
        val messageTime: TextView? = itemView.findViewById(R.id.date)
        val messageStatus: ImageView? = itemView.findViewById(R.id.message_state)
        val messageBubble: LinearLayout? = itemView.findViewById(R.id.messageBG)
        val messageLayout: LinearLayout? = itemView.findViewById(R.id.message_layout)
        val bodyLayout: LinearLayout? = itemView.findViewById(R.id.body)
        
        // Reply indicator components
        val repliedMessageLayout: View? = itemView.findViewById(R.id.mRepliedMessageLayout)
        val repliedUsername: TextView? = itemView.findViewById(R.id.mRepliedMessageLayoutUsername)
        val repliedMessage: TextView? = itemView.findViewById(R.id.mRepliedMessageLayoutMessage)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val senderId = message["sender_id"]?.toString() 
            ?: message["uid"]?.toString()
        val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
        
        return if (senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = R.layout.chat_bubble_text
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val isMyMessage = getItemViewType(position) == VIEW_TYPE_SENT
        
        // Set message text - support both field names
        val messageText = message["content"]?.toString() 
            ?: message["message_text"]?.toString() 
            ?: ""
        holder.messageText.text = messageText
        
        // Set message time - support both field names
        val timestamp = message["created_at"]?.toString()?.toLongOrNull()
            ?: message["push_date"]?.toString()?.toLongOrNull()
            ?: System.currentTimeMillis()
        holder.messageTime?.text = formatMessageTime(timestamp)
        
        // Handle edited indicator display
        val isEdited = message["is_edited"]?.toString()?.toBooleanStrictOrNull() ?: false
        holder.editedIndicator?.let { editedView ->
            if (isEdited) {
                editedView.visibility = View.VISIBLE
                // Add click listener if callback is provided
                val messageId = message["id"]?.toString() ?: message["key"]?.toString() ?: ""
                editedView.setOnClickListener {
                    // For now, just show a toast since we don't have the full listener interface
                    // This can be enhanced later when MessageAdapter is fully integrated with ChatAdapterListener
                    android.widget.Toast.makeText(
                        holder.itemView.context,
                        "Edit history feature coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                editedView.visibility = View.GONE
                editedView.setOnClickListener(null)
            }
        }
        
        // Set message status for sent messages
        holder.messageStatus?.let { statusView ->
            if (isMyMessage) {
                val deliveryStatus = message["delivery_status"]?.toString() 
                    ?: message["message_state"]?.toString() 
                    ?: "sent"
                
                when (deliveryStatus) {
                    "sending" -> {
                        statusView.setImageResource(R.drawable.ic_upload)
                        statusView.visibility = View.VISIBLE
                    }
                    "sent" -> {
                        statusView.setImageResource(R.drawable.ic_check_circle)
                        statusView.visibility = View.VISIBLE
                    }
                    "delivered" -> {
                        statusView.setImageResource(R.drawable.ic_check_circle)
                        statusView.visibility = View.VISIBLE
                    }
                    "read" -> {
                        statusView.setImageResource(R.drawable.ic_check_circle)
                        statusView.visibility = View.VISIBLE
                    }
                    else -> statusView.visibility = View.GONE
                }
            } else {
                statusView.visibility = View.GONE
            }
        }
        
        // Set message layout alignment
        holder.messageLayout?.let { layout ->
            val layoutParams = layout.layoutParams as? LinearLayout.LayoutParams
            layoutParams?.let { params ->
                params.gravity = if (isMyMessage) Gravity.END else Gravity.START
                layout.layoutParams = params
            }
        }
        
        // Set message bubble background and colors
        holder.messageBubble?.let { bubble ->
            // Set background based on message type
            if (isMyMessage) {
                bubble.setBackgroundResource(R.drawable.shape_outgoing_message_single)
            } else {
                bubble.setBackgroundResource(R.drawable.shape_incoming_message_single)
            }
        }
        
        // Set text color based on message type
        val context = holder.itemView.context
        if (isMyMessage) {
            holder.messageText.setTextColor(context.getColor(R.color.md_theme_onPrimaryContainer))
        } else {
            holder.messageText.setTextColor(context.getColor(R.color.md_theme_onSurfaceVariant))
        }
        
        // Handle reply indicator with WhatsApp-style display
        val repliedMessageId = message["replied_message_id"]?.toString()
        if (!repliedMessageId.isNullOrEmpty()) {
            // Find the replied message in the list
            val repliedMessage = messages.find { 
                it["id"]?.toString() == repliedMessageId 
            }
            
            if (repliedMessage != null) {
                holder.repliedMessageLayout?.visibility = View.VISIBLE
                
                // Get sender name from replied message - always show "You" for current user
                val repliedSenderId = repliedMessage["sender_id"]?.toString()
                    ?: repliedMessage["uid"]?.toString()
                val repliedSenderName = if (repliedSenderId == SupabaseClient.client.auth.currentUserOrNull()?.id) {
                    "You"
                } else {
                    // In a real implementation, we'd fetch the username from a cache or database
                    // For now, use a placeholder that can be enhanced later
                    "User"
                }
                
                holder.repliedUsername?.text = repliedSenderName
                
                // Get message text - already truncated to 2 lines by maxLines in XML
                val repliedText = repliedMessage["content"]?.toString()
                    ?: repliedMessage["message_text"]?.toString()
                    ?: ""
                holder.repliedMessage?.text = repliedText
                
                // Set click listener to scroll to original message
                holder.repliedMessageLayout?.setOnClickListener {
                    onReplyClick?.invoke(repliedMessageId)
                }
            } else {
                // Message not found - might be deleted or not loaded
                holder.repliedMessageLayout?.visibility = View.GONE
            }
        } else {
            holder.repliedMessageLayout?.visibility = View.GONE
        }
        
        // Set click listeners - support both id field names
        val messageId = message["id"]?.toString() 
            ?: message["key"]?.toString() 
            ?: ""
        
        holder.itemView.setOnClickListener {
            onMessageClick?.invoke(messageId, position)
        }
        
        holder.itemView.setOnLongClickListener {
            onMessageLongClick?.invoke(messageId, position) ?: false
        }
    }

    override fun getItemCount(): Int = messages.size
    
    /**
     * Format message timestamp for display
     */
    private fun formatMessageTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        
        val now = Calendar.getInstance()
        
        return if (isSameDay(calendar, now)) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else if (isYesterday(calendar, now)) {
            "Yesterday ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        } else {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun isYesterday(messageTime: Calendar, now: Calendar): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.timeInMillis = now.timeInMillis
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        
        return isSameDay(messageTime, yesterday)
    }
    
    /**
     * Update messages list and refresh UI
     */
    fun updateMessages(newMessages: List<HashMap<String, Any?>>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
    
    /**
     * Add a new message to the list
     */
    fun addMessage(message: HashMap<String, Any?>) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
    
    /**
     * Remove a message from the list
     */
    fun removeMessage(position: Int) {
        if (position >= 0 && position < messages.size) {
            messages.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
