package com.synapse.social.studioasinc.compose.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.viewmodel.FollowButtonViewModel

@Composable
fun FollowButtonCompose(
    targetUserId: String,
    modifier: Modifier = Modifier,
    viewModel: FollowButtonViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(targetUserId) {
        viewModel.initialize(targetUserId)
    }

    when {
        uiState.isLoading -> {
            OutlinedButton(
                onClick = { },
                enabled = false,
                modifier = modifier
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp
                )
            }
        }
        
        uiState.isFollowing -> {
            OutlinedButton(
                onClick = { viewModel.toggleFollow() },
                modifier = modifier,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(stringResource(R.string.following))
            }
        }
        
        else -> {
            Button(
                onClick = { viewModel.toggleFollow() },
                modifier = modifier
            ) {
                Text(stringResource(R.string.follow))
            }
        }
    }
}
