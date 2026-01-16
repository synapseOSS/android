package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.synapse.social.studioasinc.ui.postdetail.PostDetailScreen
import com.synapse.social.studioasinc.ui.theme.SynapseTheme

class PostDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "post_id"
        const val EXTRA_AUTHOR_UID = "author_uid"

        fun start(context: Context, postId: String, authorUid: String? = null) {
            context.startActivity(Intent(context, PostDetailActivity::class.java).apply {
                putExtra(EXTRA_POST_ID, postId)
                putExtra(EXTRA_AUTHOR_UID, authorUid)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val postId = intent.getStringExtra(EXTRA_POST_ID)
        
        if (postId == null) {
            finish()
            return
        }

        setContent {
            SynapseTheme {
                PostDetailScreen(
                    postId = postId,
                    onNavigateBack = { finish() },
                    onNavigateToProfile = { userId -> navigateToProfile(userId) }
                )
            }
        }
    }

    private fun navigateToProfile(userId: String) {
        startActivity(Intent(this, ProfileActivity::class.java).apply {
            putExtra("user_id", userId)
        })
    }
}
