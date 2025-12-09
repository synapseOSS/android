package com.synapse.social.studioasinc.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.ChatActivity
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.launch

class InboxChatsFragment : Fragment() {
    
    private val authService = SupabaseAuthenticationService()
    private val chatService = SupabaseChatService()
    private val databaseService = SupabaseDatabaseService()
    
    private var recyclerView: RecyclerView? = null
    private var emptyView: TextView? = null
    private var progressBar: ProgressBar? = null
    private val chatsList = ArrayList<Map<String, Any?>>()
    private var chatsAdapter: InboxChatsAdapter? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inbox_chats, container, false)
        
        recyclerView = view.findViewById(R.id.chatsRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        progressBar = view.findViewById(R.id.progressBar)
        
        setupRecyclerView()
        loadChats()
        
        return view
    }
    
    private fun setupRecyclerView() {
        recyclerView?.layoutManager = LinearLayoutManager(context)
        chatsAdapter = InboxChatsAdapter(chatsList) { chatId, otherUserId ->
            openChat(chatId, otherUserId)
        }
        recyclerView?.adapter = chatsAdapter
    }
    
    private fun loadChats() {
        val currentUser = authService.getCurrentUser()
        if (currentUser == null) {
            showEmpty("Please log in to view chats")
            return
        }
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = chatService.getUserChats(currentUser.id)
                result.fold(
                    onSuccess = { chats ->
                        chatsList.clear()
                        chatsList.addAll(chats)
                        
                        // Load user data for each chat
                        chats.forEach { chat ->
                            loadChatUserData(chat)
                        }
                        
                        chatsAdapter?.notifyDataSetChanged()
                        showLoading(false)
                        
                        if (chats.isEmpty()) {
                            showEmpty("No chats yet\nStart a conversation from Search")
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("InboxChatsFragment", "Failed to load chats", error)
                        showLoading(false)
                        showEmpty("Failed to load chats: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                showEmpty("Error: ${e.message}")
            }
        }
    }
    
    private suspend fun loadChatUserData(chat: Map<String, Any?>) {
        val chatId = chat["chat_id"]?.toString() ?: return
        val currentUserId = authService.getCurrentUser()?.id ?: return
        
        // Extract other user ID from chat_id (format: dm_userId1_userId2)
        val parts = chatId.split("_")
        if (parts.size == 3 && parts[0] == "dm") {
            val otherUserId = if (parts[1] == currentUserId) parts[2] else parts[1]
            
            // Load user data
            val userResult = databaseService.selectWhere("users", "*", "uid", otherUserId)
            userResult.fold(
                onSuccess = { users ->
                    val userData = users.firstOrNull()
                    if (userData != null) {
                        // Update chat with user data
                        val index = chatsList.indexOfFirst { it["chat_id"] == chatId }
                        if (index >= 0) {
                            val updatedChat = chatsList[index].toMutableMap()
                            updatedChat["other_user_name"] = userData["username"]
                            updatedChat["other_user_avatar"] = userData["avatar"]
                            updatedChat["other_user_id"] = otherUserId
                            chatsList[index] = updatedChat
                            chatsAdapter?.notifyItemChanged(index)
                        }
                    }
                },
                onFailure = { }
            )
        }
    }
    
    private fun openChat(chatId: String, otherUserId: String) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("uid", otherUserId)
        startActivity(intent)
    }
    
    private fun showLoading(show: Boolean) {
        progressBar?.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun showEmpty(message: String) {
        emptyView?.text = message
        emptyView?.visibility = View.VISIBLE
        recyclerView?.visibility = View.GONE
    }
    
    override fun onResume() {
        super.onResume()
        // Reload chats when returning to this fragment
        loadChats()
    }
}
