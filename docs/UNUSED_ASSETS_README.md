# Android Unused Assets Cleanup Workflow

This workflow automatically detects unused assets, resources, and XML files in your Android project and generates a comprehensive report.

## Features

- 🔍 **Comprehensive Detection**: Finds unused drawables, strings, layouts, colors, and dimensions
- 📄 **Markdown Report**: Generates detailed documentation in `/docs/unused.md`
- 🧹 **Optional Cleanup**: Safely delete unused files with `--delete` flag
- ⚡ **Fast Scanning**: Efficiently searches through Java/Kotlin code and XML resources
- 🛡️ **Safe Defaults**: Preserves essential resources like launcher icons

## Usage

### Quick Start
```bash
# Scan for unused resources (safe, no deletion)
./cleanup_assets.sh

# Scan and delete unused files
./cleanup_assets.sh --delete
```

### Advanced Usage
```bash
# Use the main script directly
./find_unused_assets.sh [--delete]
```

## What Gets Detected

### ✅ Detected Resources
- **Drawables**: PNG, XML drawables in all density folders
- **Strings**: All string resources in `values/strings.xml`
- **Layouts**: XML layout files in `layout/` folder
- **Colors**: Color resources in `values/colors.xml`
- **Dimensions**: Dimension resources in `values/dimens.xml`

### 🛡️ Protected Resources
- Launcher icons (`ic_launcher*`)
- App name strings
- System-required resources

## Output

The script generates `/docs/unused.md` with:
- Summary statistics
- Detailed lists of unused resources
- File paths for easy location
- Cleanup instructions

## Safety Notes

⚠️ **Important Considerations**:
- Resources used via reflection may appear as unused
- Dynamic resource loading won't be detected
- Always review the report before deleting
- Test your app after cleanup
- Keep backups of important resources

## Example Output

```
🔍 Android Unused Assets Detector
==================================

Scanning for unused Android resources...
Found unused resources:
  - Drawables: 2
  - Strings: 593
  - Layouts: 25
  - Colors: 12
  - Dimensions: 6

📄 View the full report: docs/unused.md
```

## Integration

Add to your CI/CD pipeline:
```yaml
- name: Check for unused resources
  run: ./cleanup_assets.sh
```

## Requirements

- Bash shell
- Android project with standard structure
- `grep` command available

## Troubleshooting

**Script not found**: Make sure you're in the project root directory
**Permission denied**: Run `chmod +x *.sh` to make scripts executable
**False positives**: Review resources that might be used dynamically
