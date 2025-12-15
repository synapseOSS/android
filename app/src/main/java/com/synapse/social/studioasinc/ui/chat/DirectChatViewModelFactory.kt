package com.synapse.social.studioasinc.ui.chat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.ui.deletion.MessageDeletionViewModel

/**
 * Factory for creating DirectChatViewModel instances with dependencies.
 * Requirements: 2.4
 */
class DirectChatViewModelFactory(
    private val application: Application,
    private val messageDeletionViewModel: MessageDeletionViewModel
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DirectChatViewModel::class.java)) {
            return DirectChatViewModel(
                application = application,
                messageDeletionViewModel = messageDeletionViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}