#!/usr/bin/env bash
# Fetches and extracts the pinned libgpg-error / libgcrypt source trees
# needed by the native (JNI) build, into external/libgpg-error and
# external/libgcrypt.
#
# This script exists so there is a single, scripted source of truth for
# these versions (rather than only prose in BUILDING.md), and so that any
# CI or packaging recipe (e.g. an F-Droid srclib/prebuild step) has a
# concrete command to invoke instead of following manual instructions.
#
# Usage: external/fetch-deps.sh

set -euo pipefail

LIBGPG_ERROR_VERSION="1.61"
LIBGCRYPT_VERSION="1.12.2"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

fetch_and_extract() {
    local name="$1"
    local version="$2"
    local url="https://gnupg.org/ftp/gcrypt/${name}/${name}-${version}.tar.bz2"
    local tarball="${name}-${version}.tar.bz2"

    if [ -d "$name" ]; then
        echo "== $name already present at external/$name, skipping =="
        return
    fi

    echo "== Fetching $name $version =="
    curl -fL --retry 3 -o "$tarball" "$url"
    tar xf "$tarball"
    mv "${name}-${version}" "$name"
    rm -f "$tarball"
}

fetch_and_extract "libgpg-error" "$LIBGPG_ERROR_VERSION"
fetch_and_extract "libgcrypt" "$LIBGCRYPT_VERSION"

echo "== Done. external/libgpg-error and external/libgcrypt are ready. =="
