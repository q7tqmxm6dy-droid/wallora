#!/usr/bin/env bash
#
# Swap the AdMob test ids for your real ones and switch the app to live ads.
#
# Get the three ids from https://admob.google.com:
#   app id        Apps -> your app -> App settings        ca-app-pub-XXXXXXXX~YYYYYYYY
#   banner id     Ad units -> create "Banner"             ca-app-pub-XXXXXXXX/1111111111
#   interstitial  Ad units -> create "Interstitial"       ca-app-pub-XXXXXXXX/2222222222
#
# The app id uses a tilde, unit ids use a slash. They are different values; do not mix them up.
#
set -euo pipefail

APP_ID="${1:-}"
BANNER_ID="${2:-}"
INTERSTITIAL_ID="${3:-}"

usage() {
  cat <<'USAGE'
Usage: scripts/set_admob_ids.sh <app-id> <banner-unit-id> <interstitial-unit-id>

Example:
  scripts/set_admob_ids.sh \
    ca-app-pub-1234567890123456~9876543210 \
    ca-app-pub-1234567890123456/1111111111 \
    ca-app-pub-1234567890123456/2222222222
USAGE
}

if [ -z "$APP_ID" ] || [ -z "$BANNER_ID" ] || [ -z "$INTERSTITIAL_ID" ]; then
  usage
  exit 1
fi

fail() { echo "error: $1" >&2; exit 1; }

# AdMob silently serves nothing on a malformed id, which is painful to debug, so check the shape.
case "$APP_ID" in
  ca-app-pub-*"~"*) ;;
  *) fail "app id must look like ca-app-pub-1234567890123456~9876543210 (note the tilde)" ;;
esac
case "$BANNER_ID" in
  ca-app-pub-*/*) ;;
  *) fail "banner unit id must look like ca-app-pub-1234567890123456/1111111111 (note the slash)" ;;
esac
case "$INTERSTITIAL_ID" in
  ca-app-pub-*/*) ;;
  *) fail "interstitial unit id must look like ca-app-pub-1234567890123456/2222222222 (note the slash)" ;;
esac

# All three ids must belong to the same AdMob publisher (the number after ca-app-pub-).
# A mixed set is accepted by every tool and then simply never serves, which is a miserable
# thing to debug, so catch it here.
publisher_of() { echo "$1" | sed -E 's#ca-app-pub-([0-9]+)[~/].*#\1#'; }
APP_PUB="$(publisher_of "$APP_ID")"
BANNER_PUB="$(publisher_of "$BANNER_ID")"
INTERSTITIAL_PUB="$(publisher_of "$INTERSTITIAL_ID")"
if [ "$APP_PUB" != "$BANNER_PUB" ] || [ "$APP_PUB" != "$INTERSTITIAL_PUB" ]; then
  fail "the ids come from different AdMob publishers ($APP_PUB / $BANNER_PUB / $INTERSTITIAL_PUB); ads would never serve"
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STRINGS="$REPO_ROOT/app/src/main/res/values/strings.xml"
CONFIG="$REPO_ROOT/app/src/main/java/com/wallora/app/ads/AdsConfig.kt"

[ -f "$STRINGS" ] || fail "cannot find $STRINGS"
[ -f "$CONFIG" ] || fail "cannot find $CONFIG"

APP_ID="$APP_ID" BANNER_ID="$BANNER_ID" INTERSTITIAL_ID="$INTERSTITIAL_ID" \
STRINGS="$STRINGS" CONFIG="$CONFIG" python3 - <<'PY'
import os
import re

app_id = os.environ["APP_ID"]
banner = os.environ["BANNER_ID"]
interstitial = os.environ["INTERSTITIAL_ID"]


def sub_once(path, pattern, replacement, what):
    text = open(path, encoding="utf-8").read()
    new, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"error: could not locate {what} in {path}")
    open(path, "w", encoding="utf-8").write(new)
    print(f"  {what}: updated")


# 1. the app id, referenced by the manifest meta-data
sub_once(
    os.environ["STRINGS"],
    r'(<string name="admob_app_id" translatable="false">)[^<]*(</string>)',
    r"\g<1>" + app_id + r"\g<2>",
    "app id (strings.xml)",
)

# 2. the two unit ids
sub_once(
    os.environ["CONFIG"],
    r'(REAL_BANNER = ")[^"]*(")',
    r"\g<1>" + banner + r"\g<2>",
    "banner unit id",
)
sub_once(
    os.environ["CONFIG"],
    r'(REAL_INTERSTITIAL = ")[^"]*(")',
    r"\g<1>" + interstitial + r"\g<2>",
    "interstitial unit id",
)

# 3. live ads on
sub_once(
    os.environ["CONFIG"],
    r"(const val USE_TEST_ADS = )(true|false)",
    r"\g<1>false",
    "USE_TEST_ADS",
)
PY

echo
echo "Done. Remaining:"
grep -n "USE_TEST_ADS = " "$CONFIG" | sed 's/^/  /'
grep -n "REAL_BANNER\|REAL_INTERSTITIAL" "$CONFIG" | sed 's/^/  /'
grep -n "admob_app_id" "$STRINGS" | sed 's/^/  /'
echo
echo "Now rebuild and install:"
echo "  JAVA_HOME=\"\$JAVA_HOME\" ./gradlew :app:assembleRelease"
