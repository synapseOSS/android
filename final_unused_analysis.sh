#!/bin/bash

# Comprehensive script to find truly unused XML layout files
# Checks: R.layout references, data binding classes, XML includes, XML attributes

echo "=== COMPREHENSIVE UNUSED LAYOUT ANALYSIS ==="
echo "Checking for R.layout references, data binding, XML includes, and XML attributes..."
echo

LAYOUT_DIR="app/src/main/res/layout"
JAVA_DIR="app/src/main/java"
RES_DIR="app/src/main/res"

declare -a UNUSED_LAYOUTS=()
declare -a USED_LAYOUTS=()

for layout_file in "$LAYOUT_DIR"/*.xml; do
    if [ -f "$layout_file" ]; then
        layout_name=$(basename "$layout_file" .xml)
        
        # Check for R.layout references
        r_layout_refs=$(grep -r "R\.layout\.$layout_name\b" "$JAVA_DIR" 2>/dev/null | wc -l)
        
        # Check for data binding class references
        binding_class=$(echo "$layout_name" | sed 's/_\([a-z]\)/\U\1/g' | sed 's/^./\U&/')Binding
        binding_refs=$(grep -r "$binding_class" "$JAVA_DIR" 2>/dev/null | wc -l)
        
        # Check for XML includes
        xml_includes=$(grep -r "layout=\"@layout/$layout_name\"" "$RES_DIR" 2>/dev/null | wc -l)
        
        # Check for XML attribute references (like controller_layout_id, etc.)
        xml_attr_refs=$(grep -r "@layout/$layout_name" "$RES_DIR" 2>/dev/null | wc -l)
        
        # Check for string references in code (for dynamic loading)
        string_refs=$(grep -r "\"$layout_name\"" "$JAVA_DIR" 2>/dev/null | wc -l)
        
        total_refs=$((r_layout_refs + binding_refs + xml_includes + xml_attr_refs + string_refs))
        
        if [ $total_refs -eq 0 ]; then
            echo "❌ UNUSED: $layout_name.xml (0 references)"
            UNUSED_LAYOUTS+=("$layout_name")
        else
            ref_details=""
            [ $r_layout_refs -gt 0 ] && ref_details="${ref_details}${r_layout_refs} R.layout + "
            [ $binding_refs -gt 0 ] && ref_details="${ref_details}${binding_refs} binding + "
            [ $xml_includes -gt 0 ] && ref_details="${ref_details}${xml_includes} includes + "
            [ $xml_attr_refs -gt 0 ] && ref_details="${ref_details}${xml_attr_refs} xml_attrs + "
            [ $string_refs -gt 0 ] && ref_details="${ref_details}${string_refs} strings + "
            ref_details=${ref_details%" + "}  # Remove trailing " + "
            
            echo "✅ USED: $layout_name.xml ($ref_details = $total_refs total)"
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
    echo "🔴 TRULY UNUSED LAYOUTS:"
    for layout in "${UNUSED_LAYOUTS[@]}"; do
        echo "  - $layout.xml"
    done
    echo
    
    echo "🎯 VERIFICATION - Double-checking suspicious files:"
    for layout in "${UNUSED_LAYOUTS[@]}"; do
        echo
        echo "Verifying $layout.xml:"
        
        # Check file size and modification date
        file_size=$(stat -c %s "$LAYOUT_DIR/$layout.xml" 2>/dev/null)
        mod_date=$(stat -c %y "$LAYOUT_DIR/$layout.xml" 2>/dev/null | cut -d' ' -f1)
        echo "  📏 Size: $file_size bytes, Modified: $mod_date"
        
        # Show first few lines to understand what it is
        echo "  📄 Content preview:"
        head -10 "$LAYOUT_DIR/$layout.xml" | sed 's/^/    /'
        
        # Final verification with all search types
        echo "  🔍 Final verification:"
        echo "    R.layout references:"
        grep -rn "R\.layout\.$layout\b" "$JAVA_DIR" 2>/dev/null | head -2 | sed 's/^/      /' || echo "      None found"
        
        binding_class=$(echo "$layout" | sed 's/_\([a-z]\)/\U\1/g' | sed 's/^./\U&/')Binding
        echo "    Binding class ($binding_class) references:"
        grep -rn "$binding_class" "$JAVA_DIR" 2>/dev/null | head -2 | sed 's/^/      /' || echo "      None found"
        
        echo "    XML includes:"
        grep -rn "layout=\"@layout/$layout\"" "$RES_DIR" 2>/dev/null | head -2 | sed 's/^/      /' || echo "      None found"
        
        echo "    XML attribute references:"
        grep -rn "@layout/$layout" "$RES_DIR" 2>/dev/null | head -2 | sed 's/^/      /' || echo "      None found"
        
        echo "    String references:"
        grep -rn "\"$layout\"" "$JAVA_DIR" 2>/dev/null | head -2 | sed 's/^/      /' || echo "      None found"
    done
    
    echo
    echo "📋 COMMANDS TO SAFELY REMOVE UNUSED FILES:"
    echo "# Create backup first:"
    backup_dir="backup_unused_$(date +%Y%m%d_%H%M%S)"
    echo "mkdir -p $backup_dir"
    for layout in "${UNUSED_LAYOUTS[@]}"; do
        echo "cp app/src/main/res/layout/$layout.xml $backup_dir/"
    done
    echo
    echo "# Then delete unused files:"
    for layout in "${UNUSED_LAYOUTS[@]}"; do
        echo "rm app/src/main/res/layout/$layout.xml"
    done
    echo
    echo "# To restore if needed:"
    echo "# cp $backup_dir/*.xml app/src/main/res/layout/"
    
else
    echo "✅ All layout files are being used!"
    echo "No unused layouts found. The project is well-maintained!"
fi

echo
echo "🎉 Analysis complete!"
echo "This analysis checked for:"
echo "  • Direct R.layout references in Java/Kotlin code"
echo "  • Data binding class usage"
echo "  • XML layout includes"
echo "  • XML attribute references (like controller_layout_id)"
echo "  • String-based dynamic references"