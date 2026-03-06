#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DATA_DIR="${DATA_DIR:-${REPO_ROOT}/data/macos-vm}"
RUN_DIR="${RUN_DIR:-${DATA_DIR}/run}"
PIDFILE="${PIDFILE:-${RUN_DIR}/qemu.pid}"

if [[ ! -f "${PIDFILE}" ]]; then
  echo "[stop-macos] No QEMU pidfile found (${PIDFILE})."
  exit 0
fi

pid="$(<"${PIDFILE}")"
if kill -0 "${pid}" >/dev/null 2>&1; then
  echo "[stop-macos] Sending TERM to QEMU PID ${pid}"
  kill "${pid}"
else
  echo "[stop-macos] PID ${pid} not running."
fi

rm -f "${PIDFILE}"
