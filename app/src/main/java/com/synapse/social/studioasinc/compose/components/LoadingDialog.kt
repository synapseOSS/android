package com.synapse.social.studioasinc.compose.components

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synapse.social.studioasinc.ui.theme.SynapseTheme

@Composable
fun LoadingDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                LoadingSynapse()
            }
        }
    }
}

fun createComposeLoadingDialog(context: Context): Dialog {
    return Dialog(context).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCanceledOnTouchOutside(false)
        
        val composeView = ComposeView(context).apply {
            setContent {
                SynapseTheme {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingSynapse()
                    }
                }
            }
        }
        setContentView(composeView)
    }
}
