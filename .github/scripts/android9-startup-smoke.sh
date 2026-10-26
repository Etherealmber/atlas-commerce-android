#!/usr/bin/env bash
set +e

./gradlew :app:connectedDebugAndroidTest --no-daemon --console=plain --stacktrace 2>&1 | tee api28-gradle.log
gradle_status=${PIPESTATUS[0]}

adb logcat -d -v threadtime > api28-logcat.txt 2>&1
adb shell dumpsys activity activities > api28-activities.txt 2>&1

if [ "$gradle_status" -ne 0 ]; then
  echo "=== Android 9 test failure and app-specific runtime diagnostics ==="
  test_details=$(grep -E -i "FAILURE:|FAILED|Tests run:|Timed out|AssertionError|NoSuchMethodError|NoClassDefFoundError|AndroidJUnitRunner|ma\.etherealmber\.atlascommerce|^e: |error:|Unresolved reference|Type mismatch|Cannot infer|Compilation error" api28-gradle.log | tail -n 35 || true)
  app_details=$(grep -E -i "Android9StartupSmoke|FATAL EXCEPTION|ANR in ma\.etherealmber\.atlascommerce|Process: ma\.etherealmber\.atlascommerce|ma\.etherealmber\.atlascommerce" api28-logcat.txt | tail -n 20 || true)
  matches=$(printf '%s\n%s' "$test_details" "$app_details" | sed '/^[[:space:]]*$/d')
  if [ -z "$matches" ]; then
    matches=$(tail -n 50 api28-gradle.log || true)
  fi
  printf '%s\n' "$matches"
  summary=$(printf '%s' "$matches" | tr '\n' ' ' | cut -c1-3500)
  echo "::error title=Android 9 startup diagnostics::$summary"
  echo "=== Activity state ==="
  cat api28-activities.txt || true
fi

exit "$gradle_status"
