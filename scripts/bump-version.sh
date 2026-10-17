#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <version>" >&2
  exit 2
fi

version="$1"
if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "version must be major.minor.patch" >&2
  exit 2
fi

if [[ ! -f CHANGELOG.md ]]; then
  echo "CHANGELOG.md not found" >&2
  exit 1
fi

first_entry="$(grep -m1 -E '^## \[[0-9]+\.[0-9]+\.[0-9]+\]' CHANGELOG.md | sed -E 's/^## \[([^]]+)\].*/\1/')"
if [[ "$first_entry" != "$version" ]]; then
  echo "CHANGELOG.md first entry must be $version before bumping versions (found: ${first_entry:-none})" >&2
  exit 1
fi

python3 - "$version" <<'PY'
from pathlib import Path
import json
import re
import sys

version = sys.argv[1]


def replace(path, pattern, value, expected=1):
    file = Path(path)
    text, count = re.subn(pattern, value, file.read_text(), flags=re.MULTILINE)
    if count != expected:
        raise SystemExit(f"expected {expected} match(es) of {pattern!r} in {path}, found {count}")
    file.write_text(text)


replace("gradle.properties", r"^version=.*$", f"version={version}")

for package_json in sorted(Path("packages").glob("*/package.json")):
    payload = json.loads(package_json.read_text())
    if "version" not in payload:
        raise SystemExit(f"missing version in {package_json}")
    payload["version"] = version
    package_json.write_text(json.dumps(payload, indent=2) + "\n")
PY
