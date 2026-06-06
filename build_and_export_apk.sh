#!/bin/bash
set -e

echo "Step 1: Generating launcher icons using sips..."
chmod +x create_launcher_icons.sh
./create_launcher_icons.sh

echo "Step 2: Building the debug APK..."
./gradlew assembleDebug

echo "Step 3: Creating target directory and exporting APK..."
mkdir -p /Users/kunal/Desktop/apk
cp app/build/outputs/apk/debug/app-debug.apk /Users/kunal/Desktop/apk/attendance-tracker.apk

echo "Done! APK successfully generated and saved to /Users/kunal/Desktop/apk/attendance-tracker.apk"
