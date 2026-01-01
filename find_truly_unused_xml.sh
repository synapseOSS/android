#!/bin/bash

echo "=== COMPREHENSIVE UNUSED XML FILE DETECTION ==="

# Function to check if a resource is referenced
check_resource_usage() {
    local file_path="$1"
    local resource_name="$2"
    local resource_type="$3"
    
    # Check Java/Kotlin files
    java_refs=$(grep -r --include="*.java" --include="*.kt" \
               -e "R\.$resource_type\.$resource_name" \
               -e "@$resource_type/$resource_name" \
               app/src/main/java/ 2>/dev/null | wc -l)
    
    # Check XML files (excluding the file itself)
    xml_refs=$(find app/src/main/res -name "*.xml" -not -path "*/$file_path" \
              -exec grep -l -E "@$resource_type/$resource_name|@\+id/$resource_name" {} \; 2>/dev/null | wc -l)
    
    # Check for string references in XML
    string_refs=$(find app/src/main/res -name "*.xml" -not -path "*/$file_path" \
                 -exec grep -l "\"$resource_name\"" {} \; 2>/dev/null | wc -l)
    
    # Check manifest files
    manifest_refs=$(find app/src/main -name "*.xml" -path "*/AndroidManifest*" \
                   -exec grep -l "$resource_name" {} \; 2>/dev/null | wc -l)
    
    total_refs=$((java_refs + xml_refs + string_refs + manifest_refs))
    
    echo "    Java/Kotlin: $java_refs, XML: $xml_refs, String: $string_refs, Manifest: $manifest_refs"
    return $total_refs
}

unused_files=()

echo ""
echo "=== CHECKING LAYOUT FILES ==="
for file in app/src/main/res/layout/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        echo "Checking layout: $filename"
        check_resource_usage "$file" "$resource_name" "layout"
        refs=$?
        
        if [ $refs -eq 0 ]; then
            echo "  ✗ UNUSED: $file"
            unused_files+=("$file")
        else
            echo "  ✓ Used ($refs references)"
        fi
        echo ""
    fi
done

echo "=== CHECKING DRAWABLE FILES ==="
for file in app/src/main/res/drawable/*.xml app/src/main/res/drawable-*/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        echo "Checking drawable: $filename"
        check_resource_usage "$file" "$resource_name" "drawable"
        refs=$?
        
        if [ $refs -eq 0 ]; then
            echo "  ✗ UNUSED: $file"
            unused_files+=("$file")
        else
            echo "  ✓ Used ($refs references)"
        fi
        echo ""
    fi
done

echo "=== CHECKING ANIMATION FILES ==="
for file in app/src/main/res/anim/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        echo "Checking animation: $filename"
        check_resource_usage "$file" "$resource_name" "anim"
        refs=$?
        
        if [ $refs -eq 0 ]; then
            echo "  ✗ UNUSED: $file"
            unused_files+=("$file")
        else
            echo "  ✓ Used ($refs references)"
        fi
        echo ""
    fi
done

echo "=== CHECKING MENU FILES ==="
for file in app/src/main/res/menu/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        echo "Checking menu: $filename"
        check_resource_usage "$file" "$resource_name" "menu"
        refs=$?
        
        if [ $refs -eq 0 ]; then
            echo "  ✗ UNUSED: $file"
            unused_files+=("$file")
        else
            echo "  ✓ Used ($refs references)"
        fi
        echo ""
    fi
done

echo "=== CHECKING COLOR FILES ==="
for file in app/src/main/res/color/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        echo "Checking color: $filename"
        check_resource_usage "$file" "$resource_name" "color"
        refs=$?
        
        if [ $refs -eq 0 ]; then
            echo "  ✗ UNUSED: $file"
            unused_files+=("$file")
        else
            echo "  ✓ Used ($refs references)"
        fi
        echo ""
    fi
done

echo "=== CHECKING OTHER XML FILES ==="
# Check for other XML files like AndroidManifest variants
for file in app/src/main/AndroidManifest*.xml; do
    if [ -f "$file" ] && [ "$file" != "app/src/main/AndroidManifest.xml" ]; then
        filename=$(basename "$file")
        
        echo "Checking manifest variant: $filename"
        # Check if referenced in build files or other configs
        refs=$(grep -r "$filename" app/build.gradle gradle.properties settings.gradle 2>/dev/null | wc -l)
        
        if [ $refs -eq 0 ]; then
            echo "  ✗ UNUSED: $file"
            unused_files+=("$file")
        else
            echo "  ✓ Used ($refs references)"
        fi
        echo ""
    fi
done

echo "=== FINAL RESULTS ==="
echo "Total unused XML files found: ${#unused_files[@]}"

if [ ${#unused_files[@]} -gt 0 ]; then
    echo ""
    echo "UNUSED XML FILES:"
    for i in "${!unused_files[@]}"; do
        file="${unused_files[$i]}"
        size=$(stat -c%s "$file" 2>/dev/null || echo "unknown")
        echo "  $((i+1)). $file (${size} bytes)"
    done
    
    echo ""
    echo "READY FOR SAFE DELETION (first 5):"
    for i in {0..4}; do
        if [ $i -lt ${#unused_files[@]} ]; then
            echo "  ${unused_files[$i]}"
        fi
    done
else
    echo "No unused XML files found."
fi