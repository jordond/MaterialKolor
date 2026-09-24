#!/bin/sh
# Draws og-default.svg into the site's og-default.png, 1200 by 630, in the kit's Bricolage
# Grotesque and no other font. Needs rsvg-convert (librsvg) and oxipng.
set -eu

here=$(cd "$(dirname "$0")" && pwd)
fonts="$here/../../kit/src/commonMain/composeResources/font"
out="$here/../src/wasmJsMain/resources/og-default.png"
conf=$(mktemp)
trap 'rm -f "$conf"' EXIT

printf '<?xml version="1.0"?>\n<fontconfig>\n  <dir>%s</dir>\n  <cachedir>%s</cachedir>\n</fontconfig>\n' \
  "$fonts" "${TMPDIR:-/tmp}/materialkolor-og-fonts" > "$conf"
# Pango on macOS asks Core Text for fonts, which would fall back to a system face, unless it is
# told to use fontconfig and so the one folder above.
PANGOCAIRO_BACKEND=fontconfig FONTCONFIG_FILE="$conf" rsvg-convert --width 1200 --height 630 --output "$out" "$here/og-default.svg"
oxipng --quiet --opt max --strip safe "$out"
echo "Wrote $out, $(wc -c < "$out" | tr -d ' ') bytes"
