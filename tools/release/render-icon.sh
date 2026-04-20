#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SVG="$ROOT_DIR/assets/jagfx-icon.svg"
PNG="$ROOT_DIR/assets/jagfx-icon.png"
ICO="$ROOT_DIR/assets/jagfx-icon.ico"
ICNS="$ROOT_DIR/assets/jagfx-icon.icns"

if [[ ! -f "$SVG" ]]; then
  echo "missing source icon: $SVG"
  exit 1
fi

render_png_with_quicklook() {
  local out_dir
  out_dir="$(mktemp -d /tmp/jagfx-icon.XXXXXX)"
  trap 'rm -rf "$out_dir"' RETURN

  qlmanage -t -s 1024 -o "$out_dir" "$SVG" >/dev/null 2>&1
  magick "$out_dir/$(basename "$SVG").png" -alpha set -fuzz 4% -transparent white "$PNG"
}

render_png_with_magick() {
  magick -background none "$SVG" -resize 1024x1024 "$PNG"
}

if ! command -v magick >/dev/null 2>&1; then
  echo "magick not found; cannot generate raster icon assets"
  exit 1
fi

if command -v qlmanage >/dev/null 2>&1; then
  render_png_with_quicklook
else
  echo "qlmanage not found; falling back to ImageMagick SVG render"
  render_png_with_magick
fi

magick "$PNG" -define icon:auto-resize=256,128,64,48,32,16 "$ICO"

if command -v sips >/dev/null 2>&1 && command -v iconutil >/dev/null 2>&1; then
  icon_tmp="$(mktemp -d /tmp/jagfx-icon.XXXXXX)"
  iconset="$icon_tmp/jagfx-icon.iconset"
  mkdir -p "$iconset"
  trap 'rm -rf "$icon_tmp"' EXIT

  for size in 16 32 128 256 512; do
    sips -z "$size" "$size" "$PNG" --out "$iconset/icon_${size}x${size}.png" >/dev/null
    doubled=$((size * 2))
    sips -z "$doubled" "$doubled" "$PNG" --out "$iconset/icon_${size}x${size}@2x.png" >/dev/null
  done

  iconutil -c icns "$iconset" -o "$ICNS"
else
  echo "sips/iconutil not found; leaving existing .icns unchanged"
fi

echo "rendered $PNG"
echo "rendered $ICO"
echo "rendered $ICNS"
