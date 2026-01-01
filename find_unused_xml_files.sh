#!/bin/bash

echo "=== COMPREHENSIVE UNUSED XML FILE SEARCH ==="

# Function to check if a resource is referenced
check_resource_usage() {
    local file_path="$1"
    local resource_type="$2"
    local resource_name="$3"
    
    echo "Checking: $file_path"
    
    # Check in Java/Kotlin source files
    if grep -r --include="*.java" --include="*.kt" \
       -e "R\.$resource_type\.$resource_name" \
       -e "@$resource_type/$resource_name" \
       app/src/main/java/ 2>/dev/null | head -1; then
        echo "  ✓ Found in source code"
        return 0
    fi
    
    # Check in XML files (excluding the file itself)
    if find app/src/main/res -name "*.xml" -not -path "*/$file_path" -exec grep -l "@$resource_type/$resource_name\|?$resource_type/$resource_name" {} \; 2>/dev/null | head -1; then
        echo "  ✓ Found in XML files"
        return 0
    fi
    
    # Check for string references
    if grep -r --include="*.java" --include="*.kt" \
       -e "\"$resource_name\"" \
       -e "'$resource_name'" \
       app/src/main/java/ 2>/dev/null | head -1; then
        echo "  ✓ Found string reference"
        return 0
    fi
    
    echo "  ✗ NO REFERENCES FOUND"
    return 1
}

unused_files=()

# Check drawable XML files
echo "=== CHECKING DRAWABLE XML FILES ==="
for file in app/src/main/res/drawable/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        if ! check_resource_usage "$file" "drawable" "$resource_name"; then
            unused_files+=("$file")
        fi
        echo ""
    fi
done

# Check animation XML files
echo "=== CHECKING ANIMATION XML FILES ==="
for file in app/src/main/res/anim/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        if ! check_resource_usage "$file" "anim" "$resource_name"; then
            unused_files+=("$file")
        fi
        echo ""
    fi
done

# Check other XML files in values directories
echo "=== CHECKING VALUES XML FILES ==="
for file in app/src/main/res/values*/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        # Skip common files that are typically always used
        if [[ "$filename" != "strings.xml" && "$filename" != "colors.xml" && "$filename" != "themes.xml" && "$filename" != "styles.xml" ]]; then
            resource_name="${filename%.xml}"
            if ! check_resource_usage "$file" "values" "$resource_name"; then
                unused_files+=("$file")
            fi
            echo ""
        fi
    fi
done

# Check XML files in other directories
echo "=== CHECKING OTHER XML FILES ==="
for file in app/src/main/res/xml/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        if ! check_resource_usage "$file" "xml" "$resource_name"; then
            unused_files+=("$file")
        fi
        echo ""
    fi
done

echo "=== RESULTS ==="
echo "Total unused XML files found: ${#unused_files[@]}"

if [ ${#unused_files[@]} -gt 0 ]; then
    echo ""
    echo "UNUSED XML FILES:"
    for file in "${unused_files[@]}"; do
        echo "  - $file"
    done
    
    echo ""
    echo "Next 5 files to consider for deletion:"
    for i in {0..4}; do
        if [ $i -lt ${#unused_files[@]} ]; then
            echo "  ${unused_files[$i]}"
        fi
    done
else
    echo ""
    echo "No unused XML files found."
fi
