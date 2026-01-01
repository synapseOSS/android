#!/bin/bash

echo "=== FINAL COMPREHENSIVE UNUSED FILE SEARCH ==="

unused_files=()
unused_files+=("app/src/main/AndroidManifest_nav3.xml")

# Check for any XML files that might be referenced only in commented code
echo "=== CHECKING FOR FILES REFERENCED ONLY IN COMMENTS ==="
for file in app/src/main/res/drawable/*.xml app/src/main/res/anim/*.xml app/src/main/res/layout/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        # Count active references (not in comments)
        active_refs=$(grep -r --include="*.java" --include="*.kt" \
                     -e "R\.[^.]*\.$resource_name" \
                     -e "@[^/]*/$resource_name" \
                     app/src/main/java/ 2>/dev/null | \
                     grep -v -E '^\s*(//|/\*|\*)' | wc -l)
        
        # Count total references including comments
        total_refs=$(grep -r --include="*.java" --include="*.kt" \
                    "$resource_name" \
                    app/src/main/java/ 2>/dev/null | wc -l)
        
        # Check XML references
        xml_refs=$(find app/src/main/res -name "*.xml" -not -path "*/$file" \
                  -exec grep -l "$resource_name" {} \; 2>/dev/null | wc -l)
        
        if [ "$active_refs" -eq 0 ] && [ "$xml_refs" -eq 0 ] && [ "$total_refs" -gt 0 ]; then
            echo "File referenced only in comments: $file"
            echo "  Total refs: $total_refs, Active refs: $active_refs, XML refs: $xml_refs"
            unused_files+=("$file")
        fi
    fi
done

# Check for any files that might be used only in debug builds
echo "=== CHECKING FOR DEBUG-ONLY FILES ==="
for file in app/src/main/res/drawable/*.xml app/src/main/res/layout/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        # Check if only referenced in debug-related files
        debug_refs=$(grep -r --include="*.java" --include="*.kt" \
                    "$resource_name" \
                    app/src/main/java/ 2>/dev/null | \
                    grep -i -E "(debug|test)" | wc -l)
        
        total_refs=$(grep -r --include="*.java" --include="*.kt" \
                    "$resource_name" \
                    app/src/main/java/ 2>/dev/null | wc -l)
        
        if [ "$debug_refs" -gt 0 ] && [ "$debug_refs" -eq "$total_refs" ]; then
            echo "File used only in debug context: $file"
            # Let's be conservative and not mark debug files as unused for now
        fi
    fi
done

# Check for any XML files that might be alternatives or backups
echo "=== CHECKING FOR ALTERNATIVE/BACKUP FILES ==="
for file in app/src/main/res/drawable/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        
        # Look for files with version numbers or alternative indicators
        if [[ "$filename" =~ _v[0-9]+\.xml$ ]] || \
           [[ "$filename" =~ _alt\.xml$ ]] || \
           [[ "$filename" =~ _backup\.xml$ ]] || \
           [[ "$filename" =~ _old\.xml$ ]] || \
           [[ "$filename" =~ _new\.xml$ ]] || \
           [[ "$filename" =~ _copy\.xml$ ]] || \
           [[ "$filename" =~ _[0-9]+\.xml$ ]]; then
            
            resource_name="${filename%.xml}"
            echo "Checking alternative/backup file: $file"
            
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
    fi
done

# Check for any files that might be unused due to refactoring
echo "=== CHECKING FOR POTENTIALLY REFACTORED FILES ==="

# Look for drawable files that might have been replaced by vector drawables
for file in app/src/main/res/drawable/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        # Check if there's a similar vector drawable
        if [ -f "app/src/main/res/drawable/${resource_name}_vector.xml" ] || \
           [ -f "app/src/main/res/drawable/ic_${resource_name}.xml" ] || \
           [ -f "app/src/main/res/drawable/vector_${resource_name}.xml" ]; then
            
            echo "Checking potentially replaced drawable: $file"
            
            # Check usage
            if ! grep -r --include="*.java" --include="*.kt" --include="*.xml" \
                 -e "R\.drawable\.$resource_name" \
                 -e "@drawable/$resource_name" \
                 app/src/main/ 2>/dev/null | grep -v "$file" | head -1; then
                echo "  ✗ NO REFERENCES FOUND (might be replaced)"
                unused_files+=("$file")
            else
                echo "  ✓ Found references"
            fi
            echo ""
        fi
    fi
done

# Remove duplicates
declare -A seen
unique_unused=()
for file in "${unused_files[@]}"; do
    if [ -z "${seen[$file]}" ]; then
        seen[$file]=1
        unique_unused+=("$file")
    fi
done

echo "=== FINAL COMPREHENSIVE RESULTS ==="
echo "Total unique unused XML files found: ${#unique_unused[@]}"

if [ ${#unique_unused[@]} -gt 0 ]; then
    echo ""
    echo "ALL UNUSED XML FILES:"
    for file in "${unique_unused[@]}"; do
        echo "  - $file"
        
        # Show file size for context
        if [ -f "$file" ]; then
            size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || echo "unknown")
            echo "    Size: $size bytes"
        fi
    done
    
    echo ""
    echo "RECOMMENDED FOR DELETION (first 5):"
    for i in {0..4}; do
        if [ $i -lt ${#unique_unused[@]} ]; then
            echo "  $((i+1)). ${unique_unused[$i]}"
        fi
    done
else
    echo ""
    echo "No unused XML files found."
fi

# If we still don't have 5 files, let's try some other file types
if [ ${#unique_unused[@]} -lt 5 ]; then
    echo ""
    echo "=== EXPANDING SEARCH TO OTHER FILE TYPES ==="
    
    # Check for unused PNG files that might be referenced as drawables
    echo "Checking for unused PNG files in drawable directories..."
    for file in app/src/main/res/drawable*/*.png; do
        if [ -f "$file" ]; then
            filename=$(basename "$file" .png)
            
            # Check if referenced
            if ! grep -r --include="*.java" --include="*.kt" --include="*.xml" \
                 -e "R\.drawable\.$filename" \
                 -e "@drawable/$filename" \
                 -e "\"$filename\"" \
                 app/src/main/ 2>/dev/null | head -1; then
                echo "Unused PNG file: $file"
                unique_unused+=("$file")
                
                if [ ${#unique_unused[@]} -ge 5 ]; then
                    break
                fi
            fi
        fi
    done
    
    # Check for unused font files
    if [ ${#unique_unused[@]} -lt 5 ]; then
        echo "Checking for unused font files..."
        for file in app/src/main/res/font/*.ttf app/src/main/res/font/*.otf; do
            if [ -f "$file" ]; then
                filename=$(basename "$file")
                filename_no_ext="${filename%.*}"
                
                if ! grep -r --include="*.java" --include="*.kt" --include="*.xml" \
                     -e "$filename" \
                     -e "$filename_no_ext" \
                     app/src/main/ 2>/dev/null | head -1; then
                    echo "Unused font file: $file"
                    unique_unused+=("$file")
                    
                    if [ ${#unique_unused[@]} -ge 5 ]; then
                        break
                    fi
                fi
            fi
        done
    fi
fi

echo ""
echo "=== FINAL SUMMARY ==="
echo "Total unused files found: ${#unique_unused[@]}"
echo ""
echo "NEXT 5 FILES TO DELETE:"
for i in {0..4}; do
    if [ $i -lt ${#unique_unused[@]} ]; then
        echo "  $((i+1)). ${unique_unused[$i]}"
    fi
done
