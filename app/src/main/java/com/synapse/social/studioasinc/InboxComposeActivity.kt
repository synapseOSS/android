package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
                        onNavigateToChat = { chatId ->
                            // Navigate to existing ChatActivity
                            // We need to keep this for now as ChatActivity migration is out of scope
                            val intent = Intent(this, ChatActivity::class.java).apply {
                                putExtra("chat_id", chatId)
                            }
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
