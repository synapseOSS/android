#!/bin/bash

# Find orphan classes in Kotlin codebase
# Classes that are defined but never imported or referenced elsewhere

echo "🔍 Searching for orphan classes in Synapse Android codebase..."
echo "=================================================="

# Directory to search
SEARCH_DIR="app/src/main/java"

# Find all Kotlin files
KOTLIN_FILES=$(find "$SEARCH_DIR" -name "*.kt" -type f)

# Array to store orphan classes
declare -a ORPHAN_CLASSES=()

# Function to extract class names from a file
extract_classes() {
    local file="$1"
    # Extract class, interface, object, and enum declarations
    grep -E "^(class|interface|object|enum class|data class|sealed class)" "$file" | \
    sed -E 's/^(class|interface|object|enum class|data class|sealed class)\s+([A-Za-z_][A-Za-z0-9_]*).*/\2/' | \
    grep -v "^$"
}

# Function to check if a class is referenced in other files
is_class_referenced() {
    local class_name="$1"
    local defining_file="$2"
    
    # Search for references in all Kotlin files except the defining file
    local references=$(find "$SEARCH_DIR" -name "*.kt" -type f ! -path "$defining_file" -exec grep -l "\b$class_name\b" {} \; 2>/dev/null | wc -l)
    
    # Also check for imports
    local imports=$(find "$SEARCH_DIR" -name "*.kt" -type f ! -path "$defining_file" -exec grep -l "import.*\.$class_name" {} \; 2>/dev/null | wc -l)
    
    # Return 0 if no references found, 1 if references exist
    if [ "$references" -eq 0 ] && [ "$imports" -eq 0 ]; then
        return 0  # Not referenced
    else
        return 1  # Referenced
    fi
}

# Process each Kotlin file
for file in $KOTLIN_FILES; do
    # Extract classes from this file
    classes=$(extract_classes "$file")
    
    if [ -n "$classes" ]; then
        while IFS= read -r class_name; do
            if [ -n "$class_name" ]; then
                # Check if this class is referenced elsewhere
                if is_class_referenced "$class_name" "$file"; then
                    # Check if it's a utility, example, or deprecated class
                    file_content=$(cat "$file")
                    
                    # Look for indicators of utility/example/deprecated classes
                    is_utility=$(echo "$file_content" | grep -i -E "(utility|util|helper|example|deprecated|@deprecated|todo|fixme)" | wc -l)
                    
                    # Check file path for common orphan patterns
                    is_example_path=$(echo "$file" | grep -E "(example|util|helper|deprecated)" | wc -l)
                    
                    # Add to orphan list with classification
                    classification=""
                    if [ "$is_utility" -gt 0 ] || [ "$is_example_path" -gt 0 ]; then
                        if echo "$file_content" | grep -i "example" > /dev/null; then
                            classification="[EXAMPLE]"
                        elif echo "$file_content" | grep -i -E "(utility|util|helper)" > /dev/null; then
                            classification="[UTILITY]"
                        elif echo "$file_content" | grep -i -E "(deprecated|@deprecated)" > /dev/null; then
                            classification="[DEPRECATED]"
                        else
                            classification="[ORPHAN]"
                        fi
                    else
                        classification="[ORPHAN]"
                    fi
                    
                    ORPHAN_CLASSES+=("$classification $class_name - $file")
                fi
            fi
        done <<< "$classes"
    fi
done

# Display results
echo ""
echo "📊 ORPHAN CLASSES FOUND:"
echo "========================"

if [ ${#ORPHAN_CLASSES[@]} -eq 0 ]; then
    echo "✅ No orphan classes found!"
else
    for orphan in "${ORPHAN_CLASSES[@]}"; do
        echo "$orphan"
    done
    
    echo ""
    echo "📈 SUMMARY:"
    echo "==========="
    echo "Total orphan classes found: ${#ORPHAN_CLASSES[@]}"
    
    # Count by type
    example_count=$(printf '%s\n' "${ORPHAN_CLASSES[@]}" | grep -c "\[EXAMPLE\]")
    utility_count=$(printf '%s\n' "${ORPHAN_CLASSES[@]}" | grep -c "\[UTILITY\]")
    deprecated_count=$(printf '%s\n' "${ORPHAN_CLASSES[@]}" | grep -c "\[DEPRECATED\]")
    orphan_count=$(printf '%s\n' "${ORPHAN_CLASSES[@]}" | grep -c "\[ORPHAN\]")
    
    echo "- Example classes: $example_count"
    echo "- Utility classes: $utility_count"
    echo "- Deprecated classes: $deprecated_count"
    echo "- Other orphans: $orphan_count"
fi

echo ""
echo "🔍 DETAILED ANALYSIS:"
echo "===================="

# Show some specific files that might contain orphans
echo ""
echo "Files that might contain orphan classes:"
find "$SEARCH_DIR" -name "*.kt" -path "*/example*" -o -name "*Example*.kt" -o -name "*Util*.kt" -o -name "*Helper*.kt" | head -10

echo ""
echo "✅ Analysis complete!"