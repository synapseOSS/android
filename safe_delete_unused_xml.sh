#!/bin/bash

echo "=== SAFE DELETION OF UNUSED XML FILES ==="

# List of unused files to delete (verified as truly unused)
unused_files=(
    "app/src/main/res/layout/activity_media_preview.xml"
    "app/src/main/res/layout/bottom_sheet_comment_options.xml"
    "app/src/main/res/layout/bottom_sheet_media_picker.xml"
    "app/src/main/res/layout/bottom_sheet_message_actions.xml"
    "app/src/main/res/layout/dialog_ai_summary.xml"
    "app/src/main/res/layout/dialog_delete_message.xml"
    "app/src/main/res/layout/dialog_edit_history.xml"
    "app/src/main/res/layout/dialog_edit_message.xml"
    "app/src/main/res/layout/item_loading_state.xml"
    "app/src/main/res/layout/item_media_picker_option.xml"
    "app/src/main/res/layout/item_message_action.xml"
    "app/src/main/res/layout/item_nested_reply.xml"
    "app/src/main/res/layout/item_post_detail_caption.xml"
    "app/src/main/res/layout/item_post_detail_image.xml"
    "app/src/main/res/layout/item_post_detail_video.xml"
    "app/src/main/res/layout/view_audio_player.xml"
    "app/src/main/res/layout/view_video_player.xml"
    "app/src/main/res/drawable/circular_background_black_alpha.xml"
    "app/src/main/res/drawable/ic_add_photo_black.xml"
    "app/src/main/res/drawable/ic_category.xml"
    "app/src/main/res/drawable/ic_check_black.xml"
    "app/src/main/res/drawable/ic_document.xml"
    "app/src/main/res/drawable/ic_settings_black.xml"
    "app/src/main/res/drawable/page_indicator_dot.xml"
    "app/src/main/res/drawable/shape_error_message.xml"
    "app/src/main/res/drawable/shape_incoming_message_first.xml"
    "app/src/main/res/drawable/shape_incoming_message_last.xml"
    "app/src/main/res/drawable/shape_incoming_message_middle.xml"
    "app/src/main/res/drawable/shape_incoming_message_single.xml"
    "app/src/main/res/drawable/shape_outgoing_message_first.xml"
    "app/src/main/res/drawable/shape_outgoing_message_last.xml"
    "app/src/main/res/drawable/shape_outgoing_message_middle.xml"
    "app/src/main/res/drawable/shape_outgoing_message_single.xml"
    "app/src/main/res/drawable/switch_in.xml"
    "app/src/main/res/drawable/switch_out.xml"
    "app/src/main/AndroidManifest_nav3.xml"
)

# Create backup directory with timestamp
backup_dir="backup_deleted_xml_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$backup_dir"

# Function to test build
test_build() {
    echo "Testing build..."
    ./gradlew assembleDebug --quiet --no-daemon 2>/dev/null
    return $?
}

# Function to backup and delete file
delete_file_safely() {
    local file="$1"
    local filename=$(basename "$file")
    
    if [ ! -f "$file" ]; then
        echo "  ⚠️  File not found: $file"
        return 1
    fi
    
    # Create backup
    cp "$file" "$backup_dir/"
    echo "  📁 Backed up to: $backup_dir/$filename"
    
    # Delete the file
    rm "$file"
    echo "  🗑️  Deleted: $file"
    
    return 0
}

# Function to restore file if build fails
restore_file() {
    local file="$1"
    local filename=$(basename "$file")
    
    if [ -f "$backup_dir/$filename" ]; then
        cp "$backup_dir/$filename" "$file"
        echo "  🔄 Restored: $file"
        return 0
    else
        echo "  ❌ Cannot restore: backup not found for $file"
        return 1
    fi
}

echo "Starting safe deletion process..."
echo "Total files to delete: ${#unused_files[@]}"
echo "Backup directory: $backup_dir"
echo ""

# Initial build test
echo "=== INITIAL BUILD TEST ==="
if ! test_build; then
    echo "❌ Initial build failed! Please fix build issues before proceeding."
    exit 1
fi
echo "✅ Initial build successful"
echo ""

deleted_count=0
failed_count=0

# Process each file
for i in "${!unused_files[@]}"; do
    file="${unused_files[$i]}"
    file_num=$((i + 1))
    
    echo "=== PROCESSING FILE $file_num/${#unused_files[@]} ==="
    echo "File: $file"
    
    # Check if file exists
    if [ ! -f "$file" ]; then
        echo "  ⚠️  File already deleted or not found, skipping..."
        echo ""
        continue
    fi
    
    # Show file size
    size=$(stat -c%s "$file" 2>/dev/null || echo "unknown")
    echo "  Size: $size bytes"
    
    # Delete the file
    if delete_file_safely "$file"; then
        echo "  🔄 Testing build after deletion..."
        
        if test_build; then
            echo "  ✅ Build successful after deleting $file"
            deleted_count=$((deleted_count + 1))
        else
            echo "  ❌ Build failed after deleting $file"
            echo "  🔄 Restoring file..."
            
            if restore_file "$file"; then
                echo "  🔄 Re-testing build after restore..."
                if test_build; then
                    echo "  ✅ Build restored successfully"
                else
                    echo "  ❌ Build still failing after restore - manual intervention needed!"
                    exit 1
                fi
            else
                echo "  ❌ Failed to restore file - manual intervention needed!"
                exit 1
            fi
            
            failed_count=$((failed_count + 1))
        fi
    else
        echo "  ❌ Failed to delete file"
        failed_count=$((failed_count + 1))
    fi
    
    echo ""
    
    # Add a small delay to avoid overwhelming the system
    sleep 1
done

echo "=== DELETION SUMMARY ==="
echo "Total files processed: ${#unused_files[@]}"
echo "Successfully deleted: $deleted_count"
echo "Failed to delete: $failed_count"
echo "Backup directory: $backup_dir"

if [ $deleted_count -gt 0 ]; then
    echo ""
    echo "✅ Successfully deleted $deleted_count unused XML files!"
    echo "📁 All deleted files are backed up in: $backup_dir"
    
    # Calculate space saved
    total_size=0
    for file in "$backup_dir"/*; do
        if [ -f "$file" ]; then
            size=$(stat -c%s "$file" 2>/dev/null || echo "0")
            total_size=$((total_size + size))
        fi
    done
    
    if [ $total_size -gt 0 ]; then
        echo "💾 Total space saved: $total_size bytes"
    fi
else
    echo ""
    echo "⚠️  No files were successfully deleted."
fi

if [ $failed_count -gt 0 ]; then
    echo ""
    echo "⚠️  $failed_count files could not be deleted (likely still in use)."
fi

echo ""
echo "=== FINAL BUILD TEST ==="
if test_build; then
    echo "✅ Final build test successful - all changes are safe!"
else
    echo "❌ Final build test failed - please check for issues!"
    exit 1
fi