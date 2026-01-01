#!/bin/bash

# Comprehensive script to find truly unused XML layout files
# Checks: R.layout references, data binding classes, XML includes, string references

echo "=== COMPREHENSIVE UNUSED LAYOUT ANALYSIS ==="
echo "Analyzing all layout files for true usage..."
echo

# Get all layout files
LAYOUT_DIR="app/src/main/res/layout"
JAVA_DIR="app/src/main/java"
RES_DIR="app/src/main/res"

if [ ! -d "$LAYOUT_DIR" ]; then
    echo "Error: Layout directory not found: $LAYOUT_DIR"
    exit 1
fi

# Create arrays to store results
declare -a UNUSED_LAYOUTS=()
declare -a POTENTIALLY_UNUSED=()

echo "Found layout files:"
find "$LAYOUT_DIR" -name "*.xml" | sort

echo
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
        
        # 1. Check for R.layout references in Java/Kotlin files
        if find "$JAVA_DIR" -name "*.java" -o -name "*.kt" | xargs grep -l "R\.layout\.$layout_name" 2>/dev/null | head -1 >/dev/null; then
            echo "  ✓ Found R.layout.$layout_name reference"
            r_layout_found=true
        fi
        
        # 2. Check for data binding class imports (LayoutnameBinding)
        # Convert layout name to binding class name (snake_case to PascalCase + Binding)
        binding_class=$(echo "$layout_name" | sed 's/_\([a-z]\)/\U\1/g' | sed 's/^./\U&/')Binding
        if find "$JAVA_DIR" -name "*.java" -o -name "*.kt" | xargs grep -l "$binding_class" 2>/dev/null | head -1 >/dev/null; then
            echo "  ✓ Found data binding class: $binding_class"
            binding_class_found=true
        fi
        
        # 3. Check for XML includes in other layout files
        if find "$RES_DIR" -name "*.xml" | xargs grep -l "layout=\"@layout/$layout_name\"" 2>/dev/null | head -1 >/dev/null; then
            echo "  ✓ Found XML include reference"
            xml_include_found=true
        fi
        
        # 4. Check for string references to layout name
        if find "$JAVA_DIR" -name "*.java" -o -name "*.kt" | xargs grep -l "\"$layout_name\"" 2>/dev/null | head -1 >/dev/null; then
            echo "  ✓ Found string reference to layout name"
            string_ref_found=true
        fi
        
        # 5. Check for reflection usage (getString, getIdentifier)
        if find "$JAVA_DIR" -name "*.java" -o -name "*.kt" | xargs grep -l "getIdentifier.*$layout_name\|getString.*$layout_name" 2>/dev/null | head -1 >/dev/null; then
            echo "  ✓ Found reflection usage"
            string_ref_found=true
        fi
        
        # Determine if layout is truly unused
        if [ "$r_layout_found" = false ] && [ "$binding_class_found" = false ] && [ "$xml_include_found" = false ] && [ "$string_ref_found" = false ]; then
            echo "  ❌ TRULY UNUSED - No references found"
            UNUSED_LAYOUTS+=("$layout_name")
        elif [ "$r_layout_found" = false ] && [ "$binding_class_found" = false ]; then
            echo "  ⚠️  POTENTIALLY UNUSED - Only indirect references"
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

echo "=== DETAILED VERIFICATION ==="
echo "Performing additional checks on suspicious files..."
echo

# Additional verification for truly unused files
for layout_name in "${UNUSED_LAYOUTS[@]}"; do
    echo "Verifying $layout_name.xml:"
    
    # Check if it's a test file
    if [[ "$layout_name" == *"test"* ]] || [[ "$layout_name" == *"debug"* ]]; then
        echo "  📝 Appears to be a test/debug file"
    fi
    
    # Check if it's from old features (common patterns)
    if [[ "$layout_name" == *"old"* ]] || [[ "$layout_name" == *"deprecated"* ]] || [[ "$layout_name" == *"unused"* ]]; then
        echo "  📝 Appears to be from deprecated features"
    fi
    
    # Check file modification date
    mod_date=$(stat -c %y "$LAYOUT_DIR/$layout_name.xml" 2>/dev/null | cut -d' ' -f1)
    echo "  📅 Last modified: $mod_date"
    
    # Check file size
    file_size=$(stat -c %s "$LAYOUT_DIR/$layout_name.xml" 2>/dev/null)
    echo "  📏 File size: $file_size bytes"
    
    echo
done

echo "=== RECOMMENDATIONS ==="
echo

if [ ${#UNUSED_LAYOUTS[@]} -gt 0 ]; then
    echo "🎯 SAFE TO DELETE:"
    echo "The following files can be safely removed as they have no references:"
    for layout in "${UNUSED_LAYOUTS[@]}"; do
        echo "  rm app/src/main/res/layout/$layout.xml"
    done
    echo
    
    echo "To backup before deletion:"
    echo "mkdir -p backup_unused_layouts_$(date +%Y%m%d_%H%M%S)"
    for layout in "${UNUSED_LAYOUTS[@]}"; do
        echo "cp app/src/main/res/layout/$layout.xml backup_unused_layouts_$(date +%Y%m%d_%H%M%S)/"
    done
    echo
fi

if [ ${#POTENTIALLY_UNUSED[@]} -gt 0 ]; then
    echo "⚠️  REVIEW REQUIRED:"
    echo "The following files need manual review:"
    for layout in "${POTENTIALLY_UNUSED[@]}"; do
        echo "  - $layout.xml (has indirect references)"
    done
fi

echo "Analysis complete!"