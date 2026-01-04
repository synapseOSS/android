package com.synapse.social.studioasinc.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.data.remote.services.SupabaseChatService
import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import com.synapse.social.studioasinc.ui.deletion.MessageDeletionViewModel

/**
 * Factory for creating InboxViewModel instances with dependencies.
 * Requirements: 2.4
 */
class InboxViewModelFactory(
    private val chatService: SupabaseChatService = SupabaseChatService(),
    private val authService: SupabaseAuthenticationService = SupabaseAuthenticationService(),
    private val databaseService: SupabaseDatabaseService = SupabaseDatabaseService(),
    private val messageDeletionViewModel: MessageDeletionViewModel
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InboxViewModel::class.java)) {
            return InboxViewModel(
                chatService = chatService,
                authService = authService,
                databaseService = databaseService,
                messageDeletionViewModel = messageDeletionViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
