#!/usr/bin/env bash
set -euo pipefail
time -p cd "$(dirname "$0")"
/usr/bin/time -p echo "capture: validating env"
/usr/bin/time -p test -n "${CAPTURE_URL:-}"
/usr/bin/time -p test -n "${CAPTURE_DIR:-}"
/usr/bin/time -p mkdir -p "$CAPTURE_DIR"
/usr/bin/time -p echo "capture: URL=$CAPTURE_URL DIR=$CAPTURE_DIR"
status=0
/usr/bin/time -p node "${RUNTIME_DIR:?}/scripts/default-capture.mjs" || status=$?
/usr/bin/time -p echo "capture: renderer exit=$status"
if /usr/bin/time -p test "$status" -ne 0; then
  exit "$status"
fi
/usr/bin/time -p test -f "$CAPTURE_DIR/final-desktop.png"
/usr/bin/time -p test -f "$CAPTURE_DIR/final-mobile.png"
/usr/bin/time -p sh -c 'test -s "$1/final-desktop.png" && test -s "$1/final-mobile.png"' _ "$CAPTURE_DIR"
/usr/bin/time -p ls -l "$CAPTURE_DIR"
exit "$status"
