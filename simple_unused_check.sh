#!/bin/bash

# Simple and reliable script to find unused XML layout files

echo "=== RELIABLE UNUSED LAYOUT ANALYSIS ==="
echo

LAYOUT_DIR="app/src/main/res/layout"
JAVA_DIR="app/src/main/java"

declare -a UNUSED_LAYOUTS=()
declare -a USED_LAYOUTS=()

echo "Checking each layout file for usage..."
echo

for layout_file in "$LAYOUT_DIR"/*.xml; do
    if [ -f "$layout_file" ]; then
        layout_name=$(basename "$layout_file" .xml)
        
        # Check for R.layout references
        r_layout_refs=$(grep -r "R\.layout\.$layout_name\b" "$JAVA_DIR" 2>/dev/null | wc -l)
        
        # Check for data binding class references
        binding_class=$(echo "$layout_name" | sed 's/_\([a-z]\)/\U\1/g' | sed 's/^./\U&/')Binding
        binding_refs=$(grep -r "$binding_class" "$JAVA_DIR" 2>/dev/null | wc -l)
        
        # Check for XML includes
        xml_includes=$(grep -r "layout=\"@layout/$layout_name\"" app/src/main/res/ 2>/dev/null | wc -l)
        
        total_refs=$((r_layout_refs + binding_refs + xml_includes))
        
        if [ $total_refs -eq 0 ]; then
            echo "❌ UNUSED: $layout_name.xml (0 references)"
            UNUSED_LAYOUTS+=("$layout_name")
        else
            echo "✅ USED: $layout_name.xml ($r_layout_refs R.layout + $binding_refs binding + $xml_includes includes = $total_refs total)"
            USED_LAYOUTS+=("$layout_name")
        fi
    fi
done

echo
echo "=== SUMMARY ==="
echo "Total layouts: $((${#USED_LAYOUTS[@]} + ${#UNUSED_LAYOUTS[@]}))"
echo "Used layouts: ${#USED_LAYOUTS[@]}"
echo "Unused layouts: ${#UNUSED_LAYOUTS[@]}"
echo

if [ ${#UNUSED_LAYOUTS[@]} -gt 0 ]; then
    echo "🔴 UNUSED LAYOUTS:"
    for layout in "${UNUSED_LAYOUTS[@]}"; do
        echo "  - $layout.xml"
    done
    echo
    
    echo "🎯 VERIFICATION - Let's double-check a few files:"
    for layout in "${UNUSED_LAYOUTS[@]:0:3}"; do
        echo
        echo "Checking $layout.xml:"
        echo "  R.layout references:"
        grep -rn "R\.layout\.$layout\b" "$JAVA_DIR" 2>/dev/null | head -2 || echo "    None found"
        
        binding_class=$(echo "$layout" | sed 's/_\([a-z]\)/\U\1/g' | sed 's/^./\U&/')Binding
        echo "  Binding class ($binding_class) references:"
        grep -rn "$binding_class" "$JAVA_DIR" 2>/dev/null | head -2 || echo "    None found"
        
        echo "  XML includes:"
        grep -rn "layout=\"@layout/$layout\"" app/src/main/res/ 2>/dev/null | head -2 || echo "    None found"
    done
    
    echo
    echo "📋 COMMANDS TO DELETE UNUSED FILES:"
    echo "# Backup first:"
    echo "mkdir -p backup_unused_$(date +%Y%m%d_%H%M%S)"
    for layout in "${UNUSED_LAYOUTS[@]}"; do
        echo "cp app/src/main/res/layout/$layout.xml backup_unused_$(date +%Y%m%d_%H%M%S)/"
    done
    echo
    echo "# Then delete:"
    for layout in "${UNUSED_LAYOUTS[@]}"; do
        echo "rm app/src/main/res/layout/$layout.xml"
    done
else
    echo "✅ All layout files are being used!"
fi

echo
echo "Analysis complete!"