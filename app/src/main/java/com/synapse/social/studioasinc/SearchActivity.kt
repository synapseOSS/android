package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.ui.search.SearchScreen
import com.synapse.social.studioasinc.ui.search.SearchViewModel
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.chat.ChatActivity

class SearchActivity : BaseActivity() {

    private var chatMode = false
    private var origin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        com.synapse.social.studioasinc.core.util.EdgeToEdgeUtils.setupEdgeToEdgeActivity(this)

        // Handle intents
        chatMode = intent.getBooleanExtra("mode", false) || intent.getStringExtra("mode") == "chat"
        origin = intent.getStringExtra("origin") ?: ""

        setContent {
            SynapseTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colorScheme.background) {
                    val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.provideFactory(this))

                    SearchScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = { uid ->
                            if (chatMode) {
                                val intent = Intent(this, ChatActivity::class.java)
                                intent.putExtra("uid", uid)
                                intent.putExtra("ORIGIN_KEY", "SearchActivity")
                                startActivity(intent)
                                finish()
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
