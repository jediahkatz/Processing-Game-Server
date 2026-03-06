#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DATA_DIR="${DATA_DIR:-${REPO_ROOT}/data/macos-vm}"
RUN_DIR="${RUN_DIR:-${DATA_DIR}/run}"
SCREENS_DIR="${SCREENS_DIR:-${DATA_DIR}/screens}"
MONITOR_SOCKET="${MONITOR_SOCKET:-${RUN_DIR}/qemu-monitor.sock}"

mkdir -p "${SCREENS_DIR}"

stamp="$(date +%Y%m%d-%H%M%S)"
ppm_path="${SCREENS_DIR}/frame-${stamp}.ppm"
png_path="${SCREENS_DIR}/frame-${stamp}.png"

if [[ ! -S "${MONITOR_SOCKET}" ]]; then
  echo "[capture-frame] Monitor socket not found: ${MONITOR_SOCKET}"
  exit 1
fi

printf "screendump %s\n" "${ppm_path}" | socat - UNIX-CONNECT:"${MONITOR_SOCKET}" >/dev/null

if [[ ! -f "${ppm_path}" ]]; then
  echo "[capture-frame] Failed to capture PPM screenshot."
  exit 1
fi

python3 - "${ppm_path}" "${png_path}" <<'PY'
import sys
from PIL import Image

src, dst = sys.argv[1], sys.argv[2]
img = Image.open(src)
img.save(dst, format="PNG")
print(dst)
PY

echo "[capture-frame] Saved:"
echo "  PPM: ${ppm_path}"
echo "  PNG: ${png_path}"
