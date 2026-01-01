#!/bin/bash

echo "=== SEARCHING FOR ADDITIONAL UNUSED FILES ==="

unused_files=()
unused_files+=("app/src/main/AndroidManifest_nav3.xml")

# Check for any XML files that might be duplicates or alternatives
echo "=== CHECKING FOR DUPLICATE OR ALTERNATIVE XML FILES ==="

# Look for files with similar names that might be alternatives
declare -A base_names
for file in app/src/main/res/drawable/*.xml app/src/main/res/layout/*.xml app/src/main/res/anim/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file" .xml)
        
        # Remove common suffixes to find potential duplicates
        base_name=$(echo "$filename" | sed -E 's/_(v[0-9]+|old|new|alt|backup|copy|[0-9]+)$//')
        
        if [ -n "${base_names[$base_name]}" ]; then
            echo "Potential duplicate found:"
            echo "  Base: ${base_names[$base_name]}"
            echo "  Alternative: $file"
            
            # Check which one is actually used
            filename1=$(basename "${base_names[$base_name]}" .xml)
            filename2=$(basename "$file" .xml)
            
            # Count references for each
            refs1=$(grep -r --include="*.java" --include="*.kt" --include="*.xml" "$filename1" app/src/main/ 2>/dev/null | wc -l)
            refs2=$(grep -r --include="*.java" --include="*.kt" --include="*.xml" "$filename2" app/src/main/ 2>/dev/null | wc -l)
            
            echo "  References to $filename1: $refs1"
            echo "  References to $filename2: $refs2"
            
            if [ "$refs1" -eq 0 ] && [ "$refs2" -gt 0 ]; then
                echo "  -> ${base_names[$base_name]} appears unused"
                unused_files+=("${base_names[$base_name]}")
            elif [ "$refs2" -eq 0 ] && [ "$refs1" -gt 0 ]; then
                echo "  -> $file appears unused"
                unused_files+=("$file")
            fi
            echo ""
        else
            base_names[$base_name]="$file"
        fi
    fi
done

# Check for any XML files that might be referenced only in build files (which might indicate they're for build variants)
echo "=== CHECKING FOR BUILD-VARIANT-ONLY XML FILES ==="
for file in app/src/main/res/drawable/*.xml app/src/main/res/values*/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        # Check if only referenced in build files
        source_refs=$(grep -r --include="*.java" --include="*.kt" "$resource_name" app/src/main/java/ 2>/dev/null | wc -l)
        xml_refs=$(find app/src/main/res -name "*.xml" -not -path "*/$file" -exec grep -l "$resource_name" {} \; 2>/dev/null | wc -l)
        build_refs=$(grep -r --include="*.gradle" "$resource_name" . 2>/dev/null | wc -l)
        
        if [ "$source_refs" -eq 0 ] && [ "$xml_refs" -eq 0 ] && [ "$build_refs" -gt 0 ]; then
            echo "File only referenced in build files: $file"
            unused_files+=("$file")
        fi
    fi
done

# Check for any XML files with very specific patterns that might be unused
echo "=== CHECKING FOR SPECIFIC PATTERN FILES ==="

# Look for files that might be placeholders or examples
for pattern in "placeholder" "example" "sample" "default" "fallback"; do
    for file in app/src/main/res/drawable/*${pattern}*.xml app/src/main/res/layout/*${pattern}*.xml; do
        if [ -f "$file" ]; then
            filename=$(basename "$file")
            resource_name="${filename%.xml}"
            
            echo "Checking pattern file: $file"
            
            # More thorough check for these files
            if ! grep -r --include="*.java" --include="*.kt" --include="*.xml" \
                 -e "R\.[^.]*\.$resource_name" \
                 -e "@[^/]*/$resource_name" \
                 -e "\"$resource_name\"" \
                 app/src/main/ 2>/dev/null | grep -v "$file" | head -1; then
                echo "  ✗ NO REFERENCES FOUND"
                unused_files+=("$file")
            else
                echo "  ✓ Found references"
            fi
            echo ""
        fi
    done
done

# Remove duplicates from unused_files array
declare -A seen
unique_unused=()
for file in "${unused_files[@]}"; do
    if [ -z "${seen[$file]}" ]; then
        seen[$file]=1
        unique_unused+=("$file")
    fi
done

echo "=== FINAL RESULTS ==="
echo "Total unique unused XML files found: ${#unique_unused[@]}"

if [ ${#unique_unused[@]} -gt 0 ]; then
    echo ""
    echo "ALL UNUSED XML FILES:"
    for file in "${unique_unused[@]}"; do
        echo "  - $file"
    done
    
    echo ""
    echo "Next 5 files recommended for deletion:"
    for i in {0..4}; do
        if [ $i -lt ${#unique_unused[@]} ]; then
            echo "  ${unique_unused[$i]}"
        fi
    done
else
    echo ""
    echo "No unused XML files found."
fi
