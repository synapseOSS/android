package com.synapse.social.studioasinc.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.synapse.social.studioasinc.core.util.ImageLoader
import com.synapse.social.studioasinc.R

/**
 * Reusable Material 3 Expressive settings UI components.
 * 
 * This file contains all the composable building blocks for settings screens,
 * following Material Design 3 guidelines with consistent styling, spacing, and behavior.
 * 
 * Requirements: 1.4, 2.1, 3.1, 4.1, 5.1, 6.1
 */

// ============================================================================
// Settings Item Components
// ============================================================================

/**
 * Enhanced toggle settings item with Material 3 Expressive corner radius support.
 * 
 * @param title The main title text
 * @param subtitle Optional descriptive text below the title
 * @param icon Optional leading icon resource
 * @param checked Current toggle state
 * @param onCheckedChange Callback when toggle state changes
 * @param enabled Whether the item is interactive
 * @param position Position in group for corner radius styling
 * 
 * Requirements: 1.4, 2.1, 3.1, 4.1, 5.1, 6.1
 */
@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    val toggleDescription = stringResource(R.string.settings_toggle_description)
    val fullDescription = "$title, $toggleDescription, ${if (checked) "enabled" else "disabled"}"
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .semantics(mergeDescendants = true) {
                    contentDescription = fullDescription
                }
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
                .heightIn(min = SettingsSpacing.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null, // Merged into parent semantics
                    modifier = Modifier.size(SettingsSpacing.iconSize),
                    tint = SettingsColors.itemIcon
                )
                Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
            }
            
            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = SettingsTypography.itemTitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface 
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = SettingsTypography.itemSubtitle,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Material 3 Switch with primary thumb and primaryContainer track
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

/**
 * Enhanced clickable settings item with Material 3 Expressive corner radius support.
 * 
 * @param title The main title text
 * @param subtitle Optional descriptive text below the title
 * @param icon Optional leading icon resource
 * @param onClick Callback when the item is clicked
 * @param enabled Whether the item is interactive
 * @param position Position in group for corner radius styling
 * 
 * Requirements: 1.4, 2.1, 3.1, 4.1, 5.1, 6.1
 */
@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
                .heightIn(min = SettingsSpacing.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(SettingsSpacing.iconSize),
                    tint = SettingsColors.itemIcon
                )
                Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
            }
            
            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = SettingsTypography.itemTitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface 
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = SettingsTypography.itemSubtitle,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

/**
 * Enhanced navigation settings item with Material 3 Expressive corner radius support.
 * 
 * @param title The main title text
 * @param subtitle Optional descriptive text below the title
 * @param icon Optional leading icon resource
 * @param onClick Callback when the item is clicked
 * @param enabled Whether the item is interactive
 * @param position Position in group for corner radius styling
 * 
 * Requirements: 1.4, 2.1, 3.1, 4.1, 5.1, 6.1
 */
@Composable
fun SettingsNavigationItem(
    title: String,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    val chevronDescription = stringResource(R.string.settings_chevron_description)
    val fullDescription = "$title, $chevronDescription"
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = fullDescription
                }
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
                .heightIn(min = SettingsSpacing.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null, // Merged into parent semantics
                    modifier = Modifier.size(SettingsSpacing.iconSize),
                    tint = SettingsColors.itemIcon
                )
                Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
            }
            
            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = SettingsTypography.itemTitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface 
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = SettingsTypography.itemSubtitle,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Chevron icon with onSurfaceVariant at 0.5 alpha
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null, // Merged into parent semantics
                modifier = Modifier.size(SettingsSpacing.iconSize),
                tint = SettingsColors.chevronIcon
            )
        }
    }
}

/**
 * Enhanced selection settings item with Material 3 Expressive corner radius support.
 * 
 * @param title The main title text
 * @param subtitle Optional descriptive text below the title
 * @param icon Optional leading icon resource
 * @param options List of selectable options
 * @param selectedOption Currently selected option
 * @param onSelect Callback when an option is selected
 * @param enabled Whether the item is interactive
 * @param position Position in group for corner radius styling
 * 
 * Requirements: 1.4, 2.1, 3.1, 4.1, 5.1, 6.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSelectionItem(
    title: String,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    var expanded by remember { mutableStateOf(false) }
    val dropdownDescription = stringResource(R.string.settings_dropdown_description)
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title, $dropdownDescription, $selectedOption"
                }
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Leading icon
                if (icon != null) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(SettingsSpacing.iconSize),
                        tint = SettingsColors.itemIcon
                    )
                    Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
                }
                
                // Title
                Text(
                    text = title,
                    style = SettingsTypography.itemTitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface 
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = SettingsTypography.itemSubtitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.padding(start = if (icon != null) 40.dp else 0.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Dropdown menu
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = !expanded },
                modifier = Modifier.padding(start = if (icon != null) 40.dp else 0.dp)
            ) {
                OutlinedTextField(
                    value = selectedOption,
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    textStyle = SettingsTypography.itemSubtitle
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onSelect(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced slider settings item with Material 3 Expressive corner radius support.
 * 
 * @param title The main title text
 * @param subtitle Optional descriptive text below the title
 * @param value Current slider value
 * @param valueRange Range of valid values
 * @param steps Number of discrete steps (0 for continuous)
 * @param onValueChange Callback when value changes
 * @param valueLabel Function to format the value for display
 * @param enabled Whether the item is interactive
 * @param position Position in group for corner radius styling
 * 
 * Requirements: 1.4, 2.1, 3.1, 4.1, 5.1, 6.1
 */
@Composable
fun SettingsSliderItem(
    title: String,
    subtitle: String? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    valueLabel: (Float) -> String = { it.toString() },
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    val sliderDescription = stringResource(R.string.settings_slider_description)
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title, $sliderDescription, ${valueLabel(value)}"
                }
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
        ) {
            // Title
            Text(
                text = title,
                style = SettingsTypography.itemTitle,
                color = if (enabled) MaterialTheme.colorScheme.onSurface 
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = SettingsTypography.itemSubtitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Slider
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Preview text
            Text(
                text = valueLabel(value),
                style = SettingsTypography.itemSubtitle,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * A button settings item with a FilledTonalButton.
 * 
 * Displays a button with 16dp corner radius.
 * Uses consistent 16dp horizontal and vertical padding.
 * 
 * @param title The button text
 * @param onClick Callback when the button is clicked
 * @param isDestructive Whether this is a destructive action (uses error colors)
 * @param enabled Whether the button is interactive
 * @param modifier Optional modifier for the button
 * 
 * Requirements: 1.4, 2.1, 3.1, 4.1, 5.1, 6.1
 */
@Composable
fun SettingsButtonItem(
    title: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            ),
        shape = SettingsShapes.itemShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isDestructive) SettingsColors.destructiveButton 
                           else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isDestructive) SettingsColors.destructiveText 
                          else MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            text = title,
            style = SettingsTypography.buttonText
        )
    }
}

/**
 * A header settings item with titleMedium typography in primary color.
 * 
 * Displays a section header with consistent styling.
 * Uses consistent 16dp horizontal and vertical padding.
 * 
 * @param title The header text
 * 
 * Requirements: 1.4, 2.1, 3.1, 4.1, 5.1, 6.1
 */
@Composable
fun SettingsHeaderItem(
    title: String
) {
    Text(
        text = title,
        style = SettingsTypography.sectionHeader,
        color = SettingsColors.sectionTitle,
        modifier = Modifier.padding(
            horizontal = SettingsSpacing.itemHorizontalPadding,
            vertical = SettingsSpacing.itemVerticalPadding
        )
    )
}


// ============================================================================
// Container Components
// ============================================================================

/**
 * A card container for grouping related settings items with Material 3 Expressive styling.
 * 
 * Uses surfaceContainer background with 24dp corner radius.
 * Automatically applies top, middle, bottom corner radius styling to child items.
 * 
 * @param modifier Optional modifier for the card
 * @param content The settings items to display in the card
 * 
 * Requirements: 1.1, 1.4
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsShapes.sectionShape,
        color = SettingsColors.cardBackground,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

/**
 * A section container with a header and card content using Material 3 Expressive design.
 * 
 * Displays a titleMedium header in primary color followed by a SettingsCard.
 * Uses 24dp spacing between sections and proper corner radius grouping.
 * 
 * @param title The section header text
 * @param modifier Optional modifier for the section
 * @param content The settings items to display in the section card
 * 
 * Requirements: 1.1, 1.4
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = SettingsTypography.sectionHeader,
            color = SettingsColors.sectionTitle,
            modifier = Modifier.padding(start = 8.dp)
        )
        SettingsCard {
            content()
        }
    }
}

/**
 * Enhanced settings group with automatic corner radius styling for Material 3 Expressive design.
 * 
 * Automatically applies:
 * - Top corners (16dp) to first item
 * - No corners (0dp) to middle items  
 * - Bottom corners (16dp) to last item
 * - Dividers between items
 * 
 * @param modifier Optional modifier for the group
 * @param items List of settings items to display
 * 
 * Requirements: 1.1, 1.4
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    items: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsShapes.sectionShape,
        color = SettingsColors.cardBackground,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            items()
        }
    }
}

/**
 * A divider to separate settings items within a card.
 * 
 * Uses outlineVariant color at 0.5 alpha.
 * 
 * Requirements: 1.1, 1.4
 */
@Composable
fun SettingsDivider() {
    HorizontalDivider(
        color = SettingsColors.divider,
        thickness = 1.dp
    )
}

// ============================================================================
// Profile Header Component
// ============================================================================

/**
 * A profile header card for the Settings Hub.
 * 
 * Displays a compact row layout with name/email on the left and avatar on the right.
 * Uses surfaceContainerHigh background with 28dp corners.
 * 
 * @param displayName User's display name
 * @param email User's email address
 * @param avatarUrl Optional URL for the user's avatar image
 * @param modifier Optional modifier for the card
 * 
 * Requirements: 1.5
 */
@Composable
fun ProfileHeaderCard(
    displayName: String,
    email: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val profileAvatarDescription = stringResource(R.string.settings_profile_avatar_description)
    
    // Debug logging
    android.util.Log.d("ProfileHeaderCard", "Rendering profile card - avatarUrl: $avatarUrl, displayName: $displayName")
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SettingsSpacing.profileHeaderPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
    // Avatar with border (Left side)
            Box(
                modifier = Modifier.size(SettingsSpacing.avatarSize)
            ) {
                if (avatarUrl != null && avatarUrl.isNotBlank()) {
                    android.util.Log.d("ProfileHeaderCard", "Loading image from URL: $avatarUrl")
                    AsyncImage(
                        model = ImageLoader.buildImageRequest(LocalContext.current, avatarUrl),
                        contentDescription = "$profileAvatarDescription, $displayName",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder avatar
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_person),
                                contentDescription = "$profileAvatarDescription, $displayName",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Border
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {}
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name and email (Right side)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center // Vertically centered
            ) {
                Text(
                    text = displayName,
                    style = SettingsTypography.profileName,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = email,
                    style = SettingsTypography.profileEmail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
