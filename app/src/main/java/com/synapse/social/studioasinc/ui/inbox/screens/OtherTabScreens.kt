package com.synapse.social.studioasinc.ui.inbox.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.ui.inbox.components.InboxEmptyState
import com.synapse.social.studioasinc.ui.inbox.models.EmptyStateType

@Composable
fun CallsTabScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    InboxEmptyState(
        type = EmptyStateType.CALLS,
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    )
}

@Composable
fun ContactsTabScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    InboxEmptyState(
        type = EmptyStateType.CONTACTS,
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    )
}
