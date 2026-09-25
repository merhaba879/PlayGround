#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK=/usr/local/lib/android/sdk
BT=$SDK/build-tools/34.0.0
AJAR=$SDK/platforms/android-34/android.jar
OUT=/tmp/mt
rm -rf $OUT
mkdir -p $OUT/classes $OUT/dex
echo "[1/6] javac..."
find src -name "*.java" > $OUT/sources.txt
cat $OUT/sources.txt
javac -source 8 -target 8 -cp "$AJAR" -d $OUT/classes @"$OUT/sources.txt"
echo "[2/6] d8..."
$BT/d8 --lib "$AJAR" --min-api 24 --output $OUT/dex/ $(find $OUT/classes -name "*.class")
ls -lh $OUT/dex/
echo "[3/6] aapt2 link..."
$BT/aapt2 link -o $OUT/unsigned.apk -I "$AJAR" --manifest AndroidManifest.xml --min-sdk-version 24 --target-sdk-version 34 --version-code 5 --version-name 3.2
ls -lh $OUT/unsigned.apk
echo "[4/6] add dex..."
python3 - <<'PY'
import zipfile, pathlib, shutil
dex_dir="/tmp/mt/dex"
apk_in="/tmp/mt/unsigned.apk"
apk_out="/tmp/mt/app-unsigned.apk"
shutil.copy(apk_in, apk_out)
# d8 outputs classes.dex (+classes2.dex if multidex)
import os
dex_files=[os.path.join(dex_dir,f) for f in os.listdir(dex_dir) if f.endswith(".dex")]
print("dex:",dex_files)
with zipfile.ZipFile(apk_out,'a',zipfile.ZIP_DEFLATED) as z:
    for df in dex_files:
        arc=os.path.basename(df)
        z.write(df,arc)
print("added")
PY
ls -lh $OUT/
echo "[5/6] zipalign..."
$BT/zipalign -f 4 $OUT/app-unsigned.apk $OUT/app-aligned.apk
echo "[6/6] sign..."
if [ ! -f $OUT/debug.keystore ]; then
  keytool -genkeypair -keystore $OUT/debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10950 -dname "CN=Android Debug,O=Android,C=US"
fi
$BT/apksigner sign --ks $OUT/debug.keystore --ks-pass pass:android --key-pass pass:android --out $OUT/mt-manager-clone-v3.2.apk $OUT/app-aligned.apk
$BT/apksigner verify --print-certs $OUT/mt-manager-clone-v3.2.apk | head -n 20
ls -lh $OUT/*.apk
echo "DONE: $OUT/mt-manager-clone-v3.2.apk"
