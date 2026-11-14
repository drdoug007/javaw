#!/bin/sh
# Regenerates test-vendor/node/node_modules from package.json (needs network; tests never do).
# Installs without lifecycle scripts into an empty temporary directory, then removes files that are
# not needed at runtime: TypeScript sources, source maps, declaration files and types-only packages.
set -eu
here=$(cd "$(dirname "$0")" && pwd)
work=$(mktemp -d)
cp "$here/package.json" "$work/"
(cd "$work" && npm install --omit=dev --ignore-scripts --no-audit --no-fund)
cd "$work/node_modules"
rm -rf @types undici-types happy-dom/src
find . \( -name '*.map' -o -name '*.d.ts' -o -name '*.d.cts' -o -name '*.d.mts' \) -delete
rm -rf "$here/node_modules"
cp -R "$work/node_modules" "$here/node_modules"
cp "$work/package-lock.json" "$here/package-lock.json"
rm -rf "$work"
