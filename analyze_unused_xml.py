#!/usr/bin/env python3
import os
import re
import glob
import subprocess
from pathlib import Path

def find_xml_files():
    """Find all XML files in res directory"""
    result = subprocess.run(['find', 'app/src/main/res', '-name', '*.xml'], 
                          capture_output=True, text=True)
    return result.stdout.strip().split('\n') if result.stdout.strip() else []

def extract_resource_name(file_path):
    """Extract resource name from file path"""
    filename = os.path.basename(file_path)
    return os.path.splitext(filename)[0]

def find_resource_references(resource_name, resource_type):
    """Find references to a resource in source code"""
    references = []
    
    # Search patterns - more comprehensive
    patterns = [
        rf'R\.{resource_type}\.{resource_name}\b',
        rf'@{resource_type}/{resource_name}\b',
        rf'@\+{resource_type}/{resource_name}\b',
        rf'"{resource_name}"',
        rf"'{resource_name}'",
        rf'{resource_name}\.xml',
        rf'layout="{resource_name}"',
        rf"layout='{resource_name}'",
        rf'android:background="@{resource_type}/{resource_name}"',
        rf'android:src="@{resource_type}/{resource_name}"',
        rf'app:srcCompat="@{resource_type}/{resource_name}"',
    ]
    
    # Search in Java/Kotlin files
    try:
        java_result = subprocess.run(['find', 'app/src/main/java', '-name', '*.java'], 
                                   capture_output=True, text=True)
        kotlin_result = subprocess.run(['find', 'app/src/main/java', '-name', '*.kt'], 
                                     capture_output=True, text=True)
        xml_result = subprocess.run(['find', 'app/src/main/res', '-name', '*.xml'], 
                                  capture_output=True, text=True)
        
        all_files = []
        if java_result.stdout.strip():
            all_files.extend(java_result.stdout.strip().split('\n'))
        if kotlin_result.stdout.strip():
            all_files.extend(kotlin_result.stdout.strip().split('\n'))
        if xml_result.stdout.strip():
            all_files.extend(xml_result.stdout.strip().split('\n'))
        
        for file_path in all_files:
            if not os.path.exists(file_path):
                continue
                
            try:
                with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                    for pattern in patterns:
                        if re.search(pattern, content):
                            references.append(file_path)
                            break
            except Exception:
                continue
                
    except Exception:
        pass
    
    return references

def get_resource_type(file_path):
    """Determine resource type from file path"""
    dir_name = os.path.basename(os.path.dirname(file_path))
    if dir_name.startswith('drawable'):
        return 'drawable'
    elif dir_name == 'layout':
        return 'layout'
    elif dir_name == 'anim':
        return 'anim'
    elif dir_name == 'animator':
        return 'animator'
    elif dir_name == 'menu':
        return 'menu'
    elif dir_name == 'color':
        return 'color'
    elif dir_name == 'xml':
        return 'xml'
    else:
        return 'unknown'

def main():
    print("🔍 Analyzing unused XML resources in Synapse project...")
    print("=" * 60)
    
    # Find all XML files
    xml_files = find_xml_files()
    print(f"📊 Total XML files found: {len(xml_files)}")
    
    unused_resources = []
    used_resources = []
    
    for xml_file in xml_files:
        resource_name = extract_resource_name(xml_file)
        resource_type = get_resource_type(xml_file)
        
        # Skip certain system files
        if resource_name in ['ic_launcher', 'ic_launcher_background', 'ic_launcher_foreground', 'ic_launcher_round']:
            continue
            
        references = find_resource_references(resource_name, resource_type)
        
        if not references:
            unused_resources.append({
                'file': xml_file,
                'name': resource_name,
                'type': resource_type
            })
        else:
            used_resources.append({
                'file': xml_file,
                'name': resource_name,
                'type': resource_type,
                'references': len(references)
            })
    
    # Print results
    print(f"\n✅ Used resources: {len(used_resources)}")
    print(f"❌ Unused resources: {len(unused_resources)}")
    print(f"📈 Usage rate: {(len(used_resources) / len(xml_files)) * 100:.1f}%")
    
    if unused_resources:
        print("\n🗑️  UNUSED XML RESOURCES:")
        print("-" * 40)
        
        # Group by type
        by_type = {}
        for resource in unused_resources:
            res_type = resource['type']
            if res_type not in by_type:
                by_type[res_type] = []
            by_type[res_type].append(resource)
        
        for res_type, resources in by_type.items():
            print(f"\n📁 {res_type.upper()} ({len(resources)} files):")
            for resource in resources:
                print(f"  • {resource['name']} ({resource['file']})")
    
    # Save detailed report
    with open('unused_xml_analysis.txt', 'w') as f:
        f.write("SYNAPSE - UNUSED XML RESOURCES ANALYSIS\n")
        f.write("=" * 50 + "\n\n")
        f.write(f"Total XML files analyzed: {len(xml_files)}\n")
        f.write(f"Used resources: {len(used_resources)}\n")
        f.write(f"Unused resources: {len(unused_resources)}\n")
        f.write(f"Usage rate: {(len(used_resources) / len(xml_files)) * 100:.1f}%\n\n")
        
        if unused_resources:
            f.write("UNUSED RESOURCES BY TYPE:\n")
            f.write("-" * 30 + "\n")
            
            for res_type, resources in by_type.items():
                f.write(f"\n{res_type.upper()} ({len(resources)} files):\n")
                for resource in resources:
                    f.write(f"  {resource['file']}\n")
    
    print(f"\n📄 Detailed report saved to: unused_xml_analysis.txt")

if __name__ == "__main__":
    main()