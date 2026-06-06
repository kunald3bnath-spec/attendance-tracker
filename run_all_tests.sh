#!/bin/bash
export JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/Users/kunal/Library/Android/sdk
export ANDROID_SDK_ROOT=/Users/kunal/Library/Android/sdk
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH

echo "Checking if emulator is already running..."
if adb devices | grep -q "emulator"; then
    echo "Emulator is already running."
else
    echo "Starting emulator medium_phone in background..."
    emulator -avd medium_phone -no-audio -no-window -gpu swiftshader_indirect -no-snapshot-load &
    
    echo "Waiting for emulator to boot..."
    adb wait-for-device
    until adb shell getprop sys.boot_completed | grep -q 1; do
        sleep 2
    done
    echo "Emulator booted successfully!"
fi

echo "Running unit tests..."
./gradlew test > unit_test_results.log 2>&1
UNIT_TEST_EXIT=$?
if [ $UNIT_TEST_EXIT -eq 0 ]; then
    echo "Unit tests PASSED"
else
    echo "Unit tests FAILED (check unit_test_results.log)"
fi

echo "Running instrumented tests (connectedDebugAndroidTest)..."
./gradlew connectedDebugAndroidTest > instrumented_test_results.log 2>&1
INSTR_TEST_EXIT=$?
if [ $INSTR_TEST_EXIT -eq 0 ]; then
    echo "Instrumented tests PASSED"
else
    echo "Instrumented tests FAILED (check instrumented_test_results.log)"
fi

echo "Capturing logcat..."
adb logcat -d > logcat_output.log

echo "All tasks completed!"
exit $((UNIT_TEST_EXIT + INSTR_TEST_EXIT))
