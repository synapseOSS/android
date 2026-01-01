#!/bin/bash

# More precise script to find truly unused XML layout files
# Avoids false positives by being more specific in searches

echo "=== PRECISE UNUSED LAYOUT ANALYSIS ==="
echo "Analyzing all layout files with precise matching..."
echo

LAYOUT_DIR="app/src/main/res/layout"
JAVA_DIR="app/src/main/java"
RES_DIR="app/src/main/res"

if [ ! -d "$LAYOUT_DIR" ]; then
    echo "Error: Layout directory not found: $LAYOUT_DIR"
    exit 1
fi

declare -a UNUSED_LAYOUTS=()
declare -a POTENTIALLY_UNUSED=()

echo "=== DETAILED ANALYSIS ==="
echo

for layout_file in "$LAYOUT_DIR"/*.xml; do
    if [ -f "$layout_file" ]; then
        layout_name=$(basename "$layout_file" .xml)
        echo "Analyzing: $layout_name"
        
        # Initialize usage flags
        r_layout_found=false
        binding_class_found=false
        xml_include_found=false
        string_ref_found=false
        
        # 1. Check for R.layout references (more precise)
        r_layout_count=$(find "$JAVA_DIR" -name "*.java" -o -name "*.kt" 2>/dev/null | xargs grep -c "R\.layout\.$layout_name\b" 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
        if [ "$r_layout_count" -gt 0 ]; then
            echo "  ✓ Found $r_layout_count R.layout.$layout_name references"
            r_layout_found=true
        else
            echo "  ❌ No R.layout.$layout_name references found"
        fi
        
        # 2. Check for data binding class imports (more precise)
        binding_class=$(echo "$layout_name" | sed 's/_\([a-z]\)/\U\1/g' | sed 's/^./\U&/')Binding
        binding_count=$(find "$JAVA_DIR" -name "*.java" -o -name "*.kt" 2>/dev/null | xargs grep -c "\b$binding_class\b" 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
        if [ "$binding_count" -gt 0 ]; then
            echo "  ✓ Found $binding_count data binding class references: $binding_class"
            binding_class_found=true
        else
            echo "  ❌ No data binding class references found: $binding_class"
        fi
        
        # 3. Check for XML includes (more precise)
        xml_include_count=$(find "$RES_DIR" -name "*.xml" 2>/dev/null | xargs grep -c "layout=\"@layout/$layout_name\"" 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
        if [ "$xml_include_count" -gt 0 ]; then
            echo "  ✓ Found $xml_include_count XML include references"
            xml_include_found=true
        else
            echo "  ❌ No XML include references found"
        fi
        
        # 4. Check for string references (more precise - avoid common words)
        if [[ ${#layout_name} -gt 5 ]]; then  # Only check for longer, more specific names
            string_count=$(find "$JAVA_DIR" -name "*.java" -o -name "*.kt" 2>/dev/null | xargs grep -c "\"$layout_name\"" 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
            if [ "$string_count" -gt 0 ]; then
                echo "  ✓ Found $string_count string references"
                string_ref_found=true
            else
                echo "  ❌ No string references found"
            fi
        else
            echo "  ⚠️  Skipping string check (name too short/common)"
        fi
        
        # Determine if layout is truly unused
        if [ "$r_layout_found" = false ] && [ "$binding_class_found" = false ] && [ "$xml_include_found" = false ] && [ "$string_ref_found" = false ]; then
            echo "  🔴 TRULY UNUSED - No references found"
            UNUSED_LAYOUTS+=("$layout_name")
        elif [ "$r_layout_found" = false ] && [ "$binding_class_found" = false ]; then
            echo "  🟡 POTENTIALLY UNUSED - Only indirect references"
            POTENTIALLY_UNUSED+=("$layout_name")
        else
            echo "  ✅ USED - Direct references found"
        fi
        
        echo
    fi
done

echo "=== SUMMARY ==="
echo

if [ ${#UNUSED_LAYOUTS[@]} -gt 0 ]; then
    echo "🔴 TRULY UNUSED LAYOUTS (${#UNUSED_LAYOUTS[@]} files):"
    echo "These files have NO references and can be safely deleted:"
    for layout in "${UNUSED_LAYOUTS[@]}"; do
        echo "  - $layout.xml"
    done
    echo
else
    echo "✅ No truly unused layouts found."
    echo
fi

if [ ${#POTENTIALLY_UNUSED[@]} -gt 0 ]; then
    echo "🟡 POTENTIALLY UNUSED LAYOUTS (${#POTENTIALLY_UNUSED[@]} files):"
    echo "These files have only indirect references - review carefully:"
    for layout in "${POTENTIALLY_UNUSED[@]}"; do
        echo "  - $layout.xml"
    done
    echo
fi

# Manual verification for suspicious files
if [ ${#UNUSED_LAYOUTS[@]} -gt 0 ] || [ ${#POTENTIALLY_UNUSED[@]} -gt 0 ]; then
    echo "=== MANUAL VERIFICATION ==="
    echo "Let's manually check some specific files..."
    echo
    
    # Check a few files manually to verify
    all_suspicious=("${UNUSED_LAYOUTS[@]}" "${POTENTIALLY_UNUSED[@]}")
    for layout_name in "${all_suspicious[@]:0:5}"; do  # Check first 5
        echo "Manual check for $layout_name:"
        
        # Show actual grep results
        echo "  R.layout references:"
        find "$JAVA_DIR" -name "*.java" -o -name "*.kt" 2>/dev/null | xargs grep -n "R\.layout\.$layout_name\b" 2>/dev/null | head -3
        
        echo "  Binding class references:"
        binding_class=$(echo "$layout_name" | sed 's/_\([a-z]\)/\U\1/g' | sed 's/^./\U&/')Binding
        find "$JAVA_DIR" -name "*.java" -o -name "*.kt" 2>/dev/null | xargs grep -n "\b$binding_class\b" 2>/dev/null | head -3
        
        echo "  XML includes:"
        find "$RES_DIR" -name "*.xml" 2>/dev/null | xargs grep -n "layout=\"@layout/$layout_name\"" 2>/dev/null | head -3
        
        echo
    done
fi

echo "Analysis complete!"