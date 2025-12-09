package com.synapse.social.studioasinc.chat

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.BottomSheetMessageActionsBinding
import com.synapse.social.studioasinc.databinding.ItemMessageActionBinding

/**
 * Bottom sheet dialog that displays available actions for a message
 * Implements Material Design bottom sheet with dynamic action filtering
 */
class MessageActionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMessageActionsBinding? = null
    private val binding get() = _binding!!

    private var messageData: Map<String, Any?>? = null
    private var currentUserId: String? = null
    private var listener: MessageActionListener? = null

    /**
     * Data class representing a message action
     */
    data class MessageAction(
        val id: String,
        val label: String,
        val icon: Int,
        val isDestructive: Boolean = false
    )

    /**
     * Listener interface for message action callbacks
     */
    interface MessageActionListener {
        fun onReplyAction(messageId: String, messageText: String, senderName: String)
        fun onForwardAction(messageId: String, messageData: Map<String, Any?>)
        fun onEditAction(messageId: String, currentText: String)
        fun onDeleteAction(messageId: String)
        fun onAISummaryAction(messageId: String, messageText: String)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMessageActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Trigger haptic feedback
        triggerHapticFeedback()

        // Apply slide-up animation to bottom sheet content
        applyBottomSheetAnimations()

        // Set up message preview
        setupMessagePreview()

        // Set up actions list
        setupActionsList()
    }

    /**
     * Apply slide-up animation to bottom sheet appearance
     */
    private fun applyBottomSheetAnimations() {
        val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.root.startAnimation(slideUpAnimation)
        
        // Apply fade-in animation to background dim (handled by BottomSheetDialog)
        dialog?.window?.let { window ->
            window.setWindowAnimations(R.style.BottomSheetAnimation)
        }
    }

    /**
     * Trigger haptic feedback on bottom sheet appearance
     */
    private fun triggerHapticFeedback() {
        try {
            val vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)
            vibrator?.let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Haptic feedback is optional, ignore errors
        }
    }

    /**
     * Set up message preview text
     */
    private fun setupMessagePreview() {
        // Support both content (Supabase) and message_text (legacy) field names
        val messageText = messageData?.get("content") as? String 
            ?: messageData?.get("message_text") as? String 
            ?: ""
        binding.messagePreview.text = messageText
    }

    /**
     * Set up RecyclerView with available actions
     */
    private fun setupActionsList() {
        val actions = getAvailableActions(messageData, currentUserId)
        
        binding.actionsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = MessageActionsAdapter(actions) { action ->
                onActionSelected(action)
            }
        }
    }

    /**
     * Get available actions based on message data and current user
     * Filters actions based on message ownership, type, and state
     */
    private fun getAvailableActions(
        messageData: Map<String, Any?>?,
        currentUserId: String?
    ): List<MessageAction> {
        if (messageData == null || currentUserId == null) {
            return emptyList()
        }

        val actions = mutableListOf<MessageAction>()
        
        // Support both sender_id (Supabase) and uid (legacy) field names
        val senderId = messageData["sender_id"] as? String 
            ?: messageData["uid"] as? String
        // Support both content (Supabase) and message_text (legacy) field names
        val messageText = messageData["content"] as? String 
            ?: messageData["message_text"] as? String 
            ?: ""
        val messageType = messageData["message_type"] as? String 
            ?: messageData["TYPE"] as? String
        val isDeleted = messageData["is_deleted"] as? Boolean ?: false
        val isSystemMessage = messageType == "system"
        val isOwnMessage = senderId == currentUserId
        val hasText = messageText.isNotEmpty()
        val isMediaOnly = !hasText && messageType in listOf("image", "video", "audio")

        // Filter out all actions for system messages or deleted messages
        if (isSystemMessage || isDeleted) {
            return emptyList()
        }

        // Reply - available for all non-system, non-deleted messages
        actions.add(
            MessageAction(
                id = "reply",
                label = getString(R.string.action_reply),
                icon = R.drawable.ic_reply
            )
        )

        // Forward - available for all non-system, non-deleted messages
        actions.add(
            MessageAction(
                id = "forward",
                label = getString(R.string.action_forward),
                icon = R.drawable.ic_forward
            )
        )

        // Edit - only for own messages with text content
        if (isOwnMessage && !isMediaOnly && hasText) {
            actions.add(
                MessageAction(
                    id = "edit",
                    label = getString(R.string.action_edit),
                    icon = R.drawable.ic_edit
                )
            )
        }

        // Delete - only for own messages
        if (isOwnMessage) {
            actions.add(
                MessageAction(
                    id = "delete",
                    label = getString(R.string.action_delete),
                    icon = R.drawable.ic_delete_48px,
                    isDestructive = true
                )
            )
        }

        // AI Summary / See Original - only for messages with >100 characters
        if (messageText.length > 100) {
            // Check if AI summary is currently being shown
            val showingSummary = messageData["showing_ai_summary"] as? Boolean ?: false
            val hasSummary = messageData["ai_summary"] as? String != null
            
            if (showingSummary && hasSummary) {
                // Show "See Original" option
                actions.add(
                    MessageAction(
                        id = "see_original",
                        label = "See Original",
                        icon = R.drawable.ic_text_fields_48px
                    )
                )
            } else {
                // Show "AI Summary" option
                actions.add(
                    MessageAction(
                        id = "ai_summary",
                        label = getString(R.string.action_ai_summary),
                        icon = R.drawable.ic_ai_summary
                    )
                )
            }
        }

        return actions
    }

    /**
     * Handle action selection
     */
    private fun onActionSelected(action: MessageAction) {
        val messageId = messageData?.get("id") as? String 
            ?: messageData?.get("key") as? String 
            ?: return
        // Support both content (Supabase) and message_text (legacy) field names
        val messageText = messageData?.get("content") as? String 
            ?: messageData?.get("message_text") as? String 
            ?: ""
        val senderName = messageData?.get("sender_name") as? String ?: "User"

        when (action.id) {
            "reply" -> listener?.onReplyAction(messageId, messageText, senderName)
            "forward" -> listener?.onForwardAction(messageId, messageData!!)
            "edit" -> listener?.onEditAction(messageId, messageText)
            "delete" -> listener?.onDeleteAction(messageId)
            "ai_summary" -> listener?.onAISummaryAction(messageId, messageText)
            "see_original" -> listener?.onAISummaryAction(messageId, messageText) // Toggle back to original
        }

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "MessageActionsBottomSheet"

        /**
         * Show the bottom sheet with message data and listener
         */
        fun show(
            fragmentManager: FragmentManager,
            messageData: Map<String, Any?>,
            currentUserId: String,
            listener: MessageActionListener
        ) {
            val bottomSheet = MessageActionsBottomSheet().apply {
                this.messageData = messageData
                this.currentUserId = currentUserId
                this.listener = listener
            }
            bottomSheet.show(fragmentManager, TAG)
        }
    }

    /**
     * RecyclerView adapter for message actions
     */
    private class MessageActionsAdapter(
        private val actions: List<MessageAction>,
        private val onActionClick: (MessageAction) -> Unit
    ) : RecyclerView.Adapter<MessageActionsAdapter.ActionViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
            val binding = ItemMessageActionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ActionViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
            holder.bind(actions[position], onActionClick)
        }

        override fun getItemCount(): Int = actions.size

        class ActionViewHolder(
            private val binding: ItemMessageActionBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(action: MessageAction, onActionClick: (MessageAction) -> Unit) {
                binding.actionIcon.setImageResource(action.icon)
                binding.actionLabel.text = action.label

                // Apply destructive styling for delete action
                if (action.isDestructive) {
                    binding.actionLabel.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.md_theme_error)
                    )
                    binding.actionIcon.setColorFilter(
                        ContextCompat.getColor(binding.root.context, R.color.md_theme_error)
                    )
                } else {
                    binding.actionLabel.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.md_theme_onSurface)
                    )
                    binding.actionIcon.clearColorFilter()
                }

                binding.root.setOnClickListener {
                    // Apply ripple effect on action selection
                    com.synapse.social.studioasinc.util.MessageAnimations.applyRippleEffect(binding.root)
                    onActionClick(action)
                }
            }
        }
    }
}
