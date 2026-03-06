#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  SUDO="sudo"
else
  SUDO=""
fi

echo "[setup-host] Updating apt metadata..."
${SUDO} apt-get update

echo "[setup-host] Installing emulation and display dependencies..."
${SUDO} apt-get install -y \
  qemu-system-x86 \
  qemu-utils \
  ovmf \
  novnc \
  websockify \
  dmg2img \
  jq \
  python3-pil \
  socat \
  git \
  curl

echo "[setup-host] Done."
