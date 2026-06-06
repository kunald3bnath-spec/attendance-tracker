#!/bin/bash
SRC_IMG="/Users/kunal/.gemini/antigravity-ide/brain/338c502c-c2d2-4d0d-95a8-6a0fbe786e16/attendance_tracker_logo_option3_v2_1780752928698.png"

# Create directories
mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi
mkdir -p app/src/main/res/drawable

# Resize using sips
sips -s format png -z 48 48 "$SRC_IMG" --out app/src/main/res/mipmap-mdpi/ic_launcher.png
sips -s format png -z 72 72 "$SRC_IMG" --out app/src/main/res/mipmap-hdpi/ic_launcher.png
sips -s format png -z 96 96 "$SRC_IMG" --out app/src/main/res/mipmap-xhdpi/ic_launcher.png
sips -s format png -z 144 144 "$SRC_IMG" --out app/src/main/res/mipmap-xxhdpi/ic_launcher.png
sips -s format png -z 192 192 "$SRC_IMG" --out app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
sips -s format png -z 512 512 "$SRC_IMG" --out app/src/main/res/drawable/ic_launcher_playstore.png

# Copy same for round icon
cp app/src/main/res/mipmap-mdpi/ic_launcher.png app/src/main/res/mipmap-mdpi/ic_launcher_round.png
cp app/src/main/res/mipmap-hdpi/ic_launcher.png app/src/main/res/mipmap-hdpi/ic_launcher_round.png
cp app/src/main/res/mipmap-xhdpi/ic_launcher.png app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
cp app/src/main/res/mipmap-xxhdpi/ic_launcher.png app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
cp app/src/main/res/mipmap-xxxhdpi/ic_launcher.png app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png

echo "Icons generated successfully!"
