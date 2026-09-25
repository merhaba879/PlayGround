#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
/usr/bin/time -p bash -c 'PROJECT_ROOT="$(pwd)"; echo "project root: $PROJECT_ROOT"'
PROJECT_ROOT="$(pwd)"
/usr/bin/time -p test -d "$PROJECT_ROOT/dist"
PORT="${PORT:-3000}"
export PORT
STATIC_DIR="$PROJECT_ROOT/dist"
WEB_DIR="${OPENCODE_WEB_DIR:-/home/runner/work/_temp/omgithub-web}"
/usr/bin/time -p mkdir -p "$WEB_DIR"
/usr/bin/time -p bash -c 'if [ -f package.json ]; then npm install --no-audit --no-fund; else echo "no package.json, skip install"; fi'
/usr/bin/time -p test -f "$STATIC_DIR/index.html"
/usr/bin/time -p bash -c 'if [ -f mt-manager-clone-v3.2.apk ] && [ mt-manager-clone-v3.2.apk -nt dist/mt-manager-clone-v3.2.apk ]; then cp -f mt-manager-clone-v3.2.apk dist/; echo "apk synced"; else echo "apk up to date"; fi'
/usr/bin/time -p bash -c 'PROJECT_ROOT="$(pwd)"; STATIC_DIR="$PROJECT_ROOT/dist"; WEB_DIR="${OPENCODE_WEB_DIR:-/home/runner/work/_temp/omgithub-web}"; JSON=$(printf "{\"project\":\"%s\",\"directory\":\"%s\"}" "$PROJECT_ROOT" "$STATIC_DIR"); echo "$JSON" > "$WEB_DIR/deployment-output.json"; echo "$JSON" > /home/runner/work/_temp/omgithub-web/deployment-output.json; cat "$WEB_DIR/deployment-output.json"'
/usr/bin/time -p python3 --version
exec /usr/bin/time -p python3 -m http.server "$PORT" --directory "$STATIC_DIR" --bind 0.0.0.0
