package com.synapse.social.studioasinc.chat

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.DialogEditMessageBinding

/**
 * Dialog for editing a message
 */
class EditMessageDialog : DialogFragment() {

    private var _binding: DialogEditMessageBinding? = null
    private val binding get() = _binding!!

    private var messageId: String? = null
    private var currentText: String? = null
    private var listener: EditMessageListener? = null

    interface EditMessageListener {
        fun onMessageEdited(messageId: String, newText: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditMessageBinding.inflate(layoutInflater)

        messageId = arguments?.getString(ARG_MESSAGE_ID)
        currentText = arguments?.getString(ARG_CURRENT_TEXT)

        setupViews()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full width
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Show keyboard
        binding.etMessageContent.requestFocus()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    private fun setupViews() {
        // Set current text
        binding.etMessageContent.setText(currentText)
        binding.etMessageContent.setSelection(currentText?.length ?: 0)
        
        // Update character count
        updateCharacterCount(currentText?.length ?: 0)

        // Add text watcher for character count and save button state
        binding.etMessageContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCharacterCount(s?.length ?: 0)
                
                // Enable save button only if text changed and not empty
                val newText = s?.toString()?.trim() ?: ""
                val hasChanged = newText != currentText?.trim()
                val isNotEmpty = newText.isNotEmpty()
                binding.btnSave.isEnabled = hasChanged && isNotEmpty
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Cancel button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            val newText = binding.etMessageContent.text?.toString()?.trim() ?: ""
            if (newText.isNotEmpty() && messageId != null) {
                listener?.onMessageEdited(messageId!!, newText)
                dismiss()
            }
        }
    }

    private fun updateCharacterCount(count: Int) {
        val maxLength = 5000
        binding.tvCharacterCount.text = "$count / $maxLength"
        
        // Change color if approaching limit
        if (count > maxLength * 0.9) {
            binding.tvCharacterCount.setTextColor(
                resources.getColor(R.color.md_theme_error, null)
            )
        } else {
            binding.tvCharacterCount.setTextColor(
                resources.getColor(android.R.color.darker_gray, null)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MESSAGE_ID = "message_id"
        private const val ARG_CURRENT_TEXT = "current_text"

        fun newInstance(
            messageId: String,
            currentText: String,
            listener: EditMessageListener
        ): EditMessageDialog {
            return EditMessageDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE_ID, messageId)
                    putString(ARG_CURRENT_TEXT, currentText)
                }
                this.listener = listener
            }
        }
    }
}
