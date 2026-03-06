# macOS VM (QEMU TCG, no KVM)

This runbook sets up a graphical macOS VM on Linux using QEMU in **software emulation mode** (`tcg`), intended for environments where `/dev/kvm` is unavailable.

## What this provides

- QEMU-based macOS VM boot path (OpenCore + Apple recovery image).
- Headless VM with VNC display output (real desktop GUI, not CLI-only).
- Optional browser access through noVNC/websockify.
- Helper scripts for host setup, asset fetch, disk creation, launch, and screenshot capture.

## Directory layout

- Scripts (tracked): `tools/macos-vm/`
- Runtime artifacts (ignored by git): `data/macos-vm/`
  - `boot/` OpenCore + BaseSystem image + firmware + manifest
  - `disks/` VM disks
  - `run/` sockets + pid files
  - `logs/` qemu/websockify logs
  - `screens/` captured screenshots

## 1) Install host dependencies

```bash
bash tools/macos-vm/setup-host.sh
```

## 2) Run preflight checks

```bash
bash tools/macos-vm/preflight-check.sh
```

Expected: it should report **TCG-only** mode if `/dev/kvm` is absent.

## 3) Fetch OpenCore + macOS recovery assets

Defaults to Sonoma recovery files and pins a pre-Tahoe OpenCore build (`OPENCORE_COMMIT=991523f`) that is more stable in TCG mode. Override `OPENCORE_COMMIT` if you want HEAD.

```bash
MACOS_SHORTNAME=sonoma OPENCORE_COMMIT=991523f bash tools/macos-vm/fetch-macos-assets.sh
```

## 4) Create VM disk

```bash
DISK_SIZE=128G bash tools/macos-vm/create-disks.sh
```

## 5) Launch VM (software emulation)

```bash
ALLOCATED_RAM=8192 CPU_CORES=2 CPU_THREADS=4 VNC_DISPLAY=51 \
  bash tools/macos-vm/run-macos-tcg.sh
```

The script prints the VNC endpoint (`127.0.0.1:5951` when `VNC_DISPLAY=51`).

Known-good TCG CPU defaults are already set in the launcher:
- `CPU_MODEL=Skylake-Client`
- `CPU_EXTRA=-hle,-rtm,+invtsc,vmware-cpuid-freq=on`
- `MY_OPTIONS=+ssse3,+sse4.2,+popcnt,+avx,+aes,+xsave,+xsaveopt,check` (AVX2 disabled)

### First-boot installer notes

Inside the macOS installer:
1. Open **Disk Utility**, erase the target disk as APFS/GUID.
2. Install macOS to that disk.
3. Reboot as needed until setup assistant/desktop.

## 6) Optional browser display (noVNC)

In a second terminal:

```bash
VNC_PORT=5951 NOVNC_PORT=6080 bash tools/macos-vm/start-display-gateway.sh
```

Open:

`http://127.0.0.1:6080/vnc.html?host=127.0.0.1&port=6080`

Stop it with:

```bash
bash tools/macos-vm/stop-display-gateway.sh
```

## 7) Capture framebuffer screenshot

```bash
bash tools/macos-vm/capture-frame.sh
```

This writes both `.ppm` and `.png` files under `data/macos-vm/screens/`.

## 8) Optional continuous install watcher

To log disk growth and periodic screenshots while installation runs:

```bash
INTERVAL_SEC=300 bash tools/macos-vm/watch-install-progress.sh
```

This appends entries to `data/macos-vm/logs/install-watch.log` and captures frames on each interval.

## Post-install boot

After installation, boot without installer media:

```bash
INSTALL_MEDIA=0 bash tools/macos-vm/run-macos-tcg.sh
```

## Optional: offline installer acceleration

If recovery install progress appears stalled (common under TCG), you can run with an offline `InstallAssistant.pkg`.

1. Download `InstallAssistant.pkg` (Apple-hosted URL), for example:

```bash
curl -L --fail --output data/macos-vm/boot/InstallAssistant.pkg \
  "https://swcdn.apple.com/content/downloads/15/23/047-60297-A_CC7WE2S6AE/h1lvp87655fnwe4zityuew187qsnj7dzcd/InstallAssistant.pkg"
```

2. Build ISO:

```bash
genisoimage -allow-limited-size -R -J -V INSTALLASSISTANT \
  -o data/macos-vm/boot/InstallAssistant.iso \
  data/macos-vm/boot/InstallAssistant.pkg
```

3. Launch VM with ISO attached:

```bash
OFFLINE_ISO=/workspace/data/macos-vm/boot/InstallAssistant.iso \
  bash tools/macos-vm/run-macos-tcg.sh
```

4. In macOS Recovery, open `Utilities -> Terminal` and run:

```bash
cd "/Volumes/Macintosh HD"
mkdir -p private/tmp
cp -R "/Install macOS Tahoe.app" private/tmp
cd "private/tmp/Install macOS Tahoe.app"
mkdir -p Contents/SharedSupport
cp -R /Volumes/InstallAssistant/InstallAssistant.pkg Contents/SharedSupport/SharedSupport.dmg
./Contents/MacOS/InstallAssistant
```

> Note: The upstream `run_offline.sh` helper is Ventura-oriented. For Tahoe in recovery, the manual sequence above is the reliable path.

## Notes

- TCG is significantly slower than KVM. Installer and setup can take a long time.
- Resolution can be changed later by using different OVMF vars images or OpenCore config adjustments.
- If installer UI disappears, check `/var/log/install.log` and running installer processes from Recovery Terminal.
