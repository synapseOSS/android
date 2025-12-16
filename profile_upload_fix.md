# Profile and Cover Photo Upload Fix

## Problem Analysis
The profile and cover photo uploads are failing in the edit profile screen. Based on code analysis, the issues are:

1. **URI Conversion Issues**: Similar to the post upload fix, content URIs may not be properly converted to file paths
2. **File Processing Errors**: The image compression/resizing might be failing
3. **Storage Upload Failures**: Issues with Supabase storage upload
4. **Error Handling**: Poor error reporting makes debugging difficult

## Root Causes Identified

### 1. URI to File Path Conversion
The `FileUtil.convertUriToFilePath()` method may fail for certain URI types, especially on newer Android versions with scoped storage.

### 2. File Processing Pipeline
The image processing pipeline in `EditProfileViewModel` has potential failure points:
- URI to file path conversion
- Image compression/resizing
- Temporary file creation

### 3. Storage Service Issues
The `SupabaseStorageService` may have authentication or network issues.

## Fixes Applied

### 1. Enhanced Error Handling in EditProfileViewModel

**Changes Made:**
- Added comprehensive logging throughout the upload process
- Implemented fallback URI handling when direct path conversion fails
- Added file validation at each step
- Enhanced error messages for better debugging

**Key Improvements:**
```kotlin
// Fallback URI handling
if (realFilePath == null) {
    val tempInputFile = File(context.cacheDir, "temp_input_avatar_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        tempInputFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    if (tempInputFile.exists() && tempInputFile.length() > 0) {
        realFilePath = tempInputFile.absolutePath
    }
}
```

### 2. Improved SupabaseStorageService

**Changes Made:**
- Added retry logic with exponential backoff
- Enhanced file validation before upload
- Better error logging and reporting
- Validation of public URL generation

**Key Improvements:**
```kotlin
// Retry logic for uploads
for (attempt in 1..3) {
    try {
        storage.from(bucket).upload(path, fileBytes) { upsert = false }
        uploadSuccess = true
        break
    } catch (e: Exception) {
        if (attempt < 3) {
            delay(1000 * attempt) // Exponential backoff
        }
    }
}
```

### 3. Debug Utility

**Created:** `ProfileUploadDebugUtil.kt`
- Test URI processing pipeline
- Validate file conversion steps
- Debug compression issues
- Test storage connectivity

## How to Use the Fix

### 1. Testing URI Processing
```kotlin
val result = ProfileUploadDebugUtil.testUriProcessing(context, uri)
if (!result.isSuccessful) {
    Log.e("Debug", "URI processing failed: ${result.error}")
}
```

### 2. Monitoring Logs
Look for these log tags:
- `EditProfile`: Main upload process
- `SupabaseStorage`: Storage operations
- `ProfileUploadDebug`: Debug utility

### 3. Common Error Messages
- **"Failed to process image"**: URI conversion or file processing failed
- **"Upload failed after retries"**: Network or storage service issues
- **"File not found"**: URI conversion returned invalid path
- **"Image compression failed"**: Bitmap processing issues

## Troubleshooting Steps

### 1. Check Logs
```bash
adb logcat | grep -E "(EditProfile|SupabaseStorage|ProfileUploadDebug)"
```

### 2. Test URI Conversion
Use the debug utility to test specific URIs that are failing.

### 3. Verify Storage Configuration
Ensure Supabase storage buckets (`avatars`, `covers`) exist and have proper permissions.

### 4. Check Network Connectivity
Verify the device has internet access and can reach Supabase servers.

## Prevention Measures

### 1. Input Validation
- Validate URI before processing
- Check file size limits
- Verify image format support

### 2. Graceful Degradation
- Fallback to copying URI content if direct path access fails
- Retry failed uploads with exponential backoff
- Clear error messages for users

### 3. Monitoring
- Log all critical steps in the upload process
- Track upload success/failure rates
- Monitor storage service health

## Testing Checklist

- [ ] Test with images from gallery
- [ ] Test with images from camera
- [ ] Test with different image formats (JPG, PNG)
- [ ] Test with large images (>5MB)
- [ ] Test with poor network conditions
- [ ] Verify error messages are user-friendly
- [ ] Check that retry mechanism works
- [ ] Validate uploaded images display correctly

## Files Modified

1. **EditProfileViewModel.kt**: Enhanced error handling and URI processing
2. **SupabaseStorageService.kt**: Added retry logic and better validation
3. **ProfileUploadDebugUtil.kt**: New debug utility for troubleshooting

## Expected Behavior After Fix

1. **Successful Upload**: Images upload reliably with proper error handling
2. **Clear Error Messages**: Users get meaningful feedback when uploads fail
3. **Automatic Retry**: Temporary network issues are handled automatically
4. **Fallback Processing**: URI conversion issues are handled gracefully
5. **Debug Support**: Developers can easily troubleshoot upload issues
