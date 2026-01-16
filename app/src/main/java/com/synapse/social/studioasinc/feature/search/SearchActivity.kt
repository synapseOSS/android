package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.ui.search.SearchScreen
import com.synapse.social.studioasinc.ui.search.SearchViewModel
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
// TODO: Re-implement chat functionality
// import com.synapse.social.studioasinc.ui.chat.ChatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : ComponentActivity() {

    private var chatMode = false
    private var origin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        // Handle intents
        chatMode = intent.getBooleanExtra("mode", false) || intent.getStringExtra("mode") == "chat"
        origin = intent.getStringExtra("origin") ?: ""

        setContent {
            SynapseTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colorScheme.background) {
                    val viewModel: SearchViewModel = hiltViewModel()

                    SearchScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = { uid ->
                            if (chatMode) {
                                // TODO: Re-implement chat navigation
                                Toast.makeText(this@SearchActivity, "Chat feature not implemented", Toast.LENGTH_SHORT).show()
                                /*
                                val intent = Intent(this, ChatActivity::class.java)
                                intent.putExtra("uid", uid)
                                intent.putExtra("ORIGIN_KEY", "SearchActivity")
                                startActivity(intent)
                                finish()
                                */
                            } else {
                                val intent = Intent(this, ProfileActivity::class.java)
                                intent.putExtra("uid", uid)
                                intent.putExtra("origin", "SearchActivity")
                                startActivity(intent)
                            }
                        },
                        onNavigateToPost = { postId ->
                            val intent = Intent(this, HomeActivity::class.java)
                            intent.putExtra("post_id", postId)
                            startActivity(intent)
                        },
                        onBack = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}
