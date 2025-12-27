package com.synapse.social.studioasinc.compose.components

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.ui.theme.SynapseTheme

class ComposeLoadingViewHolder(composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
    init {
        composeView.setContent {
            SynapseTheme {
                LoadingIndicator()
            }
        }
    }
    
    companion object {
        fun create(parent: ViewGroup): ComposeLoadingViewHolder {
            val composeView = ComposeView(parent.context)
            return ComposeLoadingViewHolder(composeView)
        }
    }
}

class ComposeEndOfListViewHolder(composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
    init {
        composeView.setContent {
            SynapseTheme {
                EndOfListIndicator()
            }
        }
    }
    
    companion object {
        fun create(parent: ViewGroup): ComposeEndOfListViewHolder {
            val composeView = ComposeView(parent.context)
            return ComposeEndOfListViewHolder(composeView)
        }
    }
}
