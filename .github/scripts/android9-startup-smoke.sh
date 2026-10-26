#!/usr/bin/env bash
set +e

./gradlew :app:connectedDebugAndroidTest --no-daemon --console=plain --stacktrace
gradle_status=$?

adb logcat -d -v threadtime > api28-logcat.txt 2>&1
adb shell dumpsys activity activities > api28-activities.txt 2>&1

if [ "$gradle_status" -ne 0 ]; then
  echo "=== Android 9 startup/runtime diagnostics ==="
  matches=$(grep -E -i "FATAL EXCEPTION|AndroidRuntime|ANR in|not responding|NoSuchMethodError|NoClassDefFoundError|Exception|Error" api28-logcat.txt | tail -n 30 || true)
  if [ -z "$matches" ]; then
    matches=$(tail -n 60 api28-logcat.txt || true)
  fi
  printf '%s\n' "$matches"
  summary=$(printf '%s' "$matches" | tr '\n' ' ' | cut -c1-3500)
  echo "::error title=Android 9 startup diagnostics::$summary"
  echo "=== Activity state ==="
  cat api28-activities.txt || true
fi

exit "$gradle_status"
