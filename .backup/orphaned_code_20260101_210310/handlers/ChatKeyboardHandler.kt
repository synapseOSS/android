package com.synapse.social.studioasinc

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView

class ChatKeyboardHandler(
    private val rootView: View,
    private val messageInput: EditText,
    private val recyclerView: RecyclerView
) {
    
    companion object {
        private const val TAG = "ChatKeyboardHandler"
    }
    
    private var onKeyboardOpenListener: ((height: Int) -> Unit)? = null
    private var onKeyboardCloseListener: (() -> Unit)? = null
    
    fun setOnKeyboardOpenListener(listener: (height: Int) -> Unit) {
        onKeyboardOpenListener = listener
    }
    
    fun setOnKeyboardCloseListener(listener: () -> Unit) {
        onKeyboardCloseListener = listener
    }
    
    fun handleInputFocus() {
        messageInput.requestFocus()
        val imm = messageInput.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT)
    }

    fun handleMessageSent() {
        // Scroll to show the new message
        recyclerView.scrollToPosition(0)
    }
    
    fun handleAttachmentPress() {
        hideKeyboard()
    }
    
    fun handleVoiceRecording(isRecording: Boolean) {
        if (isRecording) {
            hideKeyboard()
        }
    }
    
    fun handleBackPress(): Boolean {
        // Check if keyboard is open and hide it
        val imm = rootView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return if (imm.isAcceptingText) {
            hideKeyboard()
            true
        } else {
            false
        }
    }

    fun handleConfigurationChange() {
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = rootView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(messageInput.windowToken, 0)
    }
    
    fun cleanup() {
        onKeyboardOpenListener = null
        onKeyboardCloseListener = null
    }
}
