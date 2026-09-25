#!/usr/bin/env bash
set -euo pipefail
time -p cd "$(dirname "$0")"
/usr/bin/time -p pwd
PORT="${PORT:-3000}"
/usr/bin/time -p echo "starting static server on PORT=$PORT"
/usr/bin/time -p python3 --version
/usr/bin/time -p node --version
ROOT="$(pwd)"
STATIC_DIR="$ROOT"
if /usr/bin/time -p test -f "$ROOT/package.json"; then
  /usr/bin/time -p npm install --no-audit --no-fund
  if /usr/bin/time -p test -n "$(node -p "(require('./package.json').scripts||{}).build || ''")"; then
    /usr/bin/time -p npm run build
  fi
  if /usr/bin/time -p test -d "$ROOT/dist"; then
    STATIC_DIR="$ROOT/dist"
  fi
fi
/usr/bin/time -p test -f "$STATIC_DIR/index.html"
WEB_DIR="${OPENCODE_WEB_DIR:-/home/runner/work/_temp/omgithub-web}"
/usr/bin/time -p mkdir -p "$WEB_DIR"
/usr/bin/time -p sh -c 'printf "{\"project\":\"/home/runner/work/PlayGround/PlayGround\",\"directory\":\"%s\"}" "$1" > "$2/deployment-output.json"' _ "$STATIC_DIR" "$WEB_DIR"
/usr/bin/time -p cat "$WEB_DIR/deployment-output.json"
/usr/bin/time -p echo "serving $STATIC_DIR on $PORT"
/usr/bin/time -p python3 -m http.server "$PORT" --directory "$STATIC_DIR"
