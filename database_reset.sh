#!/bin/bash

# Database Reset Utility for Synapse Android App
# This script helps fix "Database column mismatched" errors

echo "🔧 Synapse Database Reset Utility"
echo "=================================="

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found. Please install Android SDK Platform Tools."
    exit 1
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected. Please connect your device and enable USB debugging."
    exit 1
fi

PACKAGE_NAME="com.synapse.social.studioasinc"

echo "📱 Checking if Synapse app is installed..."
if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo "❌ Synapse app not found on device."
    exit 1
fi

echo "✅ Synapse app found!"

echo "🛑 Stopping Synapse app..."
adb shell am force-stop "$PACKAGE_NAME"

echo "🗑️  Clearing app data and cache..."
adb shell pm clear "$PACKAGE_NAME"

echo "🔄 Restarting Synapse app..."
adb shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1

echo "✅ Database reset complete!"
echo ""
echo "The app should now recreate the database with the correct schema."
echo "Try posting, commenting, or chatting again."
