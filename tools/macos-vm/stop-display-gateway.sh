#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DATA_DIR="${DATA_DIR:-${REPO_ROOT}/data/macos-vm}"
RUN_DIR="${RUN_DIR:-${DATA_DIR}/run}"
PIDFILE="${PIDFILE:-${RUN_DIR}/websockify.pid}"

if [[ ! -f "${PIDFILE}" ]]; then
  echo "[display-gateway] No PID file found (${PIDFILE}); nothing to stop."
  exit 0
fi

pid="$(<"${PIDFILE}")"
if kill -0 "${pid}" >/dev/null 2>&1; then
  echo "[display-gateway] Stopping websockify PID ${pid}"
  kill "${pid}"
else
  echo "[display-gateway] PID ${pid} not running; cleaning stale PID file."
fi

rm -f "${PIDFILE}"
