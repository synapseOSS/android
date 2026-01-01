#!/bin/bash

LAYOUT_DIR="app/src/main/res/layout"
UNUSED_LAYOUTS=()

echo "Scanning for unused layout files..."

for layout_file in "$LAYOUT_DIR"/*.xml; do
    if [ -f "$layout_file" ]; then
        filename=$(basename "$layout_file" .xml)
        
        # Search for references in Java/Kotlin files (R.layout.filename)
        java_refs=$(find app/src -name "*.java" -o -name "*.kt" | xargs grep -l "R\.layout\.$filename" 2>/dev/null | wc -l)
        
        # Search for references in XML files (@layout/filename)
        xml_refs=$(find app/src -name "*.xml" | xargs grep -l "@layout/$filename" 2>/dev/null | wc -l)
        
        # Search for references in XML files (layout="@layout/filename")
        xml_layout_refs=$(find app/src -name "*.xml" | xargs grep -l "layout=\"@layout/$filename\"" 2>/dev/null | wc -l)
        
        # Search for string references in any file
        string_refs=$(find app/src -type f | xargs grep -l "$filename" 2>/dev/null | grep -v "$layout_file" | wc -l)
        
        total_refs=$((java_refs + xml_refs + xml_layout_refs))
        
        if [ $total_refs -eq 0 ] && [ $string_refs -eq 0 ]; then
            UNUSED_LAYOUTS+=("$filename")
            echo "UNUSED: $filename"
        else
            echo "USED: $filename (refs: $total_refs, string_refs: $string_refs)"
        fi
    fi
done

echo ""
echo "Summary of unused layouts:"
for layout in "${UNUSED_LAYOUTS[@]}"; do
    echo "- $layout.xml"
done

echo ""
echo "Total unused layouts found: ${#UNUSED_LAYOUTS[@]}"