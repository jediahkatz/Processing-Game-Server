#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

DATA_DIR="${DATA_DIR:-${REPO_ROOT}/data/macos-vm}"
BOOT_DIR="${BOOT_DIR:-${DATA_DIR}/boot}"
DISKS_DIR="${DISKS_DIR:-${DATA_DIR}/disks}"
RUN_DIR="${RUN_DIR:-${DATA_DIR}/run}"
LOG_DIR="${LOG_DIR:-${DATA_DIR}/logs}"

mkdir -p "${RUN_DIR}" "${LOG_DIR}" "${DISKS_DIR}"

ALLOCATED_RAM="${ALLOCATED_RAM:-8192}" # MiB
CPU_SOCKETS="${CPU_SOCKETS:-1}"
CPU_CORES="${CPU_CORES:-2}"
CPU_THREADS="${CPU_THREADS:-4}"

VNC_DISPLAY="${VNC_DISPLAY:-51}" # TCP port will be 5900 + display
SSH_FORWARD_PORT="${SSH_FORWARD_PORT:-2222}"
INSTALL_MEDIA="${INSTALL_MEDIA:-1}" # 1 attaches BaseSystem.img, 0 skips it

SYSTEM_DISK="${SYSTEM_DISK:-${DISKS_DIR}/mac_hdd_ng.img}"
BASESYSTEM_IMG="${BASESYSTEM_IMG:-${BOOT_DIR}/BaseSystem.img}"
OPENCORE_QCOW2="${OPENCORE_QCOW2:-${BOOT_DIR}/OpenCore.qcow2}"
OVMF_CODE="${OVMF_CODE:-${BOOT_DIR}/OVMF_CODE_4M.fd}"
OVMF_VARS_TEMPLATE="${OVMF_VARS_TEMPLATE:-${BOOT_DIR}/OVMF_VARS-1920x1080.fd}"
OVMF_VARS_RUNTIME="${OVMF_VARS_RUNTIME:-${RUN_DIR}/OVMF_VARS.fd}"

MONITOR_SOCKET="${MONITOR_SOCKET:-${RUN_DIR}/qemu-monitor.sock}"
QMP_SOCKET="${QMP_SOCKET:-${RUN_DIR}/qemu-qmp.sock}"
PIDFILE="${PIDFILE:-${RUN_DIR}/qemu.pid}"

MY_OPTIONS="${MY_OPTIONS:-+ssse3,+sse4.2,+popcnt,+avx,+avx2,+aes,+xsave,+xsaveopt,check}"
CPU_MODEL="${CPU_MODEL:-Penryn}"
CPU_VENDOR="${CPU_VENDOR:-GenuineIntel}"
CPU_EXTRA="${CPU_EXTRA:-vmware-cpuid-freq=on}"

if [[ ! -f "${SYSTEM_DISK}" ]]; then
  echo "[run-macos] Missing system disk: ${SYSTEM_DISK}"
  echo "            Run tools/macos-vm/create-disks.sh first."
  exit 1
fi

for path in "${OPENCORE_QCOW2}" "${OVMF_CODE}" "${OVMF_VARS_TEMPLATE}"; do
  if [[ ! -f "${path}" ]]; then
    echo "[run-macos] Missing required file: ${path}"
    echo "            Run tools/macos-vm/fetch-macos-assets.sh first."
    exit 1
  fi
done

if [[ "${INSTALL_MEDIA}" == "1" && ! -f "${BASESYSTEM_IMG}" ]]; then
  echo "[run-macos] INSTALL_MEDIA=1 but BaseSystem image is missing: ${BASESYSTEM_IMG}"
  exit 1
fi

if [[ ! -f "${OVMF_VARS_RUNTIME}" ]]; then
  cp "${OVMF_VARS_TEMPLATE}" "${OVMF_VARS_RUNTIME}"
fi

rm -f "${MONITOR_SOCKET}" "${QMP_SOCKET}" "${PIDFILE}"

echo "[run-macos] Starting QEMU in TCG mode"
echo "[run-macos] VNC endpoint: 127.0.0.1:$((5900 + VNC_DISPLAY))"
echo "[run-macos] Monitor socket: ${MONITOR_SOCKET}"

args=(
  -name "macOS-TCG"
  -machine q35
  -accel tcg,thread=multi,tb-size=1024
  -m "${ALLOCATED_RAM}"
  -smp "${CPU_THREADS}",cores="${CPU_CORES}",sockets="${CPU_SOCKETS}"
  -cpu "${CPU_MODEL}",vendor="${CPU_VENDOR}","${CPU_EXTRA}","${MY_OPTIONS}"
  -device qemu-xhci,id=xhci
  -device usb-kbd,bus=xhci.0
  -device usb-tablet,bus=xhci.0
  -device usb-ehci,id=ehci
  -global ICH9-LPC.acpi-pci-hotplug-with-bridge-support=off
  -device isa-applesmc,osk="ourhardworkbythesewordsguardedpleasedontsteal(c)AppleComputerInc"
  -smbios type=2
  -drive if=pflash,format=raw,readonly=on,file="${OVMF_CODE}"
  -drive if=pflash,format=raw,file="${OVMF_VARS_RUNTIME}"
  -device ich9-ahci,id=sata
  -drive id=OpenCoreBoot,if=none,snapshot=on,format=qcow2,file="${OPENCORE_QCOW2}"
  -device ide-hd,bus=sata.2,drive=OpenCoreBoot
  -drive id=MacHDD,if=none,file="${SYSTEM_DISK}",format=qcow2
  -device ide-hd,bus=sata.4,drive=MacHDD
  -netdev user,id=net0,hostfwd=tcp::"${SSH_FORWARD_PORT}"-:22
  -device vmxnet3,netdev=net0,id=net0,mac=52:54:00:c9:18:27
  -device vmware-svga
  -display none
  -vnc 127.0.0.1:"${VNC_DISPLAY}"
  -monitor unix:"${MONITOR_SOCKET}",server,nowait
  -qmp unix:"${QMP_SOCKET}",server,nowait
  -pidfile "${PIDFILE}"
)

if [[ "${INSTALL_MEDIA}" == "1" ]]; then
  args+=(
    -drive id=InstallMedia,if=none,file="${BASESYSTEM_IMG}",format=raw
    -device ide-hd,bus=sata.3,drive=InstallMedia
  )
fi

if [[ -n "${EXTRA_QEMU_ARGS:-}" ]]; then
  # shellcheck disable=SC2206
  extra=( ${EXTRA_QEMU_ARGS} )
  args+=("${extra[@]}")
fi

LOG_FILE="${LOG_DIR}/qemu-$(date +%Y%m%d-%H%M%S).log"
echo "[run-macos] Logging to ${LOG_FILE}"

qemu-system-x86_64 "${args[@]}" 2>&1 | tee -a "${LOG_FILE}"
