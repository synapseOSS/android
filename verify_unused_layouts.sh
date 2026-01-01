#!/bin/bash

# Script to verify unused layout files
echo "Verifying unused layout files..."

# Get all layout files
layout_files=($(find app/src/main/res/layout -name "*.xml" -exec basename {} \; | sort))

# Function to check if a layout is referenced
check_layout_usage() {
    local layout_name="$1"
    local layout_name_no_ext="${layout_name%.xml}"
    
    echo "Checking: $layout_name"
    
    # Search patterns
    local patterns=(
        "R\.layout\.$layout_name_no_ext"
        "@layout/$layout_name_no_ext"
        "\"$layout_name_no_ext\""
        "'$layout_name_no_ext'"
        "setContentView.*$layout_name_no_ext"
        "inflate.*$layout_name_no_ext"
        "DataBindingUtil.*$layout_name_no_ext"
    )
    
    local found=false
    
    # Search in Java/Kotlin files
    for pattern in "${patterns[@]}"; do
        if grep -r --include="*.java" --include="*.kt" "$pattern" app/src/ >/dev/null 2>&1; then
            echo "  ✓ Found reference: $pattern"
            found=true
            break
        fi
    done
    
    # Search in XML files (excluding the layout file itself)
    if ! $found; then
        if grep -r --include="*.xml" --exclude="$layout_name" "@layout/$layout_name_no_ext" app/src/ >/dev/null 2>&1; then
            echo "  ✓ Found XML reference"
            found=true
        fi
    fi
    
    if ! $found; then
        echo "  ✗ NO REFERENCES FOUND"
        return 1
    fi
    
    return 0
}

# Check each layout file
unused_layouts=()
for layout in "${layout_files[@]}"; do
    if ! check_layout_usage "$layout"; then
        unused_layouts+=("$layout")
    fi
    echo ""
done

echo "=== SUMMARY ==="
echo "Total layout files: ${#layout_files[@]}"
echo "Unused layout files: ${#unused_layouts[@]}"

if [ ${#unused_layouts[@]} -gt 0 ]; then
    echo ""
    echo "Unused layouts:"
    for layout in "${unused_layouts[@]}"; do
        echo "  - $layout"
    done
fi