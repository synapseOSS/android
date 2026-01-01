#!/bin/bash

echo "=== COMPREHENSIVE UNUSED XML FILE SEARCH ==="

unused_files=()

# Add the already found file
unused_files+=("app/src/main/AndroidManifest_nav3.xml")

# Function to check if a drawable is truly unused
check_drawable_usage() {
    local file="$1"
    local resource_name="$2"
    
    # Check for direct R.drawable references
    if grep -r --include="*.java" --include="*.kt" "R\.drawable\.$resource_name" app/src/main/java/ 2>/dev/null | head -1; then
        return 0  # Found
    fi
    
    # Check for @drawable references in XML
    if find app/src/main/res -name "*.xml" -not -path "*/$file" -exec grep -l "@drawable/$resource_name" {} \; 2>/dev/null | head -1; then
        return 0  # Found
    fi
    
    # Check for ?drawable references
    if find app/src/main/res -name "*.xml" -not -path "*/$file" -exec grep -l "?drawable/$resource_name" {} \; 2>/dev/null | head -1; then
        return 0  # Found
    fi
    
    # Check for string references (sometimes drawables are referenced by name)
    if grep -r --include="*.java" --include="*.kt" "\"$resource_name\"" app/src/main/java/ 2>/dev/null | head -1; then
        return 0  # Found
    fi
    
    return 1  # Not found
}

# Check some specific drawable files that might be unused
echo "=== CHECKING SPECIFIC DRAWABLE FILES ==="

# Check for potentially unused icon files
potential_unused_drawables=(
    "ic_category.xml"
    "ic_heart_outline.xml" 
    "ic_play_arrow.xml"
    "ic_rewind.xml"
    "ic_settings_black.xml"
    "icon_comment_24px.xml"
    "icon_favorite_24px.xml"
    "icon_play_circle_24px.xml"
    "icon_post_24px.xml"
    "story_ring.xml"
    "switch_in.xml"
    "switch_out.xml"
)

for drawable in "${potential_unused_drawables[@]}"; do
    file_path="app/src/main/res/drawable/$drawable"
    if [ -f "$file_path" ]; then
        resource_name="${drawable%.xml}"
        echo "Checking: $file_path"
        
        if ! check_drawable_usage "$file_path" "$resource_name"; then
            echo "  ✗ NO REFERENCES FOUND"
            unused_files+=("$file_path")
        else
            echo "  ✓ Found references"
        fi
        echo ""
    fi
done

# Check for unused animation files
echo "=== DOUBLE-CHECKING ANIMATION FILES ==="
for anim_file in app/src/main/res/anim/*.xml; do
    if [ -f "$anim_file" ]; then
        filename=$(basename "$anim_file")
        resource_name="${filename%.xml}"
        
        # More thorough check for animations
        found=false
        
        # Check for R.anim references
        if grep -r --include="*.java" --include="*.kt" "R\.anim\.$resource_name" app/src/main/java/ 2>/dev/null | head -1; then
            found=true
        fi
        
        # Check for @anim references in XML
        if ! $found && find app/src/main/res -name "*.xml" -exec grep -l "@anim/$resource_name" {} \; 2>/dev/null | head -1; then
            found=true
        fi
        
        # Check for animation loading by name
        if ! $found && grep -r --include="*.java" --include="*.kt" "\"$resource_name\"" app/src/main/java/ 2>/dev/null | head -1; then
            found=true
        fi
        
        if ! $found; then
            echo "Potentially unused animation: $anim_file"
            # Let's be extra careful with animations and do one more check
            if ! grep -r --include="*.java" --include="*.kt" --include="*.xml" "$resource_name" app/src/main/ 2>/dev/null | grep -v "$anim_file" | head -1; then
                echo "  ✗ CONFIRMED: NO REFERENCES FOUND"
                unused_files+=("$anim_file")
            fi
        fi
    fi
done

# Check for unused mipmap XML files
echo "=== CHECKING MIPMAP XML FILES ==="
for mipmap_file in app/src/main/res/mipmap-*/*.xml; do
    if [ -f "$mipmap_file" ]; then
        filename=$(basename "$mipmap_file")
        resource_name="${filename%.xml}"
        
        echo "Checking: $mipmap_file"
        
        # Check for mipmap references
        if ! grep -r --include="*.java" --include="*.kt" --include="*.xml" \
             -e "R\.mipmap\.$resource_name" \
             -e "@mipmap/$resource_name" \
             app/src/main/ 2>/dev/null | head -1; then
            echo "  ✗ NO REFERENCES FOUND"
            unused_files+=("$mipmap_file")
        else
            echo "  ✓ Found references"
        fi
        echo ""
    fi
done

echo "=== FINAL COMPREHENSIVE RESULTS ==="
echo "Total unused XML files found: ${#unused_files[@]}"

if [ ${#unused_files[@]} -gt 0 ]; then
    echo ""
    echo "ALL UNUSED XML FILES:"
    for file in "${unused_files[@]}"; do
        echo "  - $file"
    done
    
    echo ""
    echo "Next 5 files recommended for deletion:"
    for i in {0..4}; do
        if [ $i -lt ${#unused_files[@]} ]; then
            echo "  ${unused_files[$i]}"
        fi
    done
else
    echo ""
    echo "No unused XML files found."
fi
