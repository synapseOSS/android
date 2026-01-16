package com.synapse.social.studioasinc.feature.stories.viewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
// TODO: Re-enable when chat functionality is implemented
// import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoryViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userId = intent.getStringExtra("user_id")
        if (userId == null) {
            finish()
            return
        }
        
        setContent {
            SynapseTheme {
                // TODO: Re-enable when chat functionality is implemented
                // val viewModel: StoryViewerViewModel = hiltViewModel()
                
                // TODO: Re-enable when chat functionality is implemented
                // LaunchedEffect(userId) {
                //     viewModel.loadStories(userId)
                // }
                
                StoryViewerScreen(
                    onClose = { finish() }
                    // TODO: Re-enable when chat functionality is implemented
                    // viewModel = viewModel
                )
            }
        }
    }
}
