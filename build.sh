#!/usr/bin/env bash
# Builds a signed debug APK with the plain Android SDK tools (no Gradle needed).
set -euo pipefail
cd "$(dirname "$0")"

SDK="${LOCALAPPDATA}/Android/Sdk"
BT="$SDK/build-tools/34.0.0"
PLATFORM="$SDK/platforms/android-34/android.jar"
AAPT2="$BT/aapt2.exe"
D8_JAR="$BT/lib/d8.jar"
SIGNER_JAR="$BT/lib/apksigner.jar"
# the SDK copy of zipalign is sometimes blocked in place; run a local copy instead
ZIPALIGN="tools/bin/zipalign.exe"
if [ ! -f "$ZIPALIGN" ]; then
  mkdir -p tools/bin
  cp "$BT/zipalign.exe" "$ZIPALIGN"
fi
OUT=build

for f in "$AAPT2" "$PLATFORM" "$D8_JAR" "$SIGNER_JAR" "$ZIPALIGN"; do
  [ -f "$f" ] || { echo "missing: $f"; exit 1; }
done

# Clean only the build products, never build/shots (preview renders) or
# build/test (compiled test harness) - those are inputs for verification.
rm -rf "$OUT/res" "$OUT/gen" "$OUT/classes" "$OUT/dex"
rm -f "$OUT/res.zip" "$OUT/base.apk" "$OUT/packed.apk" "$OUT/aligned.apk" \
      "$OUT/AngryBirds.apk" "$OUT/AngryBirds.apk.idsig" "$OUT/classes.txt" \
      "$OUT/sources.txt"
mkdir -p "$OUT/res" "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "[1/6] aapt2 compile"
"$AAPT2" compile --dir res -o "$OUT/res.zip"

echo "[2/6] aapt2 link"
"$AAPT2" link \
  -o "$OUT/base.apk" \
  -I "$PLATFORM" \
  --manifest AndroidManifest.xml \
  -A assets \
  --java "$OUT/gen" \
  --min-sdk-version 21 \
  --target-sdk-version 34 \
  -0 .mp3 \
  "$OUT/res.zip"

echo "[3/6] javac"
find src "$OUT/gen" -name "*.java" > "$OUT/sources.txt"
javac -source 8 -target 8 -nowarn -encoding UTF-8 \
  -bootclasspath "$PLATFORM" \
  -d "$OUT/classes" @"$OUT/sources.txt" 2>&1 | grep -v "^warning:" | grep -v "bootstrap class path" || true

echo "[4/6] d8"
find "$OUT/classes" -name "*.class" > "$OUT/classes.txt"
java -cp "$D8_JAR" com.android.tools.r8.D8 \
  --release --lib "$PLATFORM" --min-api 21 \
  --output "$OUT/dex" @"$OUT/classes.txt"

echo "[5/6] package"
python tools/add_dex.py "$OUT/base.apk" "$OUT/dex/classes.dex" "$OUT/packed.apk"
# zipalign.exe cannot be spawned in this environment, so align in Python.
python tools/zipalign.py "$OUT/packed.apk" "$OUT/aligned.apk"

if [ ! -f debug.keystore ]; then
  echo "[key] creating debug keystore"
  keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
    -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" \
    -keyalg RSA -keysize 2048 -validity 10000 > /dev/null 2>&1
fi

echo "[6/6] sign"
java -jar "$SIGNER_JAR" sign \
  --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
  --out "$OUT/AngryBirds.apk" "$OUT/aligned.apk"

java -jar "$SIGNER_JAR" verify --print-certs "$OUT/AngryBirds.apk" | head -3
echo "OK -> $OUT/AngryBirds.apk  ($(du -h "$OUT/AngryBirds.apk" | cut -f1))"
