package com.synapse.social.studioasinc.ui.profile.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class UserDetails(
    val location: String? = null,
    val joinedDate: String? = null,
    val relationshipStatus: String? = null,
    val birthday: String? = null,
    val work: String? = null,
    val education: String? = null,
    val website: String? = null,
    val gender: String? = null,
    val pronouns: String? = null,
    val linkedAccounts: List<LinkedAccount> = emptyList()
)

data class LinkedAccount(
    val platform: String,
    val username: String
)

/**
 * Enhanced User Details Section with modern card design and animations.
 * 
 * Features:
 * - Modern card with subtle shadow
 * - Animated expand/collapse
 * - Two-column grid layout for details
 * - Staggered item animations
 * - Quick action buttons
 */
@Composable
fun UserDetailsSection(
    details: UserDetails,
    isOwnProfile: Boolean,
    onCustomizeClick: () -> Unit,
    onWebsiteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Check if there are any details to show
    val hasDetails = listOfNotNull(
        details.location,
        details.joinedDate,
        details.work,
        details.education,
        details.website,
        details.birthday,
        details.relationshipStatus,
        details.gender,
        details.pronouns
    ).any { it.isNotBlank() } || details.linkedAccounts.isNotEmpty()
    
    if (!hasDetails && !isOwnProfile) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (hasDetails) {
                    ExpandCollapseButton(
                        expanded = expanded,
                        onClick = { expanded = !expanded }
                    )
                }
            }
            
            // Collapsed summary (visible when not expanded)
            if (!expanded && hasDetails) {
                Spacer(modifier = Modifier.height(6.dp))
                CollapsedSummary(details = details)
            }
            
            // Expanded content
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(200)) + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = fadeOut(animationSpec = tween(100)) + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ExpandedDetailsContent(
                        details = details,
                        onWebsiteClick = onWebsiteClick
                    )
                    
                    if (isOwnProfile) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onCustomizeClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Details")
                        }
                    }
                }
            }
            
            // Empty state for own profile
            if (!hasDetails && isOwnProfile) {
                Spacer(modifier = Modifier.height(8.dp))
                EmptyDetailsState(onAddClick = onCustomizeClick)
            }
        }
    }
}

/**
 * Animated expand/collapse button with rotation.
 */
@Composable
private fun ExpandCollapseButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "expandRotation"
    )
    
    TextButton(onClick = onClick) {
        Text(if (expanded) "Less" else "More")
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            modifier = Modifier
                .size(18.dp)
                .rotate(rotation)
        )
    }
}

/**
 * Collapsed summary showing truncated text.
 */
@Composable
private fun CollapsedSummary(details: UserDetails) {
    val summaryText = buildString {
        details.work?.let { append(it) }
        details.location?.let {
            if (isNotEmpty()) append(" • ")
            append(it)
        }
        details.joinedDate?.let {
            if (it.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append("Joined $it")
            }
        }

        // If summary is still empty check other fields
        if (isEmpty()) {
            details.website?.let { append(it) }
            if (isEmpty()) {
                details.relationshipStatus?.let { append(it) }
            }
            if (isEmpty()) {
                 if (details.linkedAccounts.isNotEmpty()) {
                     append("Linked Accounts: ${details.linkedAccounts.size}")
                 } else {
                     append("Tap 'More' to see details")
                 }
            }
        }
    }

    Text(
        text = summaryText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
    )
}

/**
 * Full expanded details content.
 */
@Composable
private fun ExpandedDetailsContent(
    details: UserDetails,
    onWebsiteClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val detailItems = buildList {
            details.location?.let { 
                add(Triple(Icons.Outlined.LocationOn, "Location", it)) 
            }
            details.work?.let { 
                add(Triple(Icons.Outlined.Work, "Work", it)) 
            }
            details.education?.let { 
                add(Triple(Icons.Outlined.School, "Education", it)) 
            }
            details.website?.let { 
                add(Triple(Icons.Outlined.Link, "Website", it)) 
            }
            details.joinedDate?.let { 
                add(Triple(Icons.Outlined.CalendarToday, "Joined", it)) 
            }
            details.birthday?.let { 
                add(Triple(Icons.Outlined.Cake, "Birthday", it)) 
            }
            details.relationshipStatus?.let { 
                add(Triple(Icons.Outlined.Favorite, "Relationship", it)) 
            }
            details.gender?.let { 
                add(Triple(Icons.Outlined.Person, "Gender", it)) 
            }
            details.pronouns?.let { 
                add(Triple(Icons.Outlined.Badge, "Pronouns", it)) 
            }
        }
        
        detailItems.forEachIndexed { index, (icon, label, value) ->
            SimpleDetailItem(
                icon = icon,
                label = label,
                value = value,
                isClickable = label == "Website",
                onClick = if (label == "Website") { { onWebsiteClick(value) } } else null,
                animationDelay = index * 30
            )
        }
        
        // Linked accounts section
        if (details.linkedAccounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Linked Accounts",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                details.linkedAccounts.forEach { account ->
                    LinkedAccountChip(account = account)
                }
            }
        }
    }
}

/**
 * Simple detail item without nested card background.
 */
@Composable
private fun SimpleDetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    isClickable: Boolean = false,
    onClick: (() -> Unit)? = null,
    animationDelay: Int = 0,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "detailAlpha"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (isClickable && onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isClickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Chip for linked social accounts.
 */
@Composable
private fun LinkedAccountChip(account: LinkedAccount) {
    AssistChip(
        onClick = { /* TODO: Open link */ },
        label = { Text(account.platform) },
        leadingIcon = {
            Icon(
                imageVector = getPlatformIcon(account.platform),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

/**
 * Get icon for social platform.
 */
private fun getPlatformIcon(platform: String): ImageVector {
    return when (platform.lowercase()) {
        "twitter", "x" -> Icons.Default.AlternateEmail
        "instagram" -> Icons.Default.CameraAlt
        "facebook" -> Icons.Default.Facebook
        "linkedin" -> Icons.Default.Work
        "github" -> Icons.Default.Code
        "youtube" -> Icons.Default.PlayCircle
        else -> Icons.Default.Link
    }
}

/**
 * Empty state when user hasn't added any details.
 */
@Composable
private fun EmptyDetailsState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Share more about yourself",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Details")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserDetailsSectionPreview() {
    MaterialTheme {
        UserDetailsSection(
            details = UserDetails(
                location = "San Francisco, CA",
                joinedDate = "January 2024",
                work = "Software Engineer at Tech Co",
                education = "Stanford University",
                website = "https://example.com",
                pronouns = "they/them"
            ),
            isOwnProfile = true,
            onCustomizeClick = {},
            onWebsiteClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserDetailsSectionEmptyPreview() {
    MaterialTheme {
        UserDetailsSection(
            details = UserDetails(),
            isOwnProfile = true,
            onCustomizeClick = {},
            onWebsiteClick = {}
        )
    }
}
