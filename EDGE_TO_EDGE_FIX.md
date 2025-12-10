# Edge-to-Edge Status Bar Fix

## Problem
Profile and Inbox Compose activities were using static status bar colors that didn't adjust with the edge-to-edge theme colors, causing visual inconsistencies.

## Solution
Created a comprehensive edge-to-edge system that automatically adjusts status bar colors based on the current theme.

## Changes Made

### 1. Created EdgeToEdgeUtils.kt
- `EdgeToEdgeUtils.setupEdgeToEdgeActivity()` - Sets up edge-to-edge for activities
- `EdgeToEdgeUtils.SetupEdgeToEdge()` - Composable for automatic status bar color adjustment
- `EdgeToEdgeUtils.updateStatusBarAppearance()` - Manual status bar appearance updates

### 2. Updated SynapseTheme.kt
- Added `enableEdgeToEdge` parameter (default: true)
- Automatically calls `EdgeToEdgeUtils.SetupEdgeToEdge()` when enabled
- Ensures consistent edge-to-edge behavior across all screens

### 3. Updated AuthTheme.kt
- Added `enableEdgeToEdge` parameter (default: true)
- Consistent with SynapseTheme behavior

### 4. Updated Compose Activities
- **ProfileComposeActivity**: Added edge-to-edge setup
- **InboxComposeActivity**: Added edge-to-edge setup  
- **DirectChatComposeActivity**: Added edge-to-edge setup
- **AuthComposeActivity**: Updated to use new AuthTheme parameter

## How It Works

1. **Activity Setup**: Each activity calls `EdgeToEdgeUtils.setupEdgeToEdgeActivity(this)` in `onCreate()` before `setContent()`

2. **Theme Integration**: Themes automatically call `EdgeToEdgeUtils.SetupEdgeToEdge()` when `enableEdgeToEdge = true`

3. **Dynamic Updates**: Status bar colors automatically adjust when:
   - Theme changes (light/dark)
   - Dynamic colors change
   - Color scheme updates

## Benefits

- ✅ Status bar colors now properly adjust with theme
- ✅ Consistent edge-to-edge behavior across all Compose screens
- ✅ Automatic handling - no manual setup needed for new screens
- ✅ Supports both light and dark themes
- ✅ Supports dynamic colors (Android 12+)
- ✅ Backward compatible with older Android versions

## Usage for New Compose Activities

```kotlin
class NewComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge (required)
        EdgeToEdgeUtils.setupEdgeToEdgeActivity(this)
        
        setContent {
            SynapseTheme(enableEdgeToEdge = true) { // Default is true
                // Your content here
            }
        }
    }
}
```

## Testing

Test the following scenarios:
1. Switch between light and dark themes
2. Enable/disable dynamic colors (Android 12+)
3. Navigate between Profile and Inbox screens
4. Check status bar icon visibility in both themes
5. Verify edge-to-edge content layout

## Files Modified

- `util/EdgeToEdgeUtils.kt` (new)
- `ui/theme/SynapseTheme.kt`
- `ui/theme/AuthTheme.kt`
- `ProfileComposeActivity.kt`
- `InboxComposeActivity.kt`
- `DirectChatComposeActivity.kt`
- `AuthComposeActivity.kt`
