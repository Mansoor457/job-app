#!/usr/bin/env bash
# Build JobTracker APK from app/ assets + apkbuild/ sources.
# Usage: bash BUILD_APK.sh [versionCode]   (default 1)
# Requires: JDK 8+, Android SDK with platforms;android-34 + build-tools;34.0.0
# SDK lookup: $ANDROID_HOME, else ./android-sdk next to this script.
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
VC="${1:-1}"
SDK="${ANDROID_HOME:-$ROOT/android-sdk}"
BD="$SDK/build-tools/34.0.0"
AJ="$SDK/platforms/android-34/android.jar"
[ -x "$BD/aapt2" ] || { echo "Missing build-tools 34 at $BD"; echo "Install: sdkmanager --sdk_root=$SDK 'platforms;android-34' 'build-tools;34.0.0'"; exit 1; }
W="$ROOT/apkbuild"
OUT="$ROOT/out-apk"; rm -rf "$OUT"; mkdir -p "$OUT/obj" "$OUT/assets" "$OUT/res/mipmap"

# 1) assets = current web app + tracker
cp "$ROOT/app/index.html" "$ROOT/app/manifest.webmanifest" "$ROOT/app/sw.js" \
   "$ROOT/app/job_tracker.csv" "$ROOT/app/icon-192.png" "$ROOT/app/icon-512.png" "$OUT/assets/"
cp "$ROOT/app/icon-192.png" "$OUT/res/mipmap/ic_launcher.png"

# 2) compile + dex
javac -source 8 -target 8 -nowarn -Xlint:-options -bootclasspath "$AJ" -d "$OUT/obj" "$W/MainActivity.java"
"$BD/d8" --min-api 24 --output "$OUT/obj" "$OUT/obj"/pv/mansoor/jobtracker/*.class

# 3) resources + link
"$BD/aapt2" compile --dir "$OUT/res" -o "$OUT/res.zip"
"$BD/aapt2" link -o "$OUT/base.apk" -I "$AJ" --manifest "$W/AndroidManifest.xml" \
  --min-sdk-version 24 --target-sdk-version 34 --version-code "$VC" --version-name "1.$((VC-1))" \
  -A "$OUT/assets" "$OUT/res.zip"
(cd "$OUT/obj" && zip -q -0 ../base.apk classes.dex)
"$BD/zipalign" -f 4 "$OUT/base.apk" "$OUT/aligned.apk"

# 4) sign (ALWAYS same key so installs upgrade in place)
"$BD/apksigner" sign --key "$W/key.pk8" --cert "$W/cert.pem" --out "$ROOT/JobTracker-v$VC.apk" "$OUT/aligned.apk"
"$BD/apksigner" verify "$ROOT/JobTracker-v$VC.apk"
echo "OK: $ROOT/JobTracker-v$VC.apk ($(stat -c%s "$ROOT/JobTracker-v$VC.apk") bytes)"
