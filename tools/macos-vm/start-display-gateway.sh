#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DATA_DIR="${DATA_DIR:-${REPO_ROOT}/data/macos-vm}"
RUN_DIR="${RUN_DIR:-${DATA_DIR}/run}"
LOG_DIR="${LOG_DIR:-${DATA_DIR}/logs}"

mkdir -p "${RUN_DIR}" "${LOG_DIR}"

NOVNC_PORT="${NOVNC_PORT:-6080}"
VNC_HOST="${VNC_HOST:-127.0.0.1}"
VNC_PORT="${VNC_PORT:-5951}"
WEB_DIR="${WEB_DIR:-/usr/share/novnc}"
PIDFILE="${PIDFILE:-${RUN_DIR}/websockify.pid}"
LOGFILE="${LOGFILE:-${LOG_DIR}/websockify.log}"

if [[ ! -d "${WEB_DIR}" ]]; then
  echo "[display-gateway] noVNC web directory not found: ${WEB_DIR}"
  exit 1
fi

if [[ -f "${PIDFILE}" ]]; then
  existing_pid="$(<"${PIDFILE}")"
  if kill -0 "${existing_pid}" >/dev/null 2>&1; then
    echo "[display-gateway] websockify already running (PID ${existing_pid})"
    echo "[display-gateway] URL: http://127.0.0.1:${NOVNC_PORT}/vnc.html?host=127.0.0.1&port=${NOVNC_PORT}"
    exit 0
  fi
  rm -f "${PIDFILE}"
fi

echo "[display-gateway] Starting websockify on :${NOVNC_PORT} -> ${VNC_HOST}:${VNC_PORT}"
nohup websockify --web "${WEB_DIR}" "${NOVNC_PORT}" "${VNC_HOST}:${VNC_PORT}" >> "${LOGFILE}" 2>&1 &
echo $! > "${PIDFILE}"

sleep 1
if kill -0 "$(cat "${PIDFILE}")" >/dev/null 2>&1; then
  echo "[display-gateway] Started (PID $(cat "${PIDFILE}"))"
  echo "[display-gateway] Open: http://127.0.0.1:${NOVNC_PORT}/vnc.html?host=127.0.0.1&port=${NOVNC_PORT}"
else
  echo "[display-gateway] Failed to start; check ${LOGFILE}"
  exit 1
fi
