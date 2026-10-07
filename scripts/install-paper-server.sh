#!/usr/bin/env bash
# Installs the Mojang-mapped Paper server (plus all of its libraries) into the local Maven
# repository as io.papermc.paper:paper-server:<version>-R0.1-SNAPSHOT:mojang-mapped.
#
# This replaces paperweight-userdev's dev bundle, which only works with Gradle.
#
# Usage: scripts/install-paper-server.sh [minecraft-version] [path/to/paper.jar]
#   If no jar is given, the latest build for the version is downloaded from PaperMC.
set -euo pipefail

MC_VERSION="${1:-1.21.10}"
PAPER_JAR="${2:-}"
ARTIFACT_VERSION="${MC_VERSION}-R0.1-SNAPSHOT"
MVN="${MVN:-mvn}"

# On Windows "python3" may be a Microsoft Store stub that exists but doesn't run.
if [[ -z "${PYTHON:-}" ]]; then
  for candidate in python3 python; do
    if "$candidate" -c 'pass' >/dev/null 2>&1; then
      PYTHON="$candidate"
      break
    fi
  done
fi
PYTHON="${PYTHON:-python3}"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

if [[ -z "$PAPER_JAR" ]]; then
  echo "Resolving latest Paper build for $MC_VERSION..."
  URL="$(curl -fsSL -H 'User-Agent: vanillaminimaps-build' \
    "https://fill.papermc.io/v3/projects/paper/versions/${MC_VERSION}/builds/latest" \
    | "$PYTHON" -c 'import json,sys; print(json.load(sys.stdin)["downloads"]["server:default"]["url"])')"
  echo "Downloading $URL"
  PAPER_JAR="$WORK_DIR/paperclip.jar"
  curl -fsSL -H 'User-Agent: vanillaminimaps-build' -o "$PAPER_JAR" "$URL"
fi
PAPER_JAR="$(realpath "$PAPER_JAR")"

echo "Patching server (paperclip.patchonly)..."
(cd "$WORK_DIR" && java -Dpaperclip.patchonly=true -jar "$PAPER_JAR" >/dev/null)

SERVER_JAR="$(find "$WORK_DIR/versions" -name '*.jar' | head -n 1)"
if [[ -z "$SERVER_JAR" ]]; then
  echo "Patched server jar not found" >&2
  exit 1
fi

echo "Merging server jar and libraries into a single compile jar..."
MERGE_DIR="$WORK_DIR/merged"
mkdir -p "$MERGE_DIR"
# Libraries first, then the server jar so its classes win on conflicts.
while IFS= read -r jar; do
  unzip -qo "$jar" -d "$MERGE_DIR" -x 'META-INF/*.SF' 'META-INF/*.DSA' 'META-INF/*.RSA' 'module-info.class' 'META-INF/versions/*'
done < <(find "$WORK_DIR/libraries" -name '*.jar' | sort)
unzip -qo "$SERVER_JAR" -d "$MERGE_DIR" -x 'META-INF/*.SF' 'META-INF/*.DSA' 'META-INF/*.RSA' 'module-info.class' 'META-INF/versions/*'

OUT_JAR="$WORK_DIR/paper-server-${ARTIFACT_VERSION}-mojang-mapped.jar"
(cd "$MERGE_DIR" && jar --create --file "$OUT_JAR" --no-manifest .)

"$MVN" -B -q install:install-file \
  -Dfile="$OUT_JAR" \
  -DgroupId=io.papermc.paper \
  -DartifactId=paper-server \
  -Dversion="$ARTIFACT_VERSION" \
  -Dclassifier=mojang-mapped \
  -Dpackaging=jar \
  -DgeneratePom=true

echo "Installed io.papermc.paper:paper-server:${ARTIFACT_VERSION}:mojang-mapped"
