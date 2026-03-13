#!/usr/bin/env bash
# scripts/notarize-macos.sh
#
# Build, sign, notarize, staple, and package JagFx.Desktop as a macOS DMG.
#
# Usage:
#   ./scripts/notarize-macos.sh [--skip-notarize] [arch]
#
#   --skip-notarize  Build, sign, and package the DMG but skip notarization/stapling
#   arch             osx-arm64 (default, Apple Silicon) | osx-x64 (Intel)
#
# Prerequisites:
#   1. Copy .env.example → .env and fill in your credentials.
#   2. Run `xcrun notarytool store-credentials` once to create the keychain
#      profile referenced by APPLE_NOTARIZE_PROFILE in .env.
#   3. .NET 8 SDK installed.
#   4. Xcode Command Line Tools installed (codesign, hdiutil, xcrun).

set -euo pipefail
trap 'echo -e "${RED}Script failed at line $LINENO${RESET}" >&2' ERR

# -- Colours ------------------------------------------------------------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

step()  { echo -e "\n${CYAN}${BOLD}▶ $*${RESET}"; }
ok()    { echo -e "${GREEN}✔ $*${RESET}"; }
warn()  { echo -e "${YELLOW}⚠ $*${RESET}"; }
die()   { echo -e "${RED}✘ $*${RESET}" >&2; exit 1; }

# -- Resolve paths ------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# -- Load .env ----------------------------------------------------------------
ENV_FILE="$ROOT_DIR/.env"
if [[ -f "$ENV_FILE" ]]; then
    # shellcheck disable=SC1090
    set -o allexport; source "$ENV_FILE"; set +o allexport
    ok "Loaded $ENV_FILE"
else
    die ".env not found. Copy .env.example → .env and fill in your credentials."
fi

# -- Required variables -------------------------------------------------------
: "${APPLE_SIGNING_IDENTITY:?APPLE_SIGNING_IDENTITY is not set in .env}"
: "${APPLE_TEAM_ID:?APPLE_TEAM_ID is not set in .env}"
if [[ "$SKIP_NOTARIZE" == false ]]; then
    : "${APPLE_NOTARIZE_PROFILE:?APPLE_NOTARIZE_PROFILE is not set in .env}"
fi

# -- Arguments ----------------------------------------------------------------
SKIP_NOTARIZE=false
if [[ "${1:-}" == "--skip-notarize" ]]; then
    SKIP_NOTARIZE=true
    shift
fi

ARCH="${1:-osx-arm64}"
[[ "$ARCH" == "osx-arm64" || "$ARCH" == "osx-x64" ]] \
    || die "Unknown arch '$ARCH'. Use osx-arm64 or osx-x64."

# -- Derived paths ------------------------------------------------------------
PUBLISH_DIR="$ROOT_DIR/publish/$ARCH"
APP_BUNDLE="$PUBLISH_DIR/JagFx.app"
ENTITLEMENTS="$ROOT_DIR/src/JagFx.Desktop/macOS/entitlements.plist"
DESKTOP_PROJ="$ROOT_DIR/src/JagFx.Desktop"

# Read version from Directory.Build.props
VERSION=$(grep -oE '<Version>[^<]+' "$ROOT_DIR/Directory.Build.props" | head -1 | sed 's/<Version>//')
APP_NAME="JagFx"
DMG_NAME="${APP_NAME}-${VERSION}-${ARCH}.dmg"
DMG_PATH="$ROOT_DIR/publish/$DMG_NAME"

echo -e "\n${BOLD}JagFx macOS Release Builder${RESET}"
echo "  Version : $VERSION"
echo "  Arch    : $ARCH"
echo "  Output  : $DMG_PATH"

# -- Preflight checks ---------------------------------------------------------
step "Preflight checks"
command -v dotnet   >/dev/null || die "dotnet not found"
command -v codesign >/dev/null || die "codesign not found (install Xcode CLT)"
command -v hdiutil  >/dev/null || die "hdiutil not found"
command -v xcrun    >/dev/null || die "xcrun not found (install Xcode CLT)"

[[ -f "$ENTITLEMENTS" ]] || die "Entitlements not found: $ENTITLEMENTS"

CERT_LINE=$(security find-identity -v -p codesigning 2>/dev/null \
    | grep -F "$APPLE_SIGNING_IDENTITY" | head -1)
[[ -n "$CERT_LINE" ]] \
    || die "Signing certificate not found in keychain (check APPLE_SIGNING_IDENTITY in .env)"
CERT_SHA=$(awk '{print $2}' <<< "$CERT_LINE")
[[ "$CERT_SHA" =~ ^[0-9A-Fa-f]{40}$ ]] \
    || die "Could not parse certificate SHA1 from keychain output"

# Verify the Apple intermediate certificate is present (needed for full chain)
security find-certificate -c "Developer ID Certification Authority" >/dev/null 2>&1 \
    || warn "Apple 'Developer ID Certification Authority' intermediate cert not found - install from Apple PKI if signing fails"

ok "All checks passed"

# -- Build ---------------------------------------------------------------------
step "Publishing $ARCH"
rm -rf "$PUBLISH_DIR" "$APP_BUNDLE"
dotnet publish "$DESKTOP_PROJ" \
    -c Release \
    -r "$ARCH" \
    --self-contained \
    -o "$PUBLISH_DIR" \
    --nologo \
    -v quiet
ok "Published to $PUBLISH_DIR"

# The BundleMacApp MSBuild target creates $APP_BUNDLE automatically.
[[ -d "$APP_BUNDLE" ]] || die ".app bundle not found at $APP_BUNDLE -- did the BundleMacApp target run?"
ok ".app bundle created"

# Cleanup on exit
trap 'rm -rf "${STAGING_DIR:-}"' EXIT

# -- Code sign ----------------------------------------------------------------
step "Code signing"

# Sign inside-out: native dylibs first, then the bundle.
# --deep is avoided because it re-signs already-signed third-party dylibs
# in a single pass, causing errSecInternalComponent on libs like libSkiaSharp.
while IFS= read -r -d '' bin; do
    codesign --force --sign "$CERT_SHA" --timestamp "$bin" || \
        warn "Could not sign $(basename "$bin") -- continuing"
done < <(find "$APP_BUNDLE/Contents/MacOS" -type f -print0)

# Sign the bundle itself (no --deep -- sub-components are already signed above)
if ! codesign \
    --force \
    --options runtime \
    --entitlements "$ENTITLEMENTS" \
    --sign "$CERT_SHA" \
    --timestamp \
    "$APP_BUNDLE" 2>&1; then
    die "Bundle signing failed (see codesign output above)"
fi
ok "Signed"

# Verify (output suppressed -- codesign --display prints the identity)
codesign --verify --deep --strict "$APP_BUNDLE" \
    && ok "Signature verified" \
    || die "Signature verification failed"

# -- Create DMG ---------------------------------------------------------------
step "Creating DMG"
rm -f "$DMG_PATH"

# Staging directory for DMG contents
STAGING_DIR="$(mktemp -d)"

cp -R "$APP_BUNDLE" "$STAGING_DIR/"
ln -s /Applications "$STAGING_DIR/Applications"

hdiutil create \
    -volname "$APP_NAME $VERSION" \
    -srcfolder "$STAGING_DIR" \
    -ov \
    -format UDZO \
    -imagekey zlib-level=9 \
    "$DMG_PATH"
ok "DMG created: $DMG_PATH"

# -- Sign the DMG -------------------------------------------------------------
step "Signing DMG"
codesign \
    --force \
    --sign "$CERT_SHA" \
    --timestamp \
    "$DMG_PATH" 2>/dev/null
ok "DMG signed"

# -- Notarize -----------------------------------------------------------------
if [[ "$SKIP_NOTARIZE" == true ]]; then
    warn "Skipping notarization (--skip-notarize)"
else
step "Submitting to Apple Notary Service"

NOTARIZE_TIMEOUT_MINUTES="${NOTARIZE_TIMEOUT_MINUTES:-30}"
NOTARIZE_POLL_INTERVAL="${NOTARIZE_POLL_INTERVAL:-30}"
NOTARIZE_MAX_RETRIES="${NOTARIZE_MAX_RETRIES:-5}"

# Submit without --wait so we control polling
SUBMIT_OUTPUT=$(xcrun notarytool submit "$DMG_PATH" \
    --keychain-profile "$APPLE_NOTARIZE_PROFILE" 2>&1)
echo "$SUBMIT_OUTPUT"

SUBMISSION_ID=$(grep -oE '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}' <<< "$SUBMIT_OUTPUT" | head -1)
[[ -n "$SUBMISSION_ID" ]] || die "Failed to parse submission ID from notarytool output"
ok "Submitted (ID: $SUBMISSION_ID)"

# Poll for completion
DEADLINE=$(( $(date +%s) + NOTARIZE_TIMEOUT_MINUTES * 60 ))
CONSECUTIVE_FAILURES=0

step "Polling for notarization status (timeout: ${NOTARIZE_TIMEOUT_MINUTES}m, interval: ${NOTARIZE_POLL_INTERVAL}s)"

while true; do
    NOW=$(date +%s)
    if (( NOW >= DEADLINE )); then
        echo ""
        warn "Notarization timed out after ${NOTARIZE_TIMEOUT_MINUTES} minutes."
        echo ""
        echo "The submission is likely still being processed by Apple."
        echo "Check status manually and staple when ready:"
        echo ""
        echo "  xcrun notarytool info $SUBMISSION_ID --keychain-profile $APPLE_NOTARIZE_PROFILE"
        echo "  xcrun stapler staple $DMG_PATH"
        echo ""
        die "Timed out waiting for notarization (submission ID: $SUBMISSION_ID)"
    fi
    REMAINING=$(( (DEADLINE - NOW) / 60 ))

    if INFO_OUTPUT=$(xcrun notarytool info "$SUBMISSION_ID" \
        --keychain-profile "$APPLE_NOTARIZE_PROFILE" 2>&1); then
        CONSECUTIVE_FAILURES=0
    else
        CONSECUTIVE_FAILURES=$(( CONSECUTIVE_FAILURES + 1 ))
        warn "Poll failed ($CONSECUTIVE_FAILURES/$NOTARIZE_MAX_RETRIES): $INFO_OUTPUT"
        if (( CONSECUTIVE_FAILURES >= NOTARIZE_MAX_RETRIES )); then
            die "Aborting after $NOTARIZE_MAX_RETRIES consecutive poll failures (submission ID: $SUBMISSION_ID)"
        fi
        sleep "$NOTARIZE_POLL_INTERVAL"
        continue
    fi

    STATUS=$(grep -i "status:" <<< "$INFO_OUTPUT" | head -1 | sed 's/.*status:[[:space:]]*//' | tr '[:upper:]' '[:lower:]' | xargs)
    echo "[$(date '+%H:%M:%S')] Status: $STATUS (${REMAINING}m remaining)"

    case "$STATUS" in
        accepted)
            break
            ;;
        invalid|rejected)
            warn "Fetching notarization log..."
            xcrun notarytool log "$SUBMISSION_ID" \
                --keychain-profile "$APPLE_NOTARIZE_PROFILE" 2>&1 || true
            die "Notarization $STATUS (submission ID: $SUBMISSION_ID)"
            ;;
        *)
            sleep "$NOTARIZE_POLL_INTERVAL"
            ;;
    esac
done

ok "Notarization approved"

# -- Staple -------------------------------------------------------------------
step "Stapling notarization ticket"
xcrun stapler staple "$DMG_PATH"
ok "Stapled"

# Verify gatekeeper will accept it
spctl --assess --type open --context context:primary-signature -v "$DMG_PATH" \
    && ok "Gatekeeper check passed" \
    || warn "Gatekeeper check reported an issue -- review output above"

fi # end skip-notarize guard

# -- Done ---------------------------------------------------------------------
echo -e "\n${GREEN}${BOLD}✔ Done!${RESET}"
echo -e "  Distributable DMG: ${BOLD}$DMG_PATH${RESET}"
