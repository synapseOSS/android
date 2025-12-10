package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.ui.inbox.InboxScreen
import com.synapse.social.studioasinc.ui.theme.SynapseTheme

/**
 * Activity for the new Compose-based Inbox.
 * Replaces the old InboxActivity.
 */
class InboxComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SynapseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InboxScreen(
                        onNavigateBack = { finish() },
                        onNavigateToChat = { chatId, userId ->
                            // Navigate to existing ChatActivity
                            // ChatActivity expects "chatId" and "uid" as intent extras
                            Log.d("InboxComposeActivity", "Navigating to chat - chatId: $chatId, userId: $userId")
                            
                            val intent = Intent(this, ChatActivity::class.java).apply {
                                // Use correct intent extra keys that ChatActivity expects
                                putExtra("chatId", chatId)  // ChatActivity reads from "chatId" (line 132)
                                putExtra("uid", userId)      // ChatActivity reads from "uid" (line 133)
                            }
                            
                            Log.d("InboxComposeActivity", "Starting ChatActivity with extras - chatId: $chatId, uid: $userId")
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, InboxComposeActivity::class.java)
            context.startActivity(intent)
        }
    }
}
