#!/bin/bash

LAYOUT_DIR="app/src/main/res/layout"
TRULY_UNUSED=()

echo "Performing thorough scan for unused layout files..."

for layout_file in "$LAYOUT_DIR"/*.xml; do
    if [ -f "$layout_file" ]; then
        filename=$(basename "$layout_file" .xml)
        
        # Convert to CamelCase for binding class names
        binding_name=$(echo "$filename" | sed 's/_\([a-z]\)/\U\1/g' | sed 's/^./\U&/')Binding
        
        # Search patterns
        r_layout_refs=$(find app/src -name "*.java" -o -name "*.kt" | xargs grep -l "R\.layout\.$filename" 2>/dev/null | wc -l)
        xml_layout_refs=$(find app/src -name "*.xml" | xargs grep -l "@layout/$filename" 2>/dev/null | wc -l)
        binding_refs=$(find app/src -name "*.java" -o -name "*.kt" | xargs grep -l "$binding_name" 2>/dev/null | wc -l)
        include_refs=$(find app/src -name "*.xml" | xargs grep -l "layout=\"@layout/$filename\"" 2>/dev/null | wc -l)
        
        # Check for any string occurrence of the filename (excluding the file itself)
        string_refs=$(find app/src -type f \( -name "*.java" -o -name "*.kt" -o -name "*.xml" \) | xargs grep -l "$filename" 2>/dev/null | grep -v "$layout_file" | wc -l)
        
        total_refs=$((r_layout_refs + xml_layout_refs + binding_refs + include_refs))
        
        if [ $total_refs -eq 0 ] && [ $string_refs -eq 0 ]; then
            TRULY_UNUSED+=("$filename")
            echo "TRULY UNUSED: $filename (no references found)"
        else
            echo "USED: $filename (R.layout: $r_layout_refs, XML: $xml_layout_refs, Binding: $binding_refs, Include: $include_refs, String: $string_refs)"
        fi
    fi
done

echo ""
echo "=== TRULY UNUSED LAYOUTS (SAFE TO DELETE) ==="
count=0
for layout in "${TRULY_UNUSED[@]}"; do
    if [ $count -lt 5 ]; then
        echo "$((count + 1)). $layout.xml"
        count=$((count + 1))
    fi
done

echo ""
echo "Total truly unused layouts found: ${#TRULY_UNUSED[@]}"
echo "Showing first 5 that are safe to delete."