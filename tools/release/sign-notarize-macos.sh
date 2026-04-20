#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 <app-path> <dmg-path>"
  exit 1
fi

APP_PATH="$1"
DMG_PATH="$2"

required=(
  MACOS_SIGN_IDENTITY
  MACOS_NOTARY_APPLE_ID
  MACOS_NOTARY_TEAM_ID
  MACOS_NOTARY_PASSWORD
)

for key in "${required[@]}"; do
  if [[ -z "${!key:-}" ]]; then
    echo "missing env: $key"
    exit 1
  fi
done

codesign --verify --deep --strict --verbose=2 "$APP_PATH"
codesign --force --sign "$MACOS_SIGN_IDENTITY" "$DMG_PATH"

SUBMIT_LOG="$(mktemp)"
trap 'rm -f "$SUBMIT_LOG"' EXIT

if ! xcrun notarytool submit "$DMG_PATH" \
  --apple-id "$MACOS_NOTARY_APPLE_ID" \
  --team-id "$MACOS_NOTARY_TEAM_ID" \
  --password "$MACOS_NOTARY_PASSWORD" \
  --wait 2>&1 | tee "$SUBMIT_LOG"; then
  SUBMISSION_ID="$(awk '/^[[:space:]]*id: / {print $2; exit}' "$SUBMIT_LOG")"
  echo "notarytool submit failed"
  if [[ -n "$SUBMISSION_ID" ]]; then
    xcrun notarytool log "$SUBMISSION_ID" \
      --apple-id "$MACOS_NOTARY_APPLE_ID" \
      --team-id "$MACOS_NOTARY_TEAM_ID" \
      --password "$MACOS_NOTARY_PASSWORD"
  fi
  exit 1
fi

SUBMISSION_ID="$(awk '/^[[:space:]]*id: / {print $2; exit}' "$SUBMIT_LOG")"
STATUS="$(awk '/^[[:space:]]*status: / {print $2}' "$SUBMIT_LOG" | tail -1)"

if [[ "$STATUS" != "Accepted" ]]; then
  echo "notarization failed: ${STATUS:-unknown}"
  if [[ -n "$SUBMISSION_ID" ]]; then
    xcrun notarytool log "$SUBMISSION_ID" \
      --apple-id "$MACOS_NOTARY_APPLE_ID" \
      --team-id "$MACOS_NOTARY_TEAM_ID" \
      --password "$MACOS_NOTARY_PASSWORD"
  fi
  exit 1
fi

xcrun stapler staple "$DMG_PATH"
spctl --assess --type execute --verbose=4 "$APP_PATH"

echo "signed and notarized: $APP_PATH $DMG_PATH"
