package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.compose.FollowListScreen
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.backend.SupabaseChatService
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import kotlinx.coroutines.launch

class FollowListComposeActivity : ComponentActivity() {

    companion object {
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_LIST_TYPE = "list_type"
        const val TYPE_FOLLOWERS = "followers"
        const val TYPE_FOLLOWING = "following"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userId = intent.getStringExtra(EXTRA_USER_ID)
        val listType = intent.getStringExtra(EXTRA_LIST_TYPE)
        
        if (userId == null || listType == null) {
            finish()
            return
        }

        setContent {
            SynapseTheme {
                FollowListScreen(
                    userId = userId,
                    listType = listType,
                    onNavigateBack = { finish() },
                    onUserClick = { targetUserId ->
                        val intent = Intent(this@FollowListComposeActivity, ProfileComposeActivity::class.java)
                        intent.putExtra("uid", targetUserId)
                        startActivity(intent)
                    },
                    onMessageClick = { targetUserId ->
                        startDirectChat(targetUserId)
                    }
                )
            }
        }
    }

    private fun startDirectChat(targetUserId: String) {
        lifecycleScope.launch {
            try {
                val authRepository = AuthRepository()
                val currentUserId = authRepository.getCurrentUserUid()

                if (currentUserId == null) {
                    return@launch
                }

                if (targetUserId == currentUserId) {
                    return@launch
                }

                val chatService = SupabaseChatService()
                val result = chatService.getOrCreateDirectChat(currentUserId, targetUserId)
                
                result.fold(
                    onSuccess = { chatId ->
                        val intent = Intent(this@FollowListComposeActivity, ChatActivity::class.java)
                        intent.putExtra("chatId", chatId)
                        intent.putExtra("uid", targetUserId)
                        intent.putExtra("isGroup", false)
                        startActivity(intent)
                    },
                    onFailure = { }
                )
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}
