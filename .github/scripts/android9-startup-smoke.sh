#!/usr/bin/env bash
set +e

# Keep a stuck instrumentation runner from consuming the entire emulator job budget.
timeout --signal=TERM --kill-after=30s 15m ./gradlew :app:connectedDebugAndroidTest --no-daemon --console=plain --stacktrace 2>&1 | tee api28-gradle.log
debug_status=${PIPESTATUS[0]}
smoke_status=$debug_status

# The production APK is minified, unlike the debug APK used by the instrumentation test.
# Run this check even if debug instrumentation fails so the two variants provide independent evidence.
timeout --signal=TERM --kill-after=30s 10m ./gradlew :app:assembleRelease --no-daemon --console=plain --stacktrace 2>&1 | tee api28-release-gradle.log
release_build_status=${PIPESTATUS[0]}
if [ "$release_build_status" -ne 0 ]; then
  if [ "$smoke_status" -eq 0 ]; then smoke_status=$release_build_status; fi
else
    sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    apksigner=$(find "$sdk_root/build-tools" -type f -name apksigner 2>/dev/null | sort -V | tail -n 1)
    release_apk=$(find app/build/outputs/apk/release -type f -name '*.apk' 2>/dev/null | head -n 1)
    debug_keystore="$HOME/.android/debug.keystore"
    mkdir -p "$HOME/.android"
    if [ ! -f "$debug_keystore" ]; then
      keytool -genkeypair -keystore "$debug_keystore" -storepass android -keypass android \
        -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -keysize 2048 -validity 10000
    fi
    if [ -z "$apksigner" ] || [ -z "$release_apk" ]; then
      echo "Could not locate apksigner or the minified release APK."
      smoke_status=1
    else
      cp "$release_apk" api28-release-signed.apk
      "$apksigner" sign --ks "$debug_keystore" --ks-key-alias androiddebugkey \
        --ks-pass pass:android --key-pass pass:android api28-release-signed.apk
      sign_status=$?
      if [ "$sign_status" -ne 0 ]; then
        smoke_status=$sign_status
      else
        adb uninstall ma.etherealmber.atlascommerce >/dev/null 2>&1 || true
        timeout 120s adb install api28-release-signed.apk > api28-release-install.txt 2>&1
        install_status=$?
        if [ "$install_status" -ne 0 ]; then
          cat api28-release-install.txt
          smoke_status=$install_status
        else
          adb shell am force-stop ma.etherealmber.atlascommerce
          timeout 30s adb shell am start -W -n ma.etherealmber.atlascommerce/.MainActivity > api28-release-start.txt 2>&1
          start_status=$?
          adb shell rm -f /sdcard/api28-window.xml
          rm -f api28-release-window.xml
          reached_first_run=false
          for attempt in $(seq 1 12); do
            timeout 8s adb shell uiautomator dump /sdcard/api28-window.xml >/dev/null 2>&1 || true
            timeout 8s adb pull /sdcard/api28-window.xml api28-release-window.xml >/dev/null 2>&1 || true
            if grep -Eq 'Bienvenue sur Atlas|Welcome to Atlas|مرحباً بك في Atlas|Aperçu interactif|Interactive preview|معاينة تفاعلية' api28-release-window.xml 2>/dev/null; then
              reached_first_run=true
              break
            fi
            sleep 5
          done
          if [ "$start_status" -ne 0 ] || [ "$reached_first_run" != "true" ]; then
            echo "Minified release APK failed the Android 9 cold-start check."
            cat api28-release-start.txt || true
            adb shell dumpsys activity activities > api28-activities.txt 2>&1
            smoke_status=1
          else
            echo "Minified release APK reached onboarding on Android 9 (API 28)."
          fi
        fi
      fi
    fi
  fi

adb logcat -d -v threadtime > api28-logcat.txt 2>&1
adb shell dumpsys activity activities > api28-activities.txt 2>&1

if [ "$smoke_status" -ne 0 ]; then
  echo "=== Android 9 startup, build and app diagnostics ==="
  test_details=$(grep -E -i "FAILURE:|FAILED|Tests run:|Timed out|AssertionError|NoSuchMethodError|NoClassDefFoundError|AndroidJUnitRunner|ma\.etherealmber\.atlascommerce|^e: |error:|Unresolved reference|Type mismatch|Cannot infer|Compilation error|Minified release APK" api28-gradle.log api28-release-gradle.log api28-release-start.txt 2>/dev/null | tail -n 35 || true)
  app_details=$(grep -A 40 -B 5 -E "FATAL EXCEPTION|ANR in ma\.etherealmber\.atlascommerce" api28-logcat.txt | tail -n 55 || true)
  app_process=$(grep -E -i "Android9StartupSmoke|Process: ma\.etherealmber\.atlascommerce|ma\.etherealmber\.atlascommerce" api28-logcat.txt | tail -n 12 || true)
  window_details=$(grep -oE '.{0,100}(Bienvenue sur Atlas|Welcome to Atlas|مرحباً بك في Atlas|Aperçu interactif|Interactive preview|معاينة تفاعلية|Atlas Commerce|Not responding|has stopped).{0,180}' api28-release-window.xml 2>/dev/null | head -n 8 || true)
  matches=$(printf '%s\n%s\n%s\n%s' "$test_details" "$app_details" "$app_process" "$window_details" | sed '/^[[:space:]]*$/d')
  if [ -z "$matches" ]; then
    matches=$(tail -n 50 api28-gradle.log 2>/dev/null || true)
  fi
  printf '%s\n' "$matches"
  summary=$(printf '%s' "$matches" | tr '\n' ' ' | cut -c1-6500)
  echo "::error title=Android 9 startup diagnostics::$summary"
  echo "=== Activity state ==="
  cat api28-activities.txt || true
fi

exit "$smoke_status"
