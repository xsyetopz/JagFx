#!/usr/bin/env bash
# scripts/notarize-macos.sh
#
# Build, sign, notarize, staple, and package JagFx.Desktop as a macOS DMG.
#
# Usage:
#   ./scripts/notarize-macos.sh [arch]
#
#   arch  osx-arm64 (default, Apple Silicon) | osx-x64 (Intel)
#
# Prerequisites:
#   1. Copy .env.example → .env and fill in your credentials.
#   2. Run `xcrun notarytool store-credentials` once to create the keychain
#      profile referenced by APPLE_NOTARIZE_PROFILE in .env.
#   3. .NET 8 SDK installed.
#   4. Xcode Command Line Tools installed (codesign, hdiutil, xcrun).

set -euo pipefail

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
: "${APPLE_NOTARIZE_PROFILE:?APPLE_NOTARIZE_PROFILE is not set in .env}"

# -- Arguments ----------------------------------------------------------------
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

# Verify the signing certificate exists in the keychain
security find-identity -v -p codesigning 2>/dev/null | grep -qF "$APPLE_SIGNING_IDENTITY" \
    || die "Signing certificate not found in keychain (check APPLE_SIGNING_IDENTITY in .env)"
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

# -- Code sign ----------------------------------------------------------------
step "Unlocking keychain"
# codesign needs the keychain unlocked AND apple-tool:/apple: partition access
# on the private key. Without set-key-partition-list, codesign gets
# errSecInternalComponent even when the keychain is unlocked.
# Read password once; unset immediately after use.
if [[ -z "${KEYCHAIN_PASSWORD:-}" ]]; then
    read -rsp "Keychain password: " KEYCHAIN_PASSWORD
    echo
    _clear_kp=true
fi

KEYCHAIN="$HOME/Library/Keychains/login.keychain-db"
security unlock-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN" \
    || die "Could not unlock keychain"
security set-key-partition-list -S apple-tool:,apple: -s \
    -k "$KEYCHAIN_PASSWORD" "$KEYCHAIN" 2>/dev/null \
    || die "Could not set key partition list — check your keychain password"

[[ "${_clear_kp:-}" == "true" ]] && unset KEYCHAIN_PASSWORD _clear_kp
ok "Keychain ready"

step "Code signing"

# Filter signing identity out of codesign output so it never appears in the terminal.
# Errors and warnings are still shown -- only lines containing the identity are dropped.
_sign() {
    codesign "$@" 2>&1 | grep -vF "$APPLE_SIGNING_IDENTITY" || return "${PIPESTATUS[0]}"
}

# Sign inside-out: native dylibs first, then the bundle.
# --deep is avoided because it re-signs already-signed third-party dylibs
# in a single pass, causing errSecInternalComponent on libs like libSkiaSharp.
while IFS= read -r -d '' lib; do
    _sign --force --sign "$APPLE_SIGNING_IDENTITY" --timestamp "$lib" || \
        warn "Could not sign $(basename "$lib") -- continuing"
done < <(find "$APP_BUNDLE" -name "*.dylib" -print0)

# Sign the bundle itself (no --deep -- sub-components are already signed above)
_sign \
    --force \
    --options runtime \
    --entitlements "$ENTITLEMENTS" \
    --sign "$APPLE_SIGNING_IDENTITY" \
    --timestamp \
    "$APP_BUNDLE"
ok "Signed"

# Verify (output suppressed -- codesign --display prints the identity)
codesign --verify --deep --strict "$APP_BUNDLE" 2>/dev/null \
    && ok "Signature verified" \
    || die "Signature verification failed"

# -- Create DMG ---------------------------------------------------------------
step "Creating DMG"
rm -f "$DMG_PATH"

# Staging directory for DMG contents
STAGING_DIR="$(mktemp -d)"
trap 'rm -rf "$STAGING_DIR"' EXIT

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
    --sign "$APPLE_SIGNING_IDENTITY" \
    --timestamp \
    "$DMG_PATH" 2>/dev/null
ok "DMG signed"

# -- Notarize -----------------------------------------------------------------
step "Submitting to Apple Notary Service (this may take a few minutes)"
xcrun notarytool submit "$DMG_PATH" \
    --keychain-profile "$APPLE_NOTARIZE_PROFILE" \
    --wait 2>&1 | grep -Ev "(profile|team|apple-id|credentials)"
ok "Notarization approved"

# -- Staple -------------------------------------------------------------------
step "Stapling notarization ticket"
xcrun stapler staple "$DMG_PATH"
ok "Stapled"

# Verify gatekeeper will accept it
spctl --assess --type open --context context:primary-signature -v "$DMG_PATH" \
    && ok "Gatekeeper check passed" \
    || warn "Gatekeeper check reported an issue -- review output above"

# -- Done ---------------------------------------------------------------------
echo -e "\n${GREEN}${BOLD}✔ Done!${RESET}"
echo -e "  Distributable DMG: ${BOLD}$DMG_PATH${RESET}"
