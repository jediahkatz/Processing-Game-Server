#!/usr/bin/env bash
set -euo pipefail

# Run this inside macOS Recovery Terminal to bypass Setup Assistant
# and create a local admin account on the installed volume.

TARGET_DATA_VOLUME="${TARGET_DATA_VOLUME:-/Volumes/Macintosh HDa - Data}"
USERNAME="${USERNAME:-test}"
PASSWORD="${PASSWORD:-test1234}"
REALNAME="${REALNAME:-Test User}"
USER_ID="${USER_ID:-501}"
PRIMARY_GROUP_ID="${PRIMARY_GROUP_ID:-20}"

if [[ ! -d "${TARGET_DATA_VOLUME}" ]]; then
  echo "[recovery-user] Missing target data volume: ${TARGET_DATA_VOLUME}"
  echo "[recovery-user] Current volumes:"
  ls -la /Volumes || true
  exit 1
fi

if [[ ! -x /usr/bin/dscl ]]; then
  echo "[recovery-user] /usr/bin/dscl not available in this recovery shell."
  exit 1
fi

ROOT="${TARGET_DATA_VOLUME}"
DSDB="${ROOT}/private/var/db/dslocal/nodes/Default"

echo "[recovery-user] Using target: ${ROOT}"
echo "[recovery-user] Creating Setup Assistant bypass marker..."
mkdir -p "${ROOT}/private/var/db"
touch "${ROOT}/private/var/db/.AppleSetupDone"

echo "[recovery-user] Creating user '${USERNAME}' in dslocal db..."
/usr/bin/dscl -f "${DSDB}" localhost -create "/Local/Default/Users/${USERNAME}"
/usr/bin/dscl -f "${DSDB}" localhost -create "/Local/Default/Users/${USERNAME}" UserShell /bin/zsh
/usr/bin/dscl -f "${DSDB}" localhost -create "/Local/Default/Users/${USERNAME}" RealName "${REALNAME}"
/usr/bin/dscl -f "${DSDB}" localhost -create "/Local/Default/Users/${USERNAME}" UniqueID "${USER_ID}"
/usr/bin/dscl -f "${DSDB}" localhost -create "/Local/Default/Users/${USERNAME}" PrimaryGroupID "${PRIMARY_GROUP_ID}"
/usr/bin/dscl -f "${DSDB}" localhost -create "/Local/Default/Users/${USERNAME}" NFSHomeDirectory "/Users/${USERNAME}"
/usr/bin/dscl -f "${DSDB}" localhost -passwd "/Local/Default/Users/${USERNAME}" "${PASSWORD}"
/usr/bin/dscl -f "${DSDB}" localhost -append "/Local/Default/Groups/admin" GroupMembership "${USERNAME}"

echo "[recovery-user] Creating user home directory..."
mkdir -p "${ROOT}/Users/${USERNAME}"
chown -R "${USER_ID}:${PRIMARY_GROUP_ID}" "${ROOT}/Users/${USERNAME}" || true

echo "[recovery-user] Verifying account..."
/usr/bin/dscl -f "${DSDB}" localhost -read "/Local/Default/Users/${USERNAME}" || true
ls -la "${ROOT}/private/var/db/.AppleSetupDone"

echo "[recovery-user] Done. Reboot and boot installed Macintosh HD."
