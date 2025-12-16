# Content URI Fix for Post Upload

## Problem
When uploading posts with images, content URIs like `content://com.android.providers.media.documents/document/image%3A1000300165` were being saved directly to the database instead of being converted to proper file paths or uploaded to cloud storage.

## Root Cause
The issue was in the `FileUtil` class import in `CreatePostViewModel`. The app was using the wrong `FileUtil` class:

- **Wrong**: `com.synapse.social.studioasinc.util.FileUtil` - This class was just returning the URI as a string
- **Correct**: `com.synapse.social.studioasinc.FileUtil` - This class has proper URI to file path conversion logic

## Files Fixed

### 1. CreatePostViewModel.kt
- **Changed import**: From `com.synapse.social.studioasinc.util.FileUtil` to `com.synapse.social.studioasinc.FileUtil`
- **Added logging**: To debug URI conversion process
- **Added validation**: To prevent content URIs from being saved to database

### 2. MediaUploadManager.kt
- **Fixed import**: To use the correct FileUtil class

### 3. util/FileUtil.kt
- **Completely rewritten**: Now properly converts content URIs to file paths
- **Added fallback**: If direct path access fails, copies URI content to temp file
- **Better error handling**: Handles various URI schemes and document providers

### 4. UriTestUtil.kt (New)
- **Testing utility**: For debugging URI conversion issues
- **Validation methods**: To check for invalid content URIs in media items

## How the Fix Works

1. **URI Selection**: User selects images through the media picker
2. **URI Conversion**: `FileUtil.convertUriToFilePath()` converts content URIs to actual file paths
3. **Fallback Handling**: If direct path access fails, content is copied to a temporary file
4. **Validation**: Before saving, the app validates that no content URIs remain
5. **Upload**: Proper file paths are used for uploading to ImgBB or other storage services

## URI Conversion Logic

The fixed `FileUtil.convertUriToFilePath()` method handles:

- **Document URIs**: `content://com.android.providers.media.documents/...`
- **Downloads URIs**: `content://com.android.providers.downloads.documents/...`
- **External storage URIs**: `content://com.android.externalstorage.documents/...`
- **Generic content URIs**: Falls back to copying content to temp file
- **File URIs**: Direct file path extraction

## Testing

To verify the fix works:

1. **Check logs**: Look for "CreatePost" logs showing URI conversion
2. **Database inspection**: Verify no content URIs are saved in media_items
3. **Image display**: Confirm images display correctly after upload
4. **Use UriTestUtil**: Call `testUriConversion()` to debug specific URIs

## Error Messages

- **Before fix**: Content URIs saved directly, images fail to load
- **After fix**: Proper file paths or uploaded URLs, images load correctly
- **Validation error**: "Media processing failed. Please try selecting the images again."

## Prevention

The fix includes validation that prevents content URIs from reaching the database:

```kotlin
val invalidUrls = currentState.mediaItems.filter { it.url.startsWith("content://") }
if (invalidUrls.isNotEmpty()) {
    _uiState.update { it.copy(error = "Media processing failed. Please try selecting the images again.") }
    return
}
```

This ensures users get immediate feedback if URI conversion fails, rather than silently saving invalid URLs.
