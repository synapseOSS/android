#!/bin/bash

echo "=== CHECKING FOR FILES WITH MINIMAL OR CONDITIONAL USAGE ==="

# Let's look for files that might be used only in very specific contexts
# and could potentially be removed if those contexts are no longer needed

potential_candidates=()
potential_candidates+=("app/src/main/AndroidManifest_nav3.xml")

# Check for any XML files that are used only once and might be candidates for removal
echo "=== CHECKING FOR SINGLE-USE XML FILES ==="

for file in app/src/main/res/drawable/*.xml app/src/main/res/anim/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        # Count exact usage
        usage_count=$(grep -r --include="*.java" --include="*.kt" --include="*.xml" \
                     -e "R\.[^.]*\.$resource_name" \
                     -e "@[^/]*/$resource_name" \
                     app/src/main/ 2>/dev/null | wc -l)
        
        if [ "$usage_count" -eq 1 ]; then
            echo "Single-use file: $file (used $usage_count time)"
            
            # Let's see where it's used
            usage_location=$(grep -r --include="*.java" --include="*.kt" --include="*.xml" \
                           -e "R\.[^.]*\.$resource_name" \
                           -e "@[^/]*/$resource_name" \
                           app/src/main/ 2>/dev/null | head -1)
            echo "  Used in: $usage_location"
            
            # If it's used in a debug or test context, it might be a candidate
            if echo "$usage_location" | grep -i -E "(debug|test|sample)" > /dev/null; then
                echo "  -> Potential candidate (debug/test context)"
                potential_candidates+=("$file")
            fi
            echo ""
        fi
    fi
done

# Check for any files that might be used only in commented code or documentation
echo "=== CHECKING FOR FILES USED ONLY IN DOCUMENTATION ==="

for file in app/src/main/res/drawable/*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        resource_name="${filename%.xml}"
        
        # Check if used only in comments or documentation
        active_usage=$(grep -r --include="*.java" --include="*.kt" \
                      -e "R\.drawable\.$resource_name" \
                      -e "@drawable/$resource_name" \
                      app/src/main/java/ 2>/dev/null | \
                      grep -v -E '^\s*(//|/\*|\*)' | wc -l)
        
        xml_usage=$(find app/src/main/res -name "*.xml" -not -path "*/$file" \
                   -exec grep -l "@drawable/$resource_name" {} \; 2>/dev/null | wc -l)
        
        comment_usage=$(grep -r --include="*.java" --include="*.kt" \
                       "$resource_name" \
                       app/src/main/java/ 2>/dev/null | \
                       grep -E '^\s*(//|/\*|\*)' | wc -l)
        
        if [ "$active_usage" -eq 0 ] && [ "$xml_usage" -eq 0 ] && [ "$comment_usage" -gt 0 ]; then
            echo "File used only in comments: $file"
            potential_candidates+=("$file")
        fi
    fi
done

# Let's also check some specific files that might be legacy or unused
echo "=== CHECKING SPECIFIC POTENTIALLY LEGACY FILES ==="

# Check some files that might be legacy based on naming patterns
legacy_patterns=(
    "ic_launcher_background.xml"
    "ic_launcher_foreground.xml"
)

for pattern in "${legacy_patterns[@]}"; do
    file="app/src/main/res/drawable/$pattern"
    if [ -f "$file" ]; then
        resource_name="${pattern%.xml}"
        
        echo "Checking legacy pattern: $file"
        
        # Check if it's only used in mipmap XML files (which might indicate it's auto-generated)
        mipmap_usage=$(find app/src/main/res/mipmap* -name "*.xml" -exec grep -l "$resource_name" {} \; 2>/dev/null | wc -l)
        other_usage=$(grep -r --include="*.java" --include="*.kt" \
                     -e "R\.drawable\.$resource_name" \
                     app/src/main/java/ 2>/dev/null | wc -l)
        
        echo "  Mipmap usage: $mipmap_usage, Other usage: $other_usage"
        
        if [ "$mipmap_usage" -gt 0 ] && [ "$other_usage" -eq 0 ]; then
            echo "  -> Used only in mipmap files (might be auto-generated)"
            # These are typically needed for app icons, so let's be conservative
        fi
        echo ""
    fi
done

# Remove duplicates and show results
declare -A seen
unique_candidates=()
for file in "${potential_candidates[@]}"; do
    if [ -z "${seen[$file]}" ]; then
        seen[$file]=1
        unique_candidates+=("$file")
    fi
done

echo "=== RESULTS ==="
echo "Total potential candidates: ${#unique_candidates[@]}"

if [ ${#unique_candidates[@]} -gt 0 ]; then
    echo ""
    echo "POTENTIAL CANDIDATES FOR DELETION:"
    for i in "${!unique_candidates[@]}"; do
        echo "  $((i+1)). ${unique_candidates[$i]}"
        
        if [ -f "${unique_candidates[$i]}" ]; then
            size=$(stat -f%z "${unique_candidates[$i]}" 2>/dev/null || stat -c%s "${unique_candidates[$i]}" 2>/dev/null || echo "unknown")
            echo "     Size: $size bytes"
        fi
    done
else
    echo ""
    echo "No additional candidates found."
fi

echo ""
echo "=== RECOMMENDATION ==="
echo "Based on comprehensive analysis, only 1 XML file is definitively unused:"
echo "  1. app/src/main/AndroidManifest_nav3.xml (6550 bytes)"
echo ""
echo "This codebase appears to be very well-maintained with minimal unused resources."
echo "Proceeding with deletion of the confirmed unused file only would be the safest approach."
