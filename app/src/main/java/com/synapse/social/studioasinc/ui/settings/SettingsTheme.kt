package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive design tokens for the Settings feature.
 * 
 * These design tokens provide consistent styling across all settings screens,
 * following Material Design 3 guidelines with expressive surfaces and dynamic color support.
 * 
 * Requirements: 1.4, 4.1
 */

/**
 * Semantic color mappings for settings screens.
 * Colors are derived from MaterialTheme.colorScheme for dynamic theming support.
 */
object SettingsColors {
    /**
     * Tint color for category icons in the settings hub.
     */
    val categoryIconTint: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    /**
     * Background color for category icon containers.
     */
    val categoryBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primaryContainer


    /**
     * Color for section titles and headers.
     */
    val sectionTitle: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    /**
     * Background color for settings cards.
     */
    val cardBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainer

    /**
     * Elevated card background for profile header and important sections.
     */
    val cardBackgroundElevated: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHigh

    /**
     * Background color for destructive action buttons (e.g., Delete Account).
     */
    val destructiveButton: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.errorContainer

    /**
     * Text color for destructive action buttons.
     */
    val destructiveText: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onErrorContainer

    /**
     * Color for active toggle switches.
     */
    val toggleActive: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    /**
     * Track color for toggle switches.
     */
    val toggleTrack: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceVariant

    /**
     * Color for chevron icons and secondary icons.
     */
    val chevronIcon: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    /**
     * Color for dividers between settings items.
     */
    val divider: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    /**
     * Color for item icons.
     */
    val itemIcon: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * Shape definitions for settings UI components.
 * Uses consistent corner radii following Material 3 Expressive guidelines.
 */
object SettingsShapes {
    /**
     * Shape for large cards like profile header (28dp corners).
     */
    val cardShape: Shape = RoundedCornerShape(28.dp)

    /**
     * Shape for settings sections and grouped items (24dp corners).
     */
    val sectionShape: Shape = RoundedCornerShape(24.dp)

    /**
     * Shape for individual items and buttons (16dp corners).
     */
    val itemShape: Shape = RoundedCornerShape(16.dp)

    /**
     * Shape for text fields and dropdowns (12dp corners).
     */
    val inputShape: Shape = RoundedCornerShape(12.dp)

    /**
     * Shape for small chips and badges (8dp corners).
     */
    val chipShape: Shape = RoundedCornerShape(8.dp)
}

/**
 * Spacing and sizing values for consistent layout across settings screens.
 */
object SettingsSpacing {
    /**
     * Horizontal padding for screen content.
     */
    val screenPadding: Dp = 16.dp

    /**
     * Vertical spacing between sections.
     */
    val sectionSpacing: Dp = 12.dp

    /**
     * Spacing between items within a card (using dividers instead of gaps).
     */
    val itemSpacing: Dp = 0.dp

    /**
     * Padding for individual settings items.
     */
    val itemPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

    /**
     * Horizontal padding for item content.
     */
    val itemHorizontalPadding: Dp = 16.dp

    /**
     * Vertical padding for item content.
     */
    val itemVerticalPadding: Dp = 12.dp

    /**
     * Standard icon size for settings items.
     */
    val iconSize: Dp = 24.dp

    /**
     * Avatar size for profile header.
     */
    val avatarSize: Dp = 64.dp

    /**
     * Padding for profile header card.
     */
    val profileHeaderPadding: Dp = 16.dp

    /**
     * Spacing between icon and text in settings items.
     */
    val iconTextSpacing: Dp = 16.dp

    /**
     * Minimum touch target size for accessibility.
     */
    val minTouchTarget: Dp = 48.dp
}

/**
 * Typography mappings for settings screens.
 * Uses Material 3 type scale with appropriate weights.
 */
object SettingsTypography {
    /**
     * Style for screen titles in LargeTopAppBar.
     */
    val screenTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.headlineMedium

    /**
     * Style for section headers.
     */
    val sectionHeader: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium

    /**
     * Style for setting item titles.
     */
    val itemTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)

    /**
     * Style for setting item subtitles/descriptions.
     */
    val itemSubtitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium

    /**
     * Style for profile name in header.
     */
    val profileName: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleLarge

    /**
     * Style for profile email in header.
     */
    val profileEmail: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium

    /**
     * Style for button text.
     */
    val buttonText: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelLarge
}

/**
 * Position of a settings item within a group for Material 3 Expressive corner radius styling.
 */
enum class SettingsItemPosition {
    /** Single item (all corners rounded) */
    Single,
    /** First item in group (top corners rounded) */
    Top,
    /** Middle item in group (no corners rounded) */
    Middle,
    /** Last item in group (bottom corners rounded) */
    Bottom;

    /**
     * Get the appropriate shape for this position.
     */
    fun getShape(): Shape = when (this) {
        Single -> SettingsShapes.itemShape
        Top -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        Middle -> RoundedCornerShape(0.dp)
        Bottom -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    }
}
