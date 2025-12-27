#!/bin/bash

# Quick wrapper for unused assets detection
# Usage: ./cleanup_assets.sh [--delete]

echo "🔍 Android Unused Assets Detector"
echo "=================================="
echo ""

# Check if we're in an Android project
if [[ ! -f "app/build.gradle" ]]; then
    echo "❌ Error: Not in an Android project root directory"
    echo "   Please run this script from your Android project root"
    exit 1
fi

# Run the main detection script
./scripts/find_unused_assets.sh "$@"

echo ""
echo "📄 View the full report: docs/unused.md"
echo "🧹 To delete unused files: ./scripts/cleanup_assets.sh --delete"
