#!/bin/bash

# Android Unused Assets Detection Script
# Finds unused drawables, strings, layouts, and other resources

set -e

# Configuration
PROJECT_ROOT="$(pwd)"
RES_DIR="$PROJECT_ROOT/app/src/main/res"
JAVA_DIR="$PROJECT_ROOT/app/src/main/java"
MANIFEST_FILE="$PROJECT_ROOT/app/src/main/AndroidManifest.xml"
DOCS_DIR="$PROJECT_ROOT/docs"
OUTPUT_FILE="$DOCS_DIR/unused.md"
DELETE_MODE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --delete)
            DELETE_MODE=true
            shift
            ;;
        *)
            echo "Usage: $0 [--delete]"
            echo "  --delete: Delete unused assets (default: false)"
            exit 1
            ;;
    esac
done

# Create docs directory if it doesn't exist
mkdir -p "$DOCS_DIR"

# Initialize arrays for unused resources
declare -a unused_drawables=()
declare -a unused_strings=()
declare -a unused_layouts=()
declare -a unused_colors=()
declare -a unused_dimens=()
declare -a unused_other=()

echo "Scanning for unused Android resources..."

# Function to check if a resource is referenced
is_resource_used() {
    local resource_name="$1"
    local resource_type="$2"
    
    # Search in Java/Kotlin files
    if grep -r "R\.$resource_type\.$resource_name\|@$resource_type/$resource_name" "$JAVA_DIR" >/dev/null 2>&1; then
        return 0
    fi
    
    # Search in XML files (layouts, manifests, etc.)
    if grep -r "@$resource_type/$resource_name" "$RES_DIR" "$MANIFEST_FILE" >/dev/null 2>&1; then
        return 0
    fi
    
    return 1
}

# Function to find unused drawables
find_unused_drawables() {
    echo "Checking drawable resources..."
    
    for drawable_dir in "$RES_DIR"/drawable* "$RES_DIR"/mipmap*; do
        if [[ -d "$drawable_dir" ]]; then
            for file in "$drawable_dir"/*; do
                if [[ -f "$file" ]]; then
                    filename=$(basename "$file")
                    resource_name="${filename%.*}"
                    
                    # Skip launcher icons and other system resources
                    if [[ "$resource_name" =~ ^(ic_launcher|ic_launcher_background|ic_launcher_foreground)$ ]]; then
                        continue
                    fi
                    
                    if ! is_resource_used "$resource_name" "drawable" && ! is_resource_used "$resource_name" "mipmap"; then
                        unused_drawables+=("$file")
                    fi
                fi
            done
        fi
    done
}

# Function to find unused strings
find_unused_strings() {
    echo "Checking string resources..."
    
    if [[ -f "$RES_DIR/values/strings.xml" ]]; then
        while IFS= read -r line; do
            if [[ "$line" =~ \<string[[:space:]]+name=\"([^\"]+)\" ]]; then
                string_name="${BASH_REMATCH[1]}"
                
                # Skip app_name and other essential strings
                if [[ "$string_name" =~ ^(app_name|action_settings)$ ]]; then
                    continue
                fi
                
                if ! is_resource_used "$string_name" "string"; then
                    unused_strings+=("$string_name")
                fi
            fi
        done < "$RES_DIR/values/strings.xml"
    fi
}

# Function to find unused layouts
find_unused_layouts() {
    echo "Checking layout resources..."
    
    if [[ -d "$RES_DIR/layout" ]]; then
        for file in "$RES_DIR/layout"/*.xml; do
            if [[ -f "$file" ]]; then
                filename=$(basename "$file")
                layout_name="${filename%.*}"
                
                if ! is_resource_used "$layout_name" "layout"; then
                    unused_layouts+=("$file")
                fi
            fi
        done
    fi
}

# Function to find unused colors
find_unused_colors() {
    echo "Checking color resources..."
    
    if [[ -f "$RES_DIR/values/colors.xml" ]]; then
        while IFS= read -r line; do
            if [[ "$line" =~ \<color[[:space:]]+name=\"([^\"]+)\" ]]; then
                color_name="${BASH_REMATCH[1]}"
                
                if ! is_resource_used "$color_name" "color"; then
                    unused_colors+=("$color_name")
                fi
            fi
        done < "$RES_DIR/values/colors.xml"
    fi
}

# Function to find unused dimensions
find_unused_dimens() {
    echo "Checking dimension resources..."
    
    if [[ -f "$RES_DIR/values/dimens.xml" ]]; then
        while IFS= read -r line; do
            if [[ "$line" =~ \<dimen[[:space:]]+name=\"([^\"]+)\" ]]; then
                dimen_name="${BASH_REMATCH[1]}"
                
                if ! is_resource_used "$dimen_name" "dimen"; then
                    unused_dimens+=("$dimen_name")
                fi
            fi
        done < "$RES_DIR/values/dimens.xml"
    fi
}

# Function to delete unused resources
delete_unused_resources() {
    if [[ "$DELETE_MODE" == true ]]; then
        echo "Deleting unused resources..."
        
        # Delete unused drawables
        for file in "${unused_drawables[@]}"; do
            echo "Deleting: $file"
            rm -f "$file"
        done
        
        # Delete unused layouts
        for file in "${unused_layouts[@]}"; do
            echo "Deleting: $file"
            rm -f "$file"
        done
        
        echo "Note: String, color, and dimension resources need manual removal from XML files."
    fi
}

# Function to generate markdown report
generate_report() {
    echo "Generating report..."
    
    cat > "$OUTPUT_FILE" << EOF
# Unused Android Resources Report

Generated on: $(date)

## Summary

- **Unused Drawables**: ${#unused_drawables[@]}
- **Unused Strings**: ${#unused_strings[@]}
- **Unused Layouts**: ${#unused_layouts[@]}
- **Unused Colors**: ${#unused_colors[@]}
- **Unused Dimensions**: ${#unused_dimens[@]}

## Unused Drawables

EOF

    if [[ ${#unused_drawables[@]} -eq 0 ]]; then
        echo "No unused drawable resources found." >> "$OUTPUT_FILE"
    else
        for file in "${unused_drawables[@]}"; do
            echo "- \`$(basename "$file")\` - \`$file\`" >> "$OUTPUT_FILE"
        done
    fi

    cat >> "$OUTPUT_FILE" << EOF

## Unused String Resources

EOF

    if [[ ${#unused_strings[@]} -eq 0 ]]; then
        echo "No unused string resources found." >> "$OUTPUT_FILE"
    else
        for string in "${unused_strings[@]}"; do
            echo "- \`$string\`" >> "$OUTPUT_FILE"
        done
    fi

    cat >> "$OUTPUT_FILE" << EOF

## Unused Layout Files

EOF

    if [[ ${#unused_layouts[@]} -eq 0 ]]; then
        echo "No unused layout files found." >> "$OUTPUT_FILE"
    else
        for file in "${unused_layouts[@]}"; do
            echo "- \`$(basename "$file")\` - \`$file\`" >> "$OUTPUT_FILE"
        done
    fi

    cat >> "$OUTPUT_FILE" << EOF

## Unused Color Resources

EOF

    if [[ ${#unused_colors[@]} -eq 0 ]]; then
        echo "No unused color resources found." >> "$OUTPUT_FILE"
    else
        for color in "${unused_colors[@]}"; do
            echo "- \`$color\`" >> "$OUTPUT_FILE"
        done
    fi

    cat >> "$OUTPUT_FILE" << EOF

## Unused Dimension Resources

EOF

    if [[ ${#unused_dimens[@]} -eq 0 ]]; then
        echo "No unused dimension resources found." >> "$OUTPUT_FILE"
    else
        for dimen in "${unused_dimens[@]}"; do
            echo "- \`$dimen\`" >> "$OUTPUT_FILE"
        done
    fi

    cat >> "$OUTPUT_FILE" << EOF

## Notes

- This report was generated automatically and may contain false positives
- Resources used through reflection or dynamically may appear as unused
- Always review before deleting resources
- String, color, and dimension resources require manual removal from XML files

## Cleanup Commands

To delete unused drawable and layout files:
\`\`\`bash
./scripts/find_unused_assets.sh --delete
\`\`\`

EOF
}

# Main execution
echo "Starting unused resource detection..."
echo "Project root: $PROJECT_ROOT"
echo "Delete mode: $DELETE_MODE"
echo ""

find_unused_drawables
find_unused_strings
find_unused_layouts
find_unused_colors
find_unused_dimens

generate_report
delete_unused_resources

echo ""
echo "Scan complete!"
echo "Found unused resources:"
echo "  - Drawables: ${#unused_drawables[@]}"
echo "  - Strings: ${#unused_strings[@]}"
echo "  - Layouts: ${#unused_layouts[@]}"
echo "  - Colors: ${#unused_colors[@]}"
echo "  - Dimensions: ${#unused_dimens[@]}"
echo ""
echo "Report saved to: $OUTPUT_FILE"

if [[ "$DELETE_MODE" == true ]]; then
    echo "Unused files have been deleted."
else
    echo "Run with --delete flag to remove unused files."
fi
