#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "usage: $0 <desktop-binary> <output-appimage> <version>"
  exit 1
fi

BIN_PATH="$1"
OUTPUT_APPIMAGE="$2"
VERSION="$3"

if [[ ! -f "$BIN_PATH" ]]; then
  echo "desktop binary not found: $BIN_PATH"
  exit 1
fi

if ! command -v appimagetool >/dev/null 2>&1; then
  echo "appimagetool not found on PATH"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APPDIR="$(mktemp -d /tmp/jagfx-appdir.XXXXXX)"
trap 'rm -rf "$APPDIR"' EXIT

mkdir -p "$APPDIR/usr/bin"
cp "$BIN_PATH" "$APPDIR/usr/bin/jagfx"
chmod +x "$APPDIR/usr/bin/jagfx"

cat >"$APPDIR/AppRun" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
exec "$APPDIR/usr/bin/jagfx" "$@"
EOF
chmod +x "$APPDIR/AppRun"

cat >"$APPDIR/jagfx.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=JagFx
Exec=jagfx
Icon=jagfx
Categories=AudioVideo;Audio;
Comment=Jagex Synth Editor
Terminal=false
EOF

ICON="$ROOT_DIR/assets/jagfx-icon.png"
if [[ -f "$ICON" ]]; then
  cp "$ICON" "$APPDIR/jagfx.png"
fi

mkdir -p "$(dirname "$OUTPUT_APPIMAGE")"
rm -f "$OUTPUT_APPIMAGE"

ARCH=x86_64 VERSION="$VERSION" appimagetool "$APPDIR" "$OUTPUT_APPIMAGE"
chmod +x "$OUTPUT_APPIMAGE"

echo "created $OUTPUT_APPIMAGE"
