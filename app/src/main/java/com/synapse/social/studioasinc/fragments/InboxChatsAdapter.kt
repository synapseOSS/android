package com.synapse.social.studioasinc.fragments

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import java.text.SimpleDateFormat
import java.util.*

class InboxChatsAdapter(
    private val chats: ArrayList<Map<String, Any?>>,
    private val onChatClick: (String, String) -> Unit
) : RecyclerView.Adapter<InboxChatsAdapter.ChatViewHolder>() {
    
    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImage: ImageView = itemView.findViewById(R.id.chatAvatar)
        val nameText: TextView = itemView.findViewById(R.id.chatName)
        val lastMessageText: TextView = itemView.findViewById(R.id.lastMessage)
        val timeText: TextView = itemView.findViewById(R.id.chatTime)
        val unreadBadge: TextView? = itemView.findViewById(R.id.unreadBadge)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list, parent, false)
        return ChatViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        
        // Set chat name
        val chatName = chat["other_user_name"]?.toString() ?: "User"
        holder.nameText.text = chatName
        
        // Set avatar
        val avatarUrl = chat["other_user_avatar"]?.toString()
        if (!avatarUrl.isNullOrEmpty() && avatarUrl != "null") {
            Glide.with(holder.itemView.context)
                .load(Uri.parse(avatarUrl))
                .circleCrop()
                .placeholder(R.drawable.avatar)
                .into(holder.avatarImage)
        } else {
            holder.avatarImage.setImageResource(R.drawable.avatar)
        }
        
        // Set last message
        val lastMessage = chat["last_message"]?.toString() ?: "No messages yet"
        holder.lastMessageText.text = lastMessage
        
        // Set time
        val timestamp = chat["last_message_time"]?.toString()?.toLongOrNull()
        if (timestamp != null) {
            holder.timeText.text = formatTime(timestamp)
        } else {
            holder.timeText.text = ""
        }
        
        // Set unread badge (if available)
        holder.unreadBadge?.visibility = View.GONE
        
        // Set click listener
        holder.itemView.setOnClickListener {
            val chatId = chat["chat_id"]?.toString() ?: return@setOnClickListener
            val otherUserId = chat["other_user_id"]?.toString() ?: return@setOnClickListener
            onChatClick(chatId, otherUserId)
        }
    }
    
    override fun getItemCount(): Int = chats.size
    
    private fun formatTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        
        val now = Calendar.getInstance()
        
        return when {
            isSameDay(calendar, now) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            isYesterday(calendar, now) -> "Yesterday"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
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
}
