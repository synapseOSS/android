#!/bin/bash

echo "=== SEARCHING FOR ADDITIONAL UNUSED XML FILES ==="

unused_files=()

# Check AndroidManifest_nav3.xml specifically
echo "Checking: app/src/main/AndroidManifest_nav3.xml"
if ! grep -r "AndroidManifest_nav3" app/src/ build.gradle app/build.gradle 2>/dev/null; then
    echo "  ✗ NO REFERENCES FOUND"
    unused_files+=("app/src/main/AndroidManifest_nav3.xml")
else
    echo "  ✓ Found references"
fi
echo ""

# Check for any XML files that might be test-only or deprecated
echo "=== CHECKING FOR DEPRECATED OR TEST-ONLY XML FILES ==="

# Look for files with "test", "debug", "sample", "temp", "old", "backup" in names
for pattern in "test" "debug" "sample" "temp" "old" "backup" "unused" "deprecated"; do
    echo "Checking for files with pattern: $pattern"
    find app/src/main/res -name "*${pattern}*.xml" -type f | while read file; do
        if [ -f "$file" ]; then
            echo "Found potential unused file: $file"
            filename=$(basename "$file")
            resource_name="${filename%.xml}"
            
            # Quick check if it's referenced
            if ! grep -r --include="*.java" --include="*.kt" --include="*.xml" \
                 -e "$resource_name" \
                 -e "$(basename "$file")" \
                 app/src/main/ 2>/dev/null | grep -v "$file" | head -1; then
                echo "  ✗ NO REFERENCES FOUND for $file"
                unused_files+=("$file")
            else
                echo "  ✓ Found references for $file"
            fi
        fi
    done
    echo ""
done

# Check for any XML files that are only referenced in comments
echo "=== CHECKING FOR FILES ONLY REFERENCED IN COMMENTS ==="
for file in app/src/main/res/drawable/*.xml app/src/main/res/anim/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        # Check if only referenced in comments
        active_refs=$(grep -r --include="*.java" --include="*.kt" \
                     -e "R\..*\.$resource_name" \
                     -e "@.*/$resource_name" \
                     app/src/main/java/ 2>/dev/null | grep -v "^\s*//" | grep -v "^\s*\*" | wc -l)
        
        comment_refs=$(grep -r --include="*.java" --include="*.kt" \
                      -e "$resource_name" \
                      app/src/main/java/ 2>/dev/null | grep -E "^\s*(//|\*)" | wc -l)
        
        if [ "$active_refs" -eq 0 ] && [ "$comment_refs" -gt 0 ]; then
            echo "File only referenced in comments: $file"
            unused_files+=("$file")
        fi
    fi
done

echo "=== FINAL RESULTS ==="
echo "Total unused XML files found: ${#unused_files[@]}"

if [ ${#unused_files[@]} -gt 0 ]; then
    echo ""
    echo "UNUSED XML FILES:"
    for file in "${unused_files[@]}"; do
        echo "  - $file"
    done
    
    echo ""
    echo "First 5 files to consider for deletion:"
    for i in {0..4}; do
        if [ $i -lt ${#unused_files[@]} ]; then
            echo "  ${unused_files[$i]}"
        fi
    done
else
    echo ""
    echo "No additional unused XML files found."
fi
