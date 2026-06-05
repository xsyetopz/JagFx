#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "usage: $0 <app-path> <output-dmg> <volume-name>"
  exit 1
fi

APP_PATH="$1"
OUTPUT_DMG="$2"
VOLUME_NAME="$3"
VOLUME_PATH="/Volumes/$VOLUME_NAME"

if [[ ! -d "$APP_PATH" ]]; then
  echo "app bundle not found: $APP_PATH"
  exit 1
fi

OUTPUT_DIR="$(dirname "$OUTPUT_DMG")"
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DMG"

TMP_ROOT="$OUTPUT_DIR/.dmg-tmp"
mkdir -p "$TMP_ROOT"
WORK_TMP="$(mktemp -d "$TMP_ROOT/create-dmg.XXXXXX")"
trap 'rm -rf "$WORK_TMP"' EXIT
export TMPDIR="$WORK_TMP/"

if mount | grep -Fq "on $VOLUME_PATH ("; then
  hdiutil detach "$VOLUME_PATH" -force >/dev/null 2>&1 || true
fi

DMG_ROOT="$WORK_TMP/dmg-root"
mkdir -p "$DMG_ROOT"
ditto "$APP_PATH" "$DMG_ROOT/$(basename "$APP_PATH")"
ln -s /Applications "$DMG_ROOT/Applications"

SOURCE_KIB="$(du -sk "$DMG_ROOT" | awk '{print $1}')"
SOURCE_MB="$(( (SOURCE_KIB + 1023) / 1024 ))"
SIZE_MB="$(( ((SOURCE_MB * 16) + 9) / 10 + 64 ))"
if (( SIZE_MB < 256 )); then
  SIZE_MB=256
fi

create_dmg() {
  local size_mb="$1"
  hdiutil create \
    -size "${size_mb}m" \
    -fs APFS \
    -volname "$VOLUME_NAME" \
    -srcfolder "$DMG_ROOT" \
    -ov \
    -format UDZO \
    "$OUTPUT_DMG"
}

if ! create_dmg "$SIZE_MB"; then
  RETRY_SIZE_MB="$(( SIZE_MB + 256 ))"
  rm -f "$OUTPUT_DMG"
  echo "retrying DMG create with larger size: ${RETRY_SIZE_MB}m"
  create_dmg "$RETRY_SIZE_MB"
fi

echo "created $OUTPUT_DMG"
