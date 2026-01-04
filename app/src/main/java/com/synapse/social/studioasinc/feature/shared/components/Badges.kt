package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.theme.SynapseTheme

@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_verified), // Ensure this drawable exists or use a vector
        contentDescription = "Verified",
        modifier = modifier
            .size(16.dp)
            .padding(start = 4.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun GenderBadge(
    gender: String,
    modifier: Modifier = Modifier
) {
    // Determine icon based on gender string
    val iconRes = when (gender.lowercase()) {
        "male" -> R.drawable.ic_male
        "female" -> R.drawable.ic_female
        else -> null
    }

    if (iconRes != null) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = gender,
            modifier = modifier
                .size(16.dp)
                .padding(start = 4.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VerifiedBadgePreview() {
    SynapseTheme {
        VerifiedBadge()
    }
}
