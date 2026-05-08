#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

usage() {
  cat <<'EOF'
Usage: scripts/publish_release.sh [options]

Build the signed release APK, create or reuse a GitHub release, and upload the APK.

Options:
  --repo OWNER/REPO       GitHub repository. Defaults to GITHUB_REPOSITORY,
                          then the github/origin remote, then Cabbagec/LunarCal.
  --tag TAG              Release tag. Defaults to v<app versionName>.
  --target REF           Target ref if GitHub needs to create the tag. Defaults to
                          the current branch, or the current commit if detached.
  --apk PATH             APK to upload. Defaults to app/build/outputs/apk/release/app-release.apk.
  --asset-name NAME      Release asset name. Defaults to lunarcal-<version>.apk.
  --token-file PATH      Token file. Defaults to GITHUB_TOKEN_FILE,
                          then ~/.ssh/lunarcal.github.token.
  --skip-build           Upload the existing APK without running Gradle.
  --draft                Create the release as a draft if it does not exist.
  --prerelease           Create the release as a prerelease if it does not exist.
  --no-replace-asset     Fail instead of replacing an existing asset with the same name.
  -h, --help             Show this help.

Environment:
  GH_TOKEN               GitHub API token. Used before --token-file/GITHUB_TOKEN_FILE.
  GITHUB_TOKEN_FILE      Path to a token file.
  GITHUB_REPOSITORY      OWNER/REPO, for example Cabbagec/LunarCal.
  RELEASE_BODY           Release notes body. Defaults to "Release <version>."
EOF
}

die() {
  echo "error: $*" >&2
  exit 1
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "missing required command: $1"
}

version_name() {
  sed -nE 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$REPO_DIR/app/build.gradle.kts" | head -n 1
}

remote_repo() {
  local url
  url="$(git -C "$REPO_DIR" config --get remote.github.url 2>/dev/null || true)"
  if [ -z "$url" ]; then
    url="$(git -C "$REPO_DIR" config --get remote.origin.url 2>/dev/null || true)"
  fi
  python3 - "$url" <<'PY'
import re
import sys

url = sys.argv[1].strip()
if url.endswith(".git"):
    url = url[:-4]

patterns = [
    r"^git@github\.com:([^/]+/[^/]+)$",
    r"^ssh://git@github\.com/([^/]+/[^/]+)$",
    r"^https://github\.com/([^/]+/[^/]+)$",
    r"^github:([^/]+/[^/]+)$",
]
for pattern in patterns:
    match = re.match(pattern, url)
    if match:
        print(match.group(1))
        break
PY
}

json_field() {
  python3 - "$1" "$2" <<'PY'
import json
import sys

data = json.load(open(sys.argv[1], encoding="utf-8"))
value = data
for key in sys.argv[2].split("."):
    value = value[key]
print(value)
PY
}

http_request() {
  local method="$1"
  local url="$2"
  local output="$3"
  local data="${4:-}"
  local content_type="${5:-application/json}"
  local args=(
    -sS
    -X "$method"
    -o "$output"
    -w "%{http_code}"
    -H "Accept: application/vnd.github+json"
    -H "Authorization: Bearer $TOKEN"
    -H "X-GitHub-Api-Version: 2022-11-28"
  )

  if [ -n "$data" ]; then
    args+=(-H "Content-Type: $content_type" --data-binary "$data")
  fi

  curl "${args[@]}" "$url"
}

SKIP_BUILD=false
DRAFT=false
PRERELEASE=false
REPLACE_ASSET=true
REPO="${GITHUB_REPOSITORY:-}"
TAG=""
TARGET=""
APK=""
ASSET_NAME=""
TOKEN_FILE="${GITHUB_TOKEN_FILE:-$HOME/.ssh/lunarcal.github.token}"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --repo)
      REPO="${2:?missing value for --repo}"
      shift 2
      ;;
    --tag)
      TAG="${2:?missing value for --tag}"
      shift 2
      ;;
    --target)
      TARGET="${2:?missing value for --target}"
      shift 2
      ;;
    --apk)
      APK="${2:?missing value for --apk}"
      shift 2
      ;;
    --asset-name)
      ASSET_NAME="${2:?missing value for --asset-name}"
      shift 2
      ;;
    --token-file)
      TOKEN_FILE="${2:?missing value for --token-file}"
      shift 2
      ;;
    --skip-build)
      SKIP_BUILD=true
      shift
      ;;
    --draft)
      DRAFT=true
      shift
      ;;
    --prerelease)
      PRERELEASE=true
      shift
      ;;
    --no-replace-asset)
      REPLACE_ASSET=false
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "unknown argument: $1"
      ;;
  esac
done

cd "$REPO_DIR"

need_cmd curl
need_cmd git
need_cmd python3
need_cmd sed

VERSION="$(version_name)"
[ -n "$VERSION" ] || die "could not read versionName from app/build.gradle.kts"

if [ -z "$REPO" ]; then
  REPO="$(remote_repo)"
fi
if [ -z "$REPO" ]; then
  REPO="Cabbagec/LunarCal"
fi

[ -n "$TAG" ] || TAG="v$VERSION"
[ -n "$TARGET" ] || TARGET="$(git -C "$REPO_DIR" symbolic-ref --short HEAD 2>/dev/null || git -C "$REPO_DIR" rev-parse HEAD)"
[ -n "$APK" ] || APK="$REPO_DIR/app/build/outputs/apk/release/app-release.apk"
[ -n "$ASSET_NAME" ] || ASSET_NAME="lunarcal-$VERSION.apk"

if [ -n "${GH_TOKEN:-}" ]; then
  TOKEN="$GH_TOKEN"
else
  [ -s "$TOKEN_FILE" ] || die "token file missing or empty: $TOKEN_FILE"
  TOKEN="$(tr -d '\r\n' < "$TOKEN_FILE")"
fi
[ -n "$TOKEN" ] || die "GitHub token is empty"

if [ "$SKIP_BUILD" = false ]; then
  need_cmd docker
  docker compose run --rm android gradle \
    -Duser.home=/workspace/build/home \
    --no-daemon \
    --project-cache-dir /workspace/build/gradle-project-cache \
    :app:assembleRelease
fi

[ -s "$APK" ] || die "release APK missing: $APK"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

API="https://api.github.com/repos/$REPO"
RELEASE_JSON="$TMP_DIR/release.json"
PAYLOAD_JSON="$TMP_DIR/release-payload.json"
ASSETS_JSON="$TMP_DIR/assets.json"
UPLOAD_JSON="$TMP_DIR/upload.json"

echo "Publishing $ASSET_NAME to $REPO release $TAG"

HTTP="$(http_request GET "$API/releases/tags/$TAG" "$RELEASE_JSON")"
if [ "$HTTP" = "404" ]; then
  python3 - "$PAYLOAD_JSON" "$TAG" "$TARGET" "LunarCal $VERSION" "${RELEASE_BODY:-Release $VERSION.}" "$DRAFT" "$PRERELEASE" <<'PY'
import json
import sys

payload = {
    "tag_name": sys.argv[2],
    "target_commitish": sys.argv[3],
    "name": sys.argv[4],
    "body": sys.argv[5],
    "draft": sys.argv[6] == "true",
    "prerelease": sys.argv[7] == "true",
}
with open(sys.argv[1], "w", encoding="utf-8") as output:
    json.dump(payload, output)
PY
  HTTP="$(http_request POST "$API/releases" "$RELEASE_JSON" "@$PAYLOAD_JSON")"
fi

if [ "$HTTP" != "200" ] && [ "$HTTP" != "201" ]; then
  echo "GitHub release request failed with HTTP $HTTP" >&2
  python3 -m json.tool "$RELEASE_JSON" >&2 || cat "$RELEASE_JSON" >&2
  exit 1
fi

RELEASE_ID="$(json_field "$RELEASE_JSON" id)"
RELEASE_URL="$(json_field "$RELEASE_JSON" html_url)"

HTTP="$(http_request GET "$API/releases/$RELEASE_ID/assets" "$ASSETS_JSON")"
if [ "$HTTP" != "200" ]; then
  echo "GitHub asset list failed with HTTP $HTTP" >&2
  python3 -m json.tool "$ASSETS_JSON" >&2 || cat "$ASSETS_JSON" >&2
  exit 1
fi

EXISTING_ASSET_ID="$(
  python3 - "$ASSETS_JSON" "$ASSET_NAME" <<'PY'
import json
import sys

assets = json.load(open(sys.argv[1], encoding="utf-8"))
for asset in assets:
    if asset.get("name") == sys.argv[2]:
        print(asset["id"])
        break
PY
)"

if [ -n "$EXISTING_ASSET_ID" ]; then
  if [ "$REPLACE_ASSET" = false ]; then
    die "asset already exists: $ASSET_NAME"
  fi
  HTTP="$(http_request DELETE "$API/releases/assets/$EXISTING_ASSET_ID" "$TMP_DIR/delete-asset.json")"
  if [ "$HTTP" != "204" ]; then
    echo "GitHub asset delete failed with HTTP $HTTP" >&2
    cat "$TMP_DIR/delete-asset.json" >&2
    exit 1
  fi
fi

HTTP="$(
  http_request POST \
    "https://uploads.github.com/repos/$REPO/releases/$RELEASE_ID/assets?name=$ASSET_NAME" \
    "$UPLOAD_JSON" \
    "@$APK" \
    "application/vnd.android.package-archive"
)"
if [ "$HTTP" != "201" ]; then
  echo "GitHub asset upload failed with HTTP $HTTP" >&2
  python3 -m json.tool "$UPLOAD_JSON" >&2 || cat "$UPLOAD_JSON" >&2
  exit 1
fi

ASSET_URL="$(json_field "$UPLOAD_JSON" browser_download_url)"
APK_SIZE="$(du -h "$APK" | awk '{print $1}')"

echo "Release: $RELEASE_URL"
echo "Asset:   $ASSET_URL"
echo "Size:    $APK_SIZE"
