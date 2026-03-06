#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

DATA_DIR="${DATA_DIR:-${REPO_ROOT}/data/macos-vm}"
DISK_PATH="${DISK_PATH:-${DATA_DIR}/disks/mac_hdd_ng.img}"
LOG_DIR="${LOG_DIR:-${DATA_DIR}/logs}"
RUN_DIR="${RUN_DIR:-${DATA_DIR}/run}"
INTERVAL_SEC="${INTERVAL_SEC:-300}"
LOG_FILE="${LOG_FILE:-${LOG_DIR}/install-watch.log}"

mkdir -p "${LOG_DIR}"

echo "[watch-install] Starting monitor loop (interval=${INTERVAL_SEC}s)" | tee -a "${LOG_FILE}"
echo "[watch-install] Disk path: ${DISK_PATH}" | tee -a "${LOG_FILE}"

while true; do
  ts="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  if [[ -f "${RUN_DIR}/qemu.pid" ]]; then
    pid="$(<"${RUN_DIR}/qemu.pid")"
    if kill -0 "${pid}" >/dev/null 2>&1; then
      disk_size="missing"
      if [[ -f "${DISK_PATH}" ]]; then
        disk_size="$(stat -c '%s' "${DISK_PATH}")"
      fi
      echo "${ts} qemu_pid=${pid} disk_size_bytes=${disk_size}" | tee -a "${LOG_FILE}"
      bash "${SCRIPT_DIR}/capture-frame.sh" >> "${LOG_FILE}" 2>&1 || true
    else
      echo "${ts} qemu_pid=${pid} not-running" | tee -a "${LOG_FILE}"
      break
    fi
  else
    echo "${ts} qemu-pidfile-missing" | tee -a "${LOG_FILE}"
    break
  fi
  sleep "${INTERVAL_SEC}"
done

echo "[watch-install] Exiting monitor loop" | tee -a "${LOG_FILE}"
