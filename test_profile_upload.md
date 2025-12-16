# Profile Upload Test Script

## Manual Testing Steps

### 1. Test Avatar Upload

1. **Open Edit Profile Screen**
   - Navigate to profile → Edit Profile
   - Tap on avatar/profile picture

2. **Select Image from Gallery**
   - Choose "Gallery" or "Photos"
   - Select a medium-sized image (1-5MB)
   - Observe upload progress indicator

3. **Check Logs**
   ```bash
   adb logcat | grep EditProfile
   ```
   - Look for "Processing avatar URI"
   - Verify "Avatar upload successful" message

4. **Verify Result**
   - Avatar should update in UI
   - Save profile and check if avatar persists
   - Navigate away and back to verify

### 2. Test Cover Photo Upload

1. **Select Cover Photo**
   - In Edit Profile, tap cover photo area
   - Select image from gallery

2. **Monitor Process**
   - Check upload progress
   - Watch for error messages
   - Verify logs show successful upload

3. **Validate Result**
   - Cover photo should display correctly
   - Save and verify persistence

### 3. Test Error Scenarios

1. **Network Issues**
   - Turn off WiFi/mobile data during upload
   - Should show retry mechanism
   - Re-enable network and verify retry works

2. **Large Images**
   - Select very large image (>10MB)
   - Should compress and upload successfully
   - Check compressed file size in logs

3. **Invalid URIs**
   - Try with different image sources
   - Camera photos
   - Downloaded images
   - Screenshots

### 4. Debug Failed Uploads

If uploads fail, use the debug utility:

```kotlin
// In your test activity or fragment
val debugResult = ProfileUploadDebugUtil.testUriProcessing(this, selectedUri)
Log.d("Test", "Debug result: $debugResult")

if (!debugResult.isSuccessful) {
    Log.e("Test", "Processing failed: ${debugResult.error}")
    // Check specific failure points:
    // - URI conversion: debugResult.uriConversionSuccess
    // - File exists: debugResult.originalFileExists
    // - Compression: debugResult.compressionSuccess
}
```

## Expected Log Output

### Successful Avatar Upload
```
D/EditProfile: Processing avatar URI: content://media/external/images/media/1234
D/EditProfile: Converted file path: /storage/emulated/0/DCIM/Camera/IMG_20231216_143000.jpg
D/EditProfile: Compressing image to: /data/data/com.synapse.social.studioasinc/cache/temp_avatar_1702734180000.jpg
D/EditProfile: Image compressed successfully, size: 245760 bytes
D/EditProfile: Starting avatar upload for user: user123, file: /data/data/com.synapse.social.studioasinc/cache/temp_avatar_1702734180000.jpg
D/SupabaseStorage: Uploading image from: /data/data/com.synapse.social.studioasinc/cache/temp_avatar_1702734180000.jpg to bucket: avatars
D/SupabaseStorage: File size: 245760 bytes
D/SupabaseStorage: Uploading to path: user123/uuid-here.jpg
D/SupabaseStorage: Upload successful: https://supabase-url/storage/v1/object/public/avatars/user123/uuid-here.jpg
D/EditProfile: Avatar upload successful: https://supabase-url/storage/v1/object/public/avatars/user123/uuid-here.jpg
```

### Failed Upload with Fallback
```
D/EditProfile: Processing avatar URI: content://com.android.providers.media.documents/document/image%3A1000300165
D/EditProfile: Converted file path: null
D/EditProfile: URI conversion failed, copying content to temp file
D/EditProfile: Successfully copied to temp file: /data/data/com.synapse.social.studioasinc/cache/temp_input_avatar_1702734180000.jpg
D/EditProfile: Compressing image to: /data/data/com.synapse.social.studioasinc/cache/temp_avatar_1702734180000.jpg
D/EditProfile: Image compressed successfully, size: 245760 bytes
```

## Common Issues and Solutions

### Issue: "Failed to process image"
**Cause**: URI conversion and fallback both failed
**Solution**: Check file permissions, try different image source

### Issue: "Image compression failed"
**Cause**: Bitmap processing error, corrupted image, or insufficient memory
**Solution**: Try smaller image, restart app to free memory

### Issue: "Upload failed after retries"
**Cause**: Network issues or Supabase service problems
**Solution**: Check internet connection, verify Supabase configuration

### Issue: "User not logged in"
**Cause**: Authentication session expired
**Solution**: Re-login to the app

## Performance Benchmarks

### Expected Upload Times (WiFi)
- Small image (< 1MB): 2-5 seconds
- Medium image (1-5MB): 5-15 seconds  
- Large image (5-10MB): 15-30 seconds

### Expected File Sizes After Compression
- Original 8MP photo (~3MB) → Compressed (~200-500KB)
- Original screenshot (~1MB) → Compressed (~100-300KB)

## Automated Testing

For automated testing, you can create unit tests:

```kotlin
@Test
fun testUriProcessing() {
    val mockUri = Uri.parse("content://media/external/images/media/1234")
    val result = ProfileUploadDebugUtil.testUriProcessing(context, mockUri)
    assertTrue("URI processing should succeed", result.isSuccessful)
}
```
