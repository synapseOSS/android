package com.synapse.social.studioasinc.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.Chat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying chat list
 */
class ChatListAdapter(
    private val context: Context,
    private val onChatClick: (Chat) -> Unit
) : ListAdapter<Chat, ChatListAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatName: TextView = itemView.findViewById(android.R.id.text1)
        private val lastMessage: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(chat: Chat) {
            chatName.text = chat.getDisplayName()
            val lastMessageText = chat.lastMessage ?: "No messages yet"
            val timeText = chat.lastMessageTime?.let { formatTime(it) } ?: ""
            lastMessage.text = "$lastMessageText • $timeText"
            
            itemView.setOnClickListener {
                onChatClick(chat)
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 24 * 60 * 60 * 1000 -> { // Less than 24 hours
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
            diff < 7 * 24 * 60 * 60 * 1000 -> { // Less than 7 days
                val sdf = SimpleDateFormat("EEE", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
            else -> { // More than 7 days
                val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem == newItem
        }
    }
}
