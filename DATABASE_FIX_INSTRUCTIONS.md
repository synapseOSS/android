# Database Column Mismatch Fix

## Problem
You're experiencing a "Database column mismatched" error when trying to post, comment, or chat in the Synapse app. This happens when the local database schema doesn't match the expected schema after a recent update.

## Root Cause
The app was updated from database version 1 to version 2, which added a `parentCommentId` column to the `comments` table to support threaded comments (replies). However, the migration didn't apply properly on your device.

## Solutions (Try in order)

### Solution 1: Use the Database Reset Script (Recommended)
1. Connect your Android device to your computer
2. Enable USB debugging on your device
3. Run the reset script:
   ```bash
   ./database_reset.sh
   ```
4. This will clear the app data and force the database to be recreated with the correct schema

### Solution 2: Manual App Data Clear
1. Go to your device's Settings
2. Navigate to Apps → Synapse
3. Tap "Storage"
4. Tap "Clear Data" (this will log you out)
5. Restart the app and log back in

### Solution 3: Reinstall the App
1. Uninstall the Synapse app from your device
2. Install the latest version from your app store or sideload the APK
3. Log back in

## Technical Details
The migration adds support for threaded comments by adding a `parentCommentId` column to the comments table:

```sql
ALTER TABLE comments ADD COLUMN parentCommentId TEXT
```

The updated `AppDatabase.kt` now includes:
- Better migration handling with fallback to destructive migration
- Column existence checking before adding new columns
- Improved error handling

## Files Modified
- `AppDatabase.kt` - Updated migration logic
- `DatabaseMigrationHelper.kt` - New utility for database fixes
- `database_reset.sh` - Script to reset app data

## Prevention
This issue should not occur in future updates as the migration logic has been improved to handle edge cases better.
