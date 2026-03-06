# Asset Sources and Provenance

This setup uses public upstream tooling and downloads:

1. **OSX-KVM repository**
   - URL: https://github.com/kholia/OSX-KVM
   - Purpose: OpenCore boot image and helper fetch tooling.

2. **Apple recovery assets**
   - Obtained through `fetch-macOS-v2.py` in the OSX-KVM project.
   - Example output: `BaseSystem.dmg` converted to `BaseSystem.img`.

3. **OVMF firmware**
   - Preferred source: `OVMF_CODE_4M.fd` and `OVMF_VARS-1920x1080.fd` from OSX-KVM checkout.
   - Fallback source: system package files from `/usr/share/OVMF`.

After running `tools/macos-vm/fetch-macos-assets.sh`, checksums are written to:

`data/macos-vm/boot/manifest.txt`
