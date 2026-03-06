# Troubleshooting

## Installer progress seems stuck under TCG

Software emulation can make the initial ETA inaccurate and volatile. Validate actual activity before restarting:

```bash
tail -n 20 /var/log/install.log
ps aux | grep -iE "InstallAssistant|osinstall|containermanagerd" | grep -v grep
```

If log timestamps keep advancing and installer processes are present, let it continue.

## Installer UI disappeared

The GUI can close while background install work continues. Reopen from Recovery Terminal:

```bash
/Install\ macOS\ Tahoe.app/Contents/MacOS/InstallAssistant
```

Then continue through license + disk selection.

## Recovery ping fails but web access works

QEMU user networking frequently blocks ICMP. Validate HTTPS/TCP instead:

```bash
curl -I https://swcdn.apple.com
curl -I https://apple.com
nc -vz swcdn.apple.com 443
```

If these succeed, network is usable for installer traffic.

## OpenCore only shows EFI and not Base System

Confirm installer media attachment mode in launch args:

- `BaseSystem.img` must be attached as an IDE drive on SATA bus.
- Read-only block mapping can prevent detection in picker.

Use the provided launcher defaults in `run-macos-tcg.sh`.

## Base System boot returns to picker or panics in `com.apple.kec.corecrypto`

If `macOS Base System` immediately drops back to OpenCore (black screen ~60–100s), or verbose boot panics in `corecrypto`, use the known-good TCG combination:

- OpenCore pinned to pre-Tahoe commit: `OPENCORE_COMMIT=991523f`
- CPU model: `Skylake-Client`
- CPU extra: `-hle,-rtm,+invtsc,vmware-cpuid-freq=on`
- CPU flags without AVX2

Refresh assets with pinned OpenCore:

```bash
OPENCORE_COMMIT=991523f MACOS_SHORTNAME=sonoma bash tools/macos-vm/fetch-macos-assets.sh
```

Then relaunch with defaults (or equivalent explicit overrides):

```bash
INSTALL_MEDIA=1 bash tools/macos-vm/run-macos-tcg.sh
```

## VNC port already in use

Use non-conflicting display number:

```bash
VNC_DISPLAY=51 bash tools/macos-vm/run-macos-tcg.sh
```

Then point noVNC gateway to port `5951`:

```bash
VNC_PORT=5951 NOVNC_PORT=6080 bash tools/macos-vm/start-display-gateway.sh
```

## Post-install boot

After installation finishes and VM reboots:

1. In OpenCore, pick the installed `Macintosh HD` entry (not `macOS Base System`).
2. If needed, run without installer media:

```bash
INSTALL_MEDIA=0 bash tools/macos-vm/run-macos-tcg.sh
```
