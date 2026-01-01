#!/bin/bash

# Comprehensive layout usage verification
echo "=== COMPREHENSIVE LAYOUT VERIFICATION ==="

unused_layouts=(
    "activity_media_preview.xml"
    "bottom_sheet_comment_options.xml"
    "bottom_sheet_media_picker.xml"
    "bottom_sheet_message_actions.xml"
    "create_post_settings_bottom_sheet.xml"
    "dialog_ai_summary.xml"
    "dialog_delete_message.xml"
    "dialog_edit_history.xml"
    "dialog_edit_message.xml"
    "dialog_forward_message.xml"
    "fragment_inbox_stories.xml"
    "item_forward_conversation.xml"
    "item_loading_state.xml"
    "item_media_picker_option.xml"
    "item_message_action.xml"
    "item_nested_reply.xml"
    "item_post_detail_caption.xml"
    "item_post_detail_image.xml"
    "item_post_detail_video.xml"
    "user_followers_list.xml"
    "view_audio_player.xml"
    "view_video_player.xml"
)

truly_unused=()

for layout in "${unused_layouts[@]}"; do
    layout_name_no_ext="${layout%.xml}"
    echo "Deep checking: $layout"
    
    # Check all possible references
    found=false
    
    # 1. Check in all source files (including tests)
    if grep -r --include="*.java" --include="*.kt" --include="*.xml" --include="*.gradle" \
       -e "$layout_name_no_ext" \
       -e "R.layout.$layout_name_no_ext" \
       -e "@layout/$layout_name_no_ext" \
       -e "\"$layout_name_no_ext\"" \
       -e "'$layout_name_no_ext'" \
       . 2>/dev/null | grep -v "app/src/main/res/layout/$layout" | head -1; then
        echo "  ✓ Found reference"
        found=true
    fi
    
    # 2. Check for string-based references (reflection, dynamic loading)
    if ! $found; then
        if grep -r --include="*.java" --include="*.kt" \
           -e "\"$layout_name_no_ext\"" \
           -e "'$layout_name_no_ext'" \
           -e "getIdentifier.*$layout_name_no_ext" \
           app/src/ 2>/dev/null; then
            echo "  ✓ Found string/reflection reference"
            found=true
        fi
    fi
    
    # 3. Check for partial name matches (in case of dynamic construction)
    if ! $found; then
        base_name=$(echo "$layout_name_no_ext" | sed 's/_.*$//')
        if [ ${#base_name} -gt 3 ] && grep -r --include="*.java" --include="*.kt" "$base_name" app/src/ 2>/dev/null | grep -i layout | head -1; then
            echo "  ? Possible dynamic reference found for base name: $base_name"
            # Don't mark as found, but note it
        fi
    fi
    
    if ! $found; then
        echo "  ✗ CONFIRMED UNUSED"
        truly_unused+=("$layout")
    fi
    
    echo ""
done

echo "=== FINAL RESULTS ==="
echo "Initially found unused: ${#unused_layouts[@]}"
echo "Confirmed truly unused: ${#truly_unused[@]}"
echo ""

if [ ${#truly_unused[@]} -gt 0 ]; then
    echo "TRULY UNUSED LAYOUTS (safe to delete):"
    for layout in "${truly_unused[@]}"; do
        echo "  - $layout"
    done
fi