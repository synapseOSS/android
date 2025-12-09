package com.synapse.social.studioasinc.chat

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.chat.presentation.MessageActionsViewModel
import com.synapse.social.studioasinc.databinding.DialogForwardMessageBinding
import com.synapse.social.studioasinc.databinding.ItemForwardConversationBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * Dialog for selecting conversations to forward a message to
 * Supports multi-select and search/filter functionality
 */
class ForwardMessageDialog : DialogFragment() {

    companion object {
        private const val TAG = "ForwardMessageDialog"
        private const val ARG_MESSAGE_ID = "message_id"
        private const val ARG_MESSAGE_DATA = "message_data"

        /**
         * Create a new instance of ForwardMessageDialog
         * 
         * @param messageId The ID of the message to forward
         * @param messageData The complete message data as a map
         * @return New instance of ForwardMessageDialog
         */
        fun newInstance(messageId: String, messageData: Map<String, Any?>): ForwardMessageDialog {
            val dialog = ForwardMessageDialog()
            val args = Bundle().apply {
                putString(ARG_MESSAGE_ID, messageId)
                putSerializable(ARG_MESSAGE_DATA, HashMap(messageData))
            }
            dialog.arguments = args
            return dialog
        }
    }

    private var _binding: DialogForwardMessageBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MessageActionsViewModel
    private lateinit var chatService: SupabaseChatService

    private val conversationsAdapter = ForwardConversationsAdapter()
    private var allConversations = listOf<ForwardableConversation>()
    private var filteredConversations = listOf<ForwardableConversation>()

    private var messageId: String = ""
    private var messageData: Map<String, Any?> = emptyMap()

    /**
     * Data class representing a conversation that can be forwarded to
     */
    data class ForwardableConversation(
        val chatId: String,
        val displayName: String,
        val avatarUrl: String?,
        val isGroup: Boolean,
        val participantCount: Int,
        var isSelected: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog)

        // Get arguments
        arguments?.let {
            messageId = it.getString(ARG_MESSAGE_ID, "")
            @Suppress("UNCHECKED_CAST")
            messageData = it.getSerializable(ARG_MESSAGE_DATA) as? Map<String, Any?> ?: emptyMap()
        }

        // Initialize services and ViewModel
        chatService = SupabaseChatService()
        viewModel = MessageActionsViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogForwardMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchField()
        setupButtons()
        loadConversations()
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full width
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Set up the RecyclerView with conversations adapter
     */
    private fun setupRecyclerView() {
        binding.rvConversationsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conversationsAdapter
        }

        // Handle conversation selection
        conversationsAdapter.onConversationClick = { conversation ->
            conversation.isSelected = !conversation.isSelected
            conversationsAdapter.notifyDataSetChanged()
            updateSelectedCount()
            updateForwardButtonState()
        }
    }

    /**
     * Set up search field with text watcher
     */
    private fun setupSearchField() {
        binding.etSearchConversations.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterConversations(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Set up button click listeners
     */
    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnForward.setOnClickListener {
            forwardToSelectedConversations()
        }
    }

    /**
     * Load user's conversations from Supabase
     */
    private fun loadConversations() {
        lifecycleScope.launch {
            try {
                // Get current user ID
                val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: run {
                    Log.e(TAG, "No current user ID")
                    showError("Unable to load conversations")
                    return@launch
                }

                // Get user's chats
                val result = chatService.getUserChats(currentUserId)

                result.fold(
                    onSuccess = { chats ->
                        Log.d(TAG, "Loaded ${chats.size} conversations")
                        
                        // Convert to ForwardableConversation objects
                        allConversations = chats.map { chat ->
                            val chatId = chat["chat_id"] as? String ?: ""
                            val isGroup = (chat["is_group"] as? String)?.toBoolean() ?: false
                            val participantCount = (chat["participants_count"] as? String)?.toIntOrNull() ?: 2
                            
                            // Get display name based on chat type
                            val displayName = if (isGroup) {
                                chat["group_name"] as? String ?: "Group Chat"
                            } else {
                                // For direct chats, get the other user's name
                                getOtherUserName(chatId, currentUserId)
                            }

                            ForwardableConversation(
                                chatId = chatId,
                                displayName = displayName,
                                avatarUrl = chat["avatar_url"] as? String,
                                isGroup = isGroup,
                                participantCount = participantCount
                            )
                        }

                        filteredConversations = allConversations
                        conversationsAdapter.submitList(filteredConversations)
                        updateEmptyState()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to load conversations", error)
                        showError("Failed to load conversations")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversations", e)
                showError("An error occurred while loading conversations")
            }
        }
    }

    /**
     * Get the other user's name in a direct chat
     */
    private suspend fun getOtherUserName(chatId: String, currentUserId: String): String {
        return try {
            val participants = chatService.getChatParticipants(chatId).getOrNull() ?: emptyList()
            val otherUserId = participants.firstOrNull { it != currentUserId }
            
            if (otherUserId != null) {
                // Query users table to get username
                val userResult = SupabaseClient.client.from("users")
                    .select(columns = Columns.raw("username")) {
                        filter { eq("uid", otherUserId) }
                        limit(1)
                    }
                    .decodeList<JsonObject>()
                    .firstOrNull()
                
                userResult?.get("username")?.toString()?.removeSurrounding("\"") ?: "Unknown User"
            } else {
                "Unknown User"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting other user name", e)
            "Unknown User"
        }
    }

    /**
     * Filter conversations by display name
     */
    private fun filterConversations(query: String) {
        filteredConversations = if (query.isBlank()) {
            allConversations
        } else {
            allConversations.filter { conversation ->
                conversation.displayName.contains(query, ignoreCase = true)
            }
        }

        conversationsAdapter.submitList(filteredConversations)
        updateEmptyState()
    }

    /**
     * Update empty state visibility
     */
    private fun updateEmptyState() {
        if (filteredConversations.isEmpty()) {
            binding.rvConversationsList.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvConversationsList.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    /**
     * Update selected count display
     */
    private fun updateSelectedCount() {
        val selectedCount = allConversations.count { it.isSelected }
        if (selectedCount > 0) {
            binding.tvSelectedCount.visibility = View.VISIBLE
            binding.tvSelectedCount.text = "$selectedCount conversation${if (selectedCount > 1) "s" else ""} selected"
        } else {
            binding.tvSelectedCount.visibility = View.GONE
        }
    }

    /**
     * Update forward button enabled state
     */
    private fun updateForwardButtonState() {
        val hasSelection = allConversations.any { it.isSelected }
        binding.btnForward.isEnabled = hasSelection
    }

    /**
     * Forward message to selected conversations
     */
    private fun forwardToSelectedConversations() {
        val selectedChatIds = allConversations.filter { it.isSelected }.map { it.chatId }

        if (selectedChatIds.isEmpty()) {
            showError("Please select at least one conversation")
            return
        }

        // Show loading state
        binding.btnForward.isEnabled = false
        binding.btnCancel.isEnabled = false

        lifecycleScope.launch {
            viewModel.forwardMessage(messageId, messageData, selectedChatIds)
                .collectLatest { state ->
                    when {
                        state.isForwarding -> {
                            // Show loading indicator (button is already disabled)
                            Log.d(TAG, "Forwarding message...")
                        }
                        state.error != null -> {
                            // Show error
                            showError(state.error)
                            binding.btnForward.isEnabled = true
                            binding.btnCancel.isEnabled = true
                        }
                        state.forwardedCount > 0 -> {
                            // Show success
                            val message = if (state.forwardedCount == selectedChatIds.size) {
                                "Message forwarded to ${state.forwardedCount} conversation${if (state.forwardedCount > 1) "s" else ""}"
                            } else {
                                "Message forwarded to ${state.forwardedCount} of ${selectedChatIds.size} conversations"
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                    }
                }
        }
    }

    /**
     * Show error message
     */
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * RecyclerView adapter for forward conversations
     */
    private inner class ForwardConversationsAdapter :
        RecyclerView.Adapter<ForwardConversationsAdapter.ConversationViewHolder>() {

        private var conversations = listOf<ForwardableConversation>()
        var onConversationClick: ((ForwardableConversation) -> Unit)? = null

        fun submitList(newConversations: List<ForwardableConversation>) {
            conversations = newConversations
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
            val binding = ItemForwardConversationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ConversationViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
            holder.bind(conversations[position])
        }

        override fun getItemCount(): Int = conversations.size

        inner class ConversationViewHolder(
            private val binding: ItemForwardConversationBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(conversation: ForwardableConversation) {
                binding.apply {
                    // Set conversation name
                    tvConversationName.text = conversation.displayName

                    // Set participant count for groups
                    if (conversation.isGroup) {
                        tvParticipantCount.visibility = View.VISIBLE
                        tvParticipantCount.text = "${conversation.participantCount} participants"
                    } else {
                        tvParticipantCount.visibility = View.GONE
                    }

                    // Set checkbox state
                    cbSelect.isChecked = conversation.isSelected

                    // Load avatar
                    if (conversation.avatarUrl != null) {
                        Glide.with(ivAvatar.context)
                            .load(conversation.avatarUrl)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(ivAvatar)
                    } else {
                        ivAvatar.setImageResource(R.drawable.ic_launcher_foreground)
                    }

                    // Handle click
                    root.setOnClickListener {
                        onConversationClick?.invoke(conversation)
                    }
                }
            }
        }
    }
}
