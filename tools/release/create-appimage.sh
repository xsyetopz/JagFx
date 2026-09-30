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

for tool in appimagetool patchelf ldconfig ldd sha256sum; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "$tool not found on PATH"
    exit 1
  fi
done

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APPDIR="$(mktemp -d /tmp/jagfx-appdir.XXXXXX)"
trap 'rm -rf "$APPDIR"' EXIT
# mktemp -d creates the directory as 0700, and appimagetool keeps that mode on the
# image root. Sandboxes such as firejail mount the image as root, so that mode denies access.
chmod 755 "$APPDIR"

mkdir -p "$APPDIR/usr/bin" "$APPDIR/usr/lib"
cp "$BIN_PATH" "$APPDIR/usr/bin/jagfx"
chmod +x "$APPDIR/usr/bin/jagfx"

# Bundle the C library, its loader, and every library the app loads, so the
# AppImage does not depend on the host glibc. Avalonia (X11), SkiaSharp, and
# .NET (ICU) load these with dlopen, so ldd on the binary does not list them.
# The GL, Vulkan, and GTK stacks stay on the host: a host GL driver that needs a
# newer glibc fails to load and Avalonia falls back to software rendering.
DLOPEN_LIBS=(
  libX11.so.6 libXcursor.so.1 libXext.so.6 libXi.so.6 libXrandr.so.2
  libICE.so.6 libSM.so.6 libfontconfig.so.1 libdbus-1.so.3 libz.so.1
)
ICU_UC="$(ldconfig -p | awk '$1 ~ /^libicuuc\.so\.[0-9]+$/ && /x86-64/ { print $1 }' | sort -V | tail -n 1)"
if [[ -z "$ICU_UC" ]]; then
  echo "ICU (libicuuc.so.N) not found; install libicu"
  exit 1
fi
DLOPEN_LIBS+=("$ICU_UC" "${ICU_UC/libicuuc/libicui18n}")

declare -A BUNDLED=()
bundle_deps() { # <elf>: record the elf's full dependency closure as soname -> path
  local deps name path
  deps="$(ldd "$1")"
  if grep -q 'not found' <<<"$deps"; then
    echo "unresolved dependencies of $1:"
    grep 'not found' <<<"$deps"
    exit 1
  fi
  while read -r name path; do
    BUNDLED[$name]="$path"
  done < <(awk '
    $2 == "=>" && $3 ~ /^\// { print $1, $3; next }
    $1 ~ /^\// { n = split($1, parts, "/"); print parts[n], $1 }' <<<"$deps")
}

bundle_deps "$BIN_PATH"
for soname in "${DLOPEN_LIBS[@]}"; do
  path="$(ldconfig -p | awk -v n="$soname" '$1 == n && /x86-64/ { print $NF; exit }')"
  if [[ -z "$path" ]]; then
    echo "library not found on the build host: $soname"
    exit 1
  fi
  BUNDLED[$soname]="$path"
  bundle_deps "$path"
done

for soname in "${!BUNDLED[@]}"; do
  cp -L "${BUNDLED[$soname]}" "$APPDIR/usr/lib/$soname"
done
LOADER=ld-linux-x86-64.so.2
for required in "$LOADER" libc.so.6; do
  if [[ ! -f "$APPDIR/usr/lib/$required" ]]; then
    echo "$required was not bundled"
    exit 1
  fi
done
chmod 755 "$APPDIR/usr/lib/$LOADER"

# The .NET single-file host reads its bundle through /proc/self/exe, so the app
# cannot be started as "ld-linux.so jagfx". The binary's interpreter is instead
# set to a fixed /tmp path that AppRun links to the bundled loader. The name
# carries a hash of the bundled glibc, so every link of that name points to an
# identical loader. RPATH (not RUNPATH) also applies to the dependencies of
# libraries loaded with dlopen. A relative interpreter would resolve against the
# working directory, and LD_LIBRARY_PATH would reach child processes such as
# aplay, which run with the host loader.
GLIBC_ID="$(cat "$APPDIR/usr/lib/$LOADER" "$APPDIR/usr/lib/libc.so.6" | sha256sum | cut -c1-12)"
INTERP="/tmp/.jagfx-ld-$GLIBC_ID"
# shellcheck disable=SC2016 # $ORIGIN is expanded by the loader, not the shell
patchelf --set-interpreter "$INTERP" --force-rpath --set-rpath '$ORIGIN/../lib' "$APPDIR/usr/bin/jagfx"

# POSIX sh: hosts such as Alpine have no bash. Anyone can create names in /tmp,
# so use the link only when this user owns it and it points to this loader.
# The sticky bit on /tmp keeps other users from replacing it after the check.
cat >"$APPDIR/AppRun" <<'EOF'
#!/bin/sh
set -eu
APPDIR="${APPDIR:-$(dirname "$(readlink -f "$0")")}"
loader="$APPDIR/usr/lib/ld-linux-x86-64.so.2"
link="@INTERP@"
uid="$(id -u)"
link_ok() {
  [ -L "$link" ] && [ "$(stat -c %u "$link")" = "$uid" ] && [ "$(readlink "$link")" = "$loader" ]
}
if ! link_ok; then
  ln -s "$loader" "$link.$$"
  mv -f "$link.$$" "$link" 2>/dev/null || rm -f "$link.$$"
fi
if ! link_ok; then
  echo "JagFx: $link exists but is not your link to $loader; remove it and try again" >&2
  exit 1
fi
exec "$APPDIR/usr/bin/jagfx" "$@"
EOF
sed -i "s|@INTERP@|$INTERP|" "$APPDIR/AppRun"
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
