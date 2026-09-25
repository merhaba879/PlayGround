#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
/usr/bin/time -p bash -n "$0"
/usr/bin/time -p test -n "${CAPTURE_URL:-}"
/usr/bin/time -p test -n "${CAPTURE_DIR:-}"
/usr/bin/time -p mkdir -p "${CAPTURE_DIR}"
/usr/bin/time -p bash -c 'echo "capturing URL: $CAPTURE_URL"; echo "output dir: $CAPTURE_DIR"'
/usr/bin/time -p node "${RUNTIME_DIR:?}/scripts/default-capture.mjs"
STATUS=$?
/usr/bin/time -p bash -c 'echo "capture exit: '"$STATUS"'"'
/usr/bin/time -p test -f "${CAPTURE_DIR}/final-desktop.png"
/usr/bin/time -p test -f "${CAPTURE_DIR}/final-mobile.png"
/usr/bin/time -p ls -lh "${CAPTURE_DIR}"
exit $STATUS
