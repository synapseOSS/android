# Recovery Instructions for Deleted Layout Files

**Deletion Date:** January 1, 2026 at 20:49:02
**Backup Location:** `.backup/unused_layouts_20260101_204902/`

## Deleted Files:
1. `item_edit_history.xml` - Used by EditHistoryDialog (in backup)
2. `view_audio_player.xml` - Audio player component (unused)
3. `activity_media_preview.xml` - Media preview activity (unused)
4. `view_video_player.xml` - Video player component (unused)

## To Recover:
```bash
# Restore all files
cp .backup/unused_layouts_20260101_204902/*.xml app/src/main/res/layout/

# Or restore individual files
cp .backup/unused_layouts_20260101_204902/item_edit_history.xml app/src/main/res/layout/
cp .backup/unused_layouts_20260101_204902/view_audio_player.xml app/src/main/res/layout/
cp .backup/unused_layouts_20260101_204902/activity_media_preview.xml app/src/main/res/layout/
cp .backup/unused_layouts_20260101_204902/view_video_player.xml app/src/main/res/layout/
```

## Note:
If you restore these files, you may also need to restore their associated classes from other backup directories if they were previously moved.
