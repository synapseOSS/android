#!/usr/bin/env python3
import os
import re
import glob

def get_drawable_files(project_path):
    drawable_dirs = glob.glob(f"{project_path}/**/drawable*", recursive=True)
    drawables = set()
    for dir_path in drawable_dirs:
        if os.path.isdir(dir_path):
            for file in os.listdir(dir_path):
                if file.endswith(('.png', '.jpg', '.jpeg', '.webp', '.xml', '.svg')):
                    drawables.add(os.path.splitext(file)[0])
    return drawables

def find_drawable_references(project_path):
    patterns = [
        r'@drawable/(\w+)',
        r'R\.drawable\.(\w+)',
        r'getDrawable\([^,)]*R\.drawable\.(\w+)',
        r'ContextCompat\.getDrawable\([^,)]*R\.drawable\.(\w+)',
        r'drawable="@drawable/(\w+)"'
    ]
    
    used_drawables = set()
    file_patterns = ['**/*.xml', '**/*.kt', '**/*.java']
    
    for pattern in file_patterns:
        for file_path in glob.glob(f"{project_path}/{pattern}", recursive=True):
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    for regex in patterns:
                        matches = re.findall(regex, content)
                        used_drawables.update(matches)
            except:
                continue
    
    return used_drawables

def main():
    project_path = input("Enter Android project path (or '.' for current): ").strip() or '.'
    
    print("Scanning drawable files...")
    all_drawables = get_drawable_files(project_path)
    
    print("Scanning for references...")
    used_drawables = find_drawable_references(project_path)
    
    unused_drawables = all_drawables - used_drawables
    
    print(f"\nFound {len(all_drawables)} drawable files")
    print(f"Found {len(used_drawables)} referenced drawables")
    print(f"Found {len(unused_drawables)} unused drawables\n")
    
    if unused_drawables:
        print("Unused drawables:")
        for drawable in sorted(unused_drawables):
            print(f"  {drawable}")
    else:
        print("No unused drawables found!")

if __name__ == "__main__":
    main()