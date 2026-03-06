#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DATA_DIR="${DATA_DIR:-${REPO_ROOT}/data/macos-vm}"

required_bins=(
  qemu-system-x86_64
  qemu-img
  python3
  websockify
  dmg2img
  jq
  socat
)

missing=()
for bin in "${required_bins[@]}"; do
  if ! command -v "${bin}" >/dev/null 2>&1; then
    missing+=("${bin}")
  fi
done

if [[ ${#missing[@]} -gt 0 ]]; then
  echo "[preflight] Missing required binaries: ${missing[*]}"
  exit 1
fi

if [[ -e /dev/kvm ]]; then
  echo "[preflight] /dev/kvm is present."
  if [[ -r /dev/kvm && -w /dev/kvm ]]; then
    echo "[preflight] KVM is accessible, but this setup intentionally uses software emulation (TCG)."
  else
    echo "[preflight] /dev/kvm exists but is not accessible. Will run in TCG mode."
  fi
else
  echo "[preflight] /dev/kvm not present. Confirmed TCG-only environment."
fi

mem_mib="$(awk '/MemTotal/ {print int($2/1024)}' /proc/meminfo)"
disk_kib="$(df --output=avail "${REPO_ROOT}" | awk 'NR==2 {print $1}')"
disk_gib="$((disk_kib / 1024 / 1024))"

echo "[preflight] Host memory: ${mem_mib} MiB"
echo "[preflight] Free disk at ${REPO_ROOT}: ${disk_gib} GiB"

if (( mem_mib < 8192 )); then
  echo "[preflight] WARNING: Less than 8 GiB RAM available. macOS installer may be unstable."
fi

if (( disk_gib < 80 )); then
  echo "[preflight] WARNING: Less than 80 GiB free disk available."
fi

mkdir -p "${DATA_DIR}" "${DATA_DIR}/boot" "${DATA_DIR}/run" "${DATA_DIR}/logs" "${DATA_DIR}/screens"

echo "[preflight] Data directory: ${DATA_DIR}"
echo "[preflight] OK"
