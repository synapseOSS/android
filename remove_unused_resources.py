#!/usr/bin/env python3
"""
Script to remove unused resources identified by Android lint
"""
import re
import os
import sys

def extract_unused_resources(lint_file):
    """Extract unused resource file paths from lint report"""
    unused_files = []
    
    with open(lint_file, 'r') as f:
        content = f.read()
    
    # Pattern to match unused resource warnings
    pattern = r'(/home/Ashik/android/app/src/main/res/[^:]+):[^:]*: Warning: The resource [^[]+\[UnusedResources\]'
    matches = re.findall(pattern, content)
    
    return list(set(matches))  # Remove duplicates

def remove_unused_resources(unused_files, dry_run=True):
    """Remove unused resource files"""
    removed_count = 0
    
    for file_path in unused_files:
        if os.path.exists(file_path):
            if dry_run:
                print(f"Would remove: {file_path}")
            else:
                try:
                    os.remove(file_path)
                    print(f"Removed: {file_path}")
                    removed_count += 1
                except Exception as e:
                    print(f"Error removing {file_path}: {e}")
        else:
            print(f"File not found: {file_path}")
    
    return removed_count

def main():
    lint_file = "/home/Ashik/android/app/build/reports/lint-results-debug.txt"
    
    if not os.path.exists(lint_file):
        print(f"Lint report not found: {lint_file}")
        return 1
    
    print("Extracting unused resources from lint report...")
    unused_files = extract_unused_resources(lint_file)
    
    print(f"Found {len(unused_files)} unused resource files")
    
    # First do a dry run
    print("\n=== DRY RUN ===")
    remove_unused_resources(unused_files, dry_run=True)
    
    # Ask for confirmation
    response = input(f"\nDo you want to remove these {len(unused_files)} files? (y/N): ")
    
    if response.lower() == 'y':
        print("\n=== REMOVING FILES ===")
        removed_count = remove_unused_resources(unused_files, dry_run=False)
        print(f"\nRemoved {removed_count} unused resource files")
    else:
        print("Operation cancelled")
    
    return 0

if __name__ == "__main__":
    sys.exit(main())
