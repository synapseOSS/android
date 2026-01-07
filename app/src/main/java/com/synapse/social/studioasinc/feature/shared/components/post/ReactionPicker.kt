package com.synapse.social.studioasinc.ui.components.post

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapse.social.studioasinc.domain.model.ReactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionPicker(
    onReactionSelected: (ReactionType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ReactionType.values().forEach { reaction ->
                ReactionItem(
                    reaction = reaction,
                    onClick = onReactionSelected
                )
            }
        }
    }
}

@Composable
fun ReactionItem(
    reaction: ReactionType,
    onClick: (ReactionType) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick(reaction) }
    ) {
        Image(
            painter = painterResource(id = reaction.iconRes),
            contentDescription = reaction.displayName,
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp)
        )
    }
}
