# Attendance Tracker Android Application

A premium, modern Android Attendance Tracker built using Jetpack Compose, Material 3, and Room DB local storage. Designed with rich aesthetics, light/dark mode support, and features built entirely using AI Agent assistance.

## Key Features
1. **Multi-Project Organization**: Root-level project dashboard. Create and scope separate attendance lists for different projects or groups.
2. **Inline Calendar Dashboard**: Interactive calendar picker displayed inline (no typing required) to select any target date easily.
3. **Batch Attendance Submit**: Attendance grid displaying avatars and initials, allowing users to toggle "Present/Absent" states with a unified batch **Submit** button.
4. **Interactive History Log**: View per-member attendance history lists neatly grouped and sorted by month.
5. **Overview Heatmap**: Interactive visual heatmap showing daily attendance density/intensity, coupled with expandable lists of attendees per date.
6. **Built by AI Agent Pill Badge**: Sparkling purple gradient indicator on headers demonstrating the AI-assisted build.
7. **Premium Design**: Dark/light mode theme with smooth transition animations and an indigo primary palette.

---

## 🛠️ Building & Running Locally

### Prerequisites
- **JDK 17**: Ensure Java 17 is installed on your system.
- **Android SDK**: Android API level 35 capability.
- **Gradle**: Gradle 8.14+ is used for compilation.

### Step-by-Step CLI Build Instructions
Open your terminal inside the project root directory:

1. **Set Java environment variable** (pointing to your JDK 17 installation, for example on macOS):
   ```bash
   export JAVA_HOME=/opt/homebrew/opt/openjdk@17
   export PATH="$JAVA_HOME/bin:$PATH"
   ```

2. **Clean and Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug --no-daemon
   ```
   Once completed successfully, the APK will be generated at:
   `app/build/outputs/apk/debug/AttendanceTracker-debug-1.0.apk`

---

## 📲 Installing the APK on your Android System

To install the compiled APK on any Android phone or device, use one of the following methods:

### Method 1: Installing via ADB (Recommended for Developers)
1. Enable **Developer Options** and **USB Debugging** on your Android device:
   - Go to *Settings* -> *About Phone*.
   - Tap *Build Number* 7 times until you see "You are now a developer!".
   - Go back to *Settings* -> *System* -> *Developer Options* and enable **USB Debugging**.
2. Connect your Android device to your computer via USB.
3. Install the APK from terminal:
   ```bash
   adb install app/build/outputs/apk/debug/AttendanceTracker-debug-1.0.apk
   ```

### Method 2: Manual Installation (Installing directly from Phone)
1. Transfer the built APK file `AttendanceTracker-debug-1.0.apk` to your phone (e.g., via Google Drive, USB transfer, or Email).
2. Open a File Manager app on your Android phone and navigate to the folder containing the APK.
3. Tap the APK file to install it.
4. If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*:
   - Tap **Settings** on the prompt.
   - Toggle **Allow from this source** to ON.
   - Navigate back and tap **Install**.
5. Once installed, tap **Open** to launch the Attendance Tracker app.

---

## ⚖️ License
This project is licensed under the [MIT License](LICENSE) - which offers free usage, modification, and distribution rights while explicitly disclaiming any warranties or liabilities to protect developers from legal issues.
