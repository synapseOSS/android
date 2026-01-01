#!/bin/bash

echo "=== FINAL TARGETED SEARCH FOR UNUSED LAYOUTS ==="

# Get all layout files
layout_files=($(find app/src/main/res/layout -name "*.xml" -exec basename {} \; | sort))

truly_unused=()

for layout in "${layout_files[@]}"; do
    layout_name_no_ext="${layout%.xml}"
    echo "Checking: $layout"
    
    found=false
    
    # 1. Check in Java/Kotlin source files only (ignore build files)
    if grep -r --include="*.java" --include="*.kt" \
       -e "R\.layout\.$layout_name_no_ext" \
       -e "@layout/$layout_name_no_ext" \
       -e "setContentView.*$layout_name_no_ext" \
       -e "inflate.*$layout_name_no_ext" \
       -e "DataBindingUtil.*$layout_name_no_ext" \
       app/src/main/java/ 2>/dev/null | head -1; then
        echo "  ✓ Found in source code"
        found=true
    fi
    
    # 2. Check in XML files (excluding the layout file itself and build files)
    if ! $found; then
        if find app/src/main/res -name "*.xml" -not -path "*/layout/$layout" -exec grep -l "@layout/$layout_name_no_ext" {} \; 2>/dev/null | head -1; then
            echo "  ✓ Found in XML files"
            found=true
        fi
    fi
    
    # 3. Check for string references in source
    if ! $found; then
        if grep -r --include="*.java" --include="*.kt" \
           -e "\"$layout_name_no_ext\"" \
           -e "'$layout_name_no_ext'" \
           app/src/main/java/ 2>/dev/null | head -1; then
            echo "  ✓ Found string reference"
            found=true
        fi
    fi
    
    if ! $found; then
        echo "  ✗ NO SOURCE REFERENCES FOUND"
        truly_unused+=("$layout")
    fi
    
    echo ""
done

echo "=== RESULTS ==="
echo "Total layout files: ${#layout_files[@]}"
echo "Truly unused (no source references): ${#truly_unused[@]}"

if [ ${#truly_unused[@]} -gt 0 ]; then
    echo ""
    echo "LAYOUTS WITH NO SOURCE CODE REFERENCES:"
    for layout in "${truly_unused[@]}"; do
        echo "  - $layout"
    done
    
    if [ ${#truly_unused[@]} -ge 5 ]; then
        echo ""
        echo "Found ${#truly_unused[@]} unused layouts. Selecting first 5 for deletion:"
        for i in {0..4}; do
            if [ $i -lt ${#truly_unused[@]} ]; then
                echo "  ${truly_unused[$i]}"
            fi
        done
    fi
else
    echo ""
    echo "All layout files appear to be referenced in source code."
fi