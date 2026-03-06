#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

DATA_DIR="${DATA_DIR:-${REPO_ROOT}/data/macos-vm}"
BOOT_DIR="${BOOT_DIR:-${DATA_DIR}/boot}"
UPSTREAM_DIR="${UPSTREAM_DIR:-${DATA_DIR}/OSX-KVM}"
RECOVERY_DIR="${RECOVERY_DIR:-${DATA_DIR}/com.apple.recovery.boot}"
MACOS_SHORTNAME="${MACOS_SHORTNAME:-sonoma}" # high-sierra|mojave|catalina|big-sur|monterey|ventura|sonoma|sequoia|tahoe
MACOS_OS_TYPE="${MACOS_OS_TYPE:-latest}"     # default|latest
OPENCORE_COMMIT="${OPENCORE_COMMIT:-991523f}" # pre-Tahoe OpenCore is more stable under TCG

mkdir -p "${DATA_DIR}" "${BOOT_DIR}"

if [[ ! -d "${UPSTREAM_DIR}/.git" ]]; then
  echo "[fetch-assets] Cloning OSX-KVM into ${UPSTREAM_DIR}"
  git clone https://github.com/kholia/OSX-KVM.git "${UPSTREAM_DIR}"
else
  echo "[fetch-assets] Updating existing OSX-KVM checkout"
  git -C "${UPSTREAM_DIR}" fetch origin
  git -C "${UPSTREAM_DIR}" pull --ff-only
fi

echo "[fetch-assets] Downloading recovery assets for ${MACOS_SHORTNAME}"
set +e
python3 "${UPSTREAM_DIR}/fetch-macOS-v2.py" \
  --action download \
  --shortname "${MACOS_SHORTNAME}" \
  --os-type "${MACOS_OS_TYPE}" \
  --outdir "${RECOVERY_DIR}"
fetch_status=$?
set -e

if [[ ${fetch_status} -ne 0 ]]; then
  echo "[fetch-assets] WARNING: fetch-macOS-v2.py exited non-zero (${fetch_status})."
  echo "[fetch-assets]          Will continue if recovery artifacts are present."
fi

if [[ ! -f "${RECOVERY_DIR}/BaseSystem.dmg" ]]; then
  echo "[fetch-assets] ERROR: ${RECOVERY_DIR}/BaseSystem.dmg not found"
  exit 1
fi

echo "[fetch-assets] Converting BaseSystem.dmg -> BaseSystem.img"
dmg2img -i "${RECOVERY_DIR}/BaseSystem.dmg" "${BOOT_DIR}/BaseSystem.img"

echo "[fetch-assets] Copying OpenCore + firmware assets into boot directory"
copy_from_repo() {
  local rel_path="$1"
  local dst_path="$2"
  if [[ -n "${OPENCORE_COMMIT}" ]] && git -C "${UPSTREAM_DIR}" cat-file -e "${OPENCORE_COMMIT}:${rel_path}" 2>/dev/null; then
    git -C "${UPSTREAM_DIR}" show "${OPENCORE_COMMIT}:${rel_path}" > "${dst_path}"
  else
    if [[ -n "${OPENCORE_COMMIT}" ]]; then
      echo "[fetch-assets] WARNING: ${OPENCORE_COMMIT}:${rel_path} not found, falling back to HEAD"
    fi
    cp -f "${UPSTREAM_DIR}/${rel_path}" "${dst_path}"
  fi
}

copy_from_repo "OpenCore/OpenCore.qcow2" "${BOOT_DIR}/OpenCore.qcow2"

if [[ -f "${UPSTREAM_DIR}/OVMF_CODE_4M.fd" ]] || [[ -n "${OPENCORE_COMMIT}" ]]; then
  if ! copy_from_repo "OVMF_CODE_4M.fd" "${BOOT_DIR}/OVMF_CODE_4M.fd"; then
    cp -f /usr/share/OVMF/OVMF_CODE_4M.fd "${BOOT_DIR}/OVMF_CODE_4M.fd"
  fi
else
  cp -f /usr/share/OVMF/OVMF_CODE_4M.fd "${BOOT_DIR}/OVMF_CODE_4M.fd"
fi

if [[ -f "${UPSTREAM_DIR}/OVMF_VARS-1920x1080.fd" ]] || [[ -n "${OPENCORE_COMMIT}" ]]; then
  if ! copy_from_repo "OVMF_VARS-1920x1080.fd" "${BOOT_DIR}/OVMF_VARS-1920x1080.fd"; then
    cp -f /usr/share/OVMF/OVMF_VARS_4M.fd "${BOOT_DIR}/OVMF_VARS-1920x1080.fd"
  fi
else
  cp -f /usr/share/OVMF/OVMF_VARS_4M.fd "${BOOT_DIR}/OVMF_VARS-1920x1080.fd"
fi

MANIFEST="${BOOT_DIR}/manifest.txt"
(
  cd "${BOOT_DIR}"
  sha256sum BaseSystem.img OpenCore.qcow2 OVMF_CODE_4M.fd OVMF_VARS-1920x1080.fd > "${MANIFEST}"
)

echo "[fetch-assets] Manifest written to ${MANIFEST}"
echo "[fetch-assets] Done"
