#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

DATA_DIR="${DATA_DIR:-${REPO_ROOT}/data/macos-vm}"
DISKS_DIR="${DISKS_DIR:-${DATA_DIR}/disks}"
DISK_PATH="${DISK_PATH:-${DISKS_DIR}/mac_hdd_ng.img}"
DISK_SIZE="${DISK_SIZE:-128G}"

mkdir -p "${DISKS_DIR}"

if [[ -f "${DISK_PATH}" ]]; then
  echo "[create-disks] Disk already exists: ${DISK_PATH}"
  qemu-img info "${DISK_PATH}"
  exit 0
fi

echo "[create-disks] Creating qcow2 disk ${DISK_PATH} (${DISK_SIZE})"
qemu-img create -f qcow2 "${DISK_PATH}" "${DISK_SIZE}"
qemu-img info "${DISK_PATH}"
