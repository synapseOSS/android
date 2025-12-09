package com.synapse.social.studioasinc.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.chat.presentation.MessageActionsViewModel
import com.synapse.social.studioasinc.databinding.DialogDeleteMessageBinding
import com.synapse.social.studioasinc.util.MessageAnimations
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dialog for confirming message deletion
 * Provides options for "Delete for me" and "Delete for everyone"
 */
class DeleteConfirmationDialog : DialogFragment() {

    companion object {
        private const val TAG = "DeleteConfirmationDialog"
        private const val ARG_MESSAGE_IDS = "message_ids"
        private const val ARG_CAN_DELETE_FOR_EVERYONE = "can_delete_for_everyone"

        /**
         * Create a new instance of DeleteConfirmationDialog
         * 
         * @param messageIds List of message IDs to delete
         * @param canDeleteForEveryone Whether the user owns all messages (enables "Delete for everyone" option)
         * @return New instance of DeleteConfirmationDialog
         */
        fun newInstance(messageIds: List<String>, canDeleteForEveryone: Boolean): DeleteConfirmationDialog {
            val dialog = DeleteConfirmationDialog()
            val args = Bundle().apply {
                putStringArrayList(ARG_MESSAGE_IDS, ArrayList(messageIds))
                putBoolean(ARG_CAN_DELETE_FOR_EVERYONE, canDeleteForEveryone)
            }
            dialog.arguments = args
            return dialog
        }
    }

    private var _binding: DialogDeleteMessageBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MessageActionsViewModel

    private var messageIds: List<String> = emptyList()
    private var canDeleteForEveryone: Boolean = false
    
    // Callback for external deletion handling (used by MessageDeletionCoordinator)
    private var deletionCallback: ((messageIds: List<String>, deleteForEveryone: Boolean) -> Unit)? = null
    
    /**
     * Set callback for deletion execution
     * When set, this callback will be used instead of the internal ViewModel
     * 
     * @param callback Function to execute deletion (messageIds, deleteForEveryone)
     */
    fun setDeletionCallback(callback: (messageIds: List<String>, deleteForEveryone: Boolean) -> Unit) {
        this.deletionCallback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog)

        // Get arguments
        arguments?.let {
            messageIds = it.getStringArrayList(ARG_MESSAGE_IDS)?.toList() ?: emptyList()
            canDeleteForEveryone = it.getBoolean(ARG_CAN_DELETE_FOR_EVERYONE, false)
        }

        // Initialize ViewModel
        viewModel = MessageActionsViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDeleteMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDialogTitle()
        setupRadioButtons()
        setupButtons()
    }

    /**
     * Set up dialog title based on message count
     * Shows "Delete message?" for single message or "Delete X messages?" for multiple
     * 
     * Requirements: 5.1, 5.2
     */
    private fun setupDialogTitle() {
        val messageCount = messageIds.size
        binding.confirmationMessage.text = if (messageCount == 1) {
            getString(R.string.delete_message_confirmation)
        } else {
            getString(R.string.delete_messages_confirmation, messageCount)
        }
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
     * Set up radio buttons with default selection and conditional option display
     * Shows both options when user owns all messages, only "Delete for me" otherwise
     * 
     * Requirements: 5.3, 5.4, 5.5
     */
    private fun setupRadioButtons() {
        // Set default selection to "Delete for me"
        binding.radioDeleteForMe.isChecked = true

        // Show both options when user owns all messages
        if (canDeleteForEveryone) {
            binding.radioDeleteForEveryone.visibility = View.VISIBLE
            binding.deleteForEveryoneExplanation.visibility = View.VISIBLE
            binding.radioDeleteForEveryone.isEnabled = true
            binding.deleteForEveryoneExplanation.isEnabled = true
            binding.radioDeleteForEveryone.alpha = 1.0f
            binding.deleteForEveryoneExplanation.alpha = 1.0f
        } else {
            // Hide "Delete for everyone" option when any message is not owned by user
            binding.radioDeleteForEveryone.visibility = View.GONE
            binding.deleteForEveryoneExplanation.visibility = View.GONE
        }
    }

    /**
     * Set up button click listeners
     */
    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            deleteMessage()
        }
    }

    /**
     * Delete the message(s) based on selected option
     * Wires up button clicks to callback interface
     * 
     * Requirements: 5.5
     */
    private fun deleteMessage() {
        // Get selected option from RadioGroup
        val deleteForEveryone = when (binding.deleteOptionsGroup.checkedRadioButtonId) {
            R.id.radio_delete_for_everyone -> true
            R.id.radio_delete_for_me -> false
            else -> false // Default to "Delete for me"
        }

        // Validate that user can delete for everyone
        if (deleteForEveryone && !canDeleteForEveryone) {
            Toast.makeText(
                requireContext(),
                "You can only delete your own messages for everyone",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // If callback is set, use it instead of internal ViewModel
        if (deletionCallback != null) {
            deletionCallback?.invoke(messageIds, deleteForEveryone)
            dismiss()
            return
        }

        // Fallback to internal ViewModel for single message (backward compatibility)
        if (messageIds.size == 1) {
            // Disable buttons during deletion
            binding.btnDelete.isEnabled = false
            binding.btnCancel.isEnabled = false

            // Show loading indicator
            binding.btnDelete.text = getString(R.string.text_loading)
            Log.d(TAG, "Deleting message: ${messageIds[0]} (deleteForEveryone: $deleteForEveryone)")

            lifecycleScope.launch {
                viewModel.deleteMessage(messageIds[0], deleteForEveryone)
                    .collectLatest { state ->
                        when (state) {
                            is MessageActionsViewModel.MessageActionState.Loading -> {
                                // Loading state - show pulse animation on delete button
                                Log.d(TAG, "Deleting message...")
                                MessageAnimations.applyPendingActionAnimation(binding.btnDelete)
                            }
                            is MessageActionsViewModel.MessageActionState.Success -> {
                                // Clear animations
                                MessageAnimations.clearAnimations(binding.btnDelete)
                                
                                // Success - show message and dismiss
                                Toast.makeText(
                                    requireContext(),
                                    state.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            }
                            is MessageActionsViewModel.MessageActionState.Error -> {
                                // Clear animations
                                MessageAnimations.clearAnimations(binding.btnDelete)
                                
                                // Error - show error and re-enable buttons
                                Toast.makeText(
                                    requireContext(),
                                    state.error,
                                    Toast.LENGTH_SHORT
                                ).show()
                                binding.btnDelete.isEnabled = true
                                binding.btnCancel.isEnabled = true
                                binding.btnDelete.text = getString(R.string.delete)
                            }
                            is MessageActionsViewModel.MessageActionState.Idle -> {
                                // Idle state - do nothing
                            }
                        }
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
