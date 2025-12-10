package com.synapse.social.studioasinc

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.synapse.social.studioasinc.ui.createpost.CreatePostScreen
import com.synapse.social.studioasinc.ui.createpost.CreatePostViewModel
import com.synapse.social.studioasinc.ui.theme.SynapseTheme

class CreatePostActivity : BaseActivity() {

    private val viewModel: CreatePostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle edit mode if necessary
        // Handle edit mode if necessary
        intent.getStringExtra("edit_post_id")?.let { 
             viewModel.loadPostForEdit(it)
        }

        setContent {
            SynapseTheme {
                CreatePostScreen(
                    viewModel = viewModel,
                    onNavigateUp = { finish() }
                )
            }
        }
    }
}
