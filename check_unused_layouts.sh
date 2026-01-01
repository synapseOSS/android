#!/bin/bash

cd app/src/main/res/layout

echo "Checking layout files for usage..."
echo "=================================="

for file in *.xml; do
    if [ -f "$file" ]; then
        name=$(basename "$file" .xml)
        size=$(stat -c%s "$file")
        
        # Skip if file is larger than 2KB
        if [ $size -gt 2048 ]; then
            continue
        fi
        
        # Check for direct R.layout references
        direct_refs=$(grep -r "R\.layout\.$name" ../../ --include="*.kt" --include="*.java" 2>/dev/null | wc -l)
        
        # Check for data binding references (convert snake_case to PascalCase)
        binding_name=$(echo "$name" | sed 's/_\([a-z]\)/\U\1/g' | sed 's/^./\U&/')
        binding_refs=$(grep -r "${binding_name}Binding" ../../ --include="*.kt" --include="*.java" 2>/dev/null | wc -l)
        
        # Check for include references in other XML files
        include_refs=$(grep -r "layout=\"@layout/$name\"" ../.. --include="*.xml" 2>/dev/null | wc -l)
        
        # Check for tools:listitem references
        listitem_refs=$(grep -r "tools:listitem=\"@layout/$name\"" ../.. --include="*.xml" 2>/dev/null | wc -l)
        
        total_refs=$((direct_refs + binding_refs + include_refs + listitem_refs))
        
        if [ $total_refs -eq 0 ]; then
            echo "UNUSED: $file ($size bytes)"
        fi
    fi
done