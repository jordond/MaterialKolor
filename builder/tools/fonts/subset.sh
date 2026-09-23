#!/usr/bin/env bash
#
# Rebuilds the two builder faces that ship in :builder:kit.
#
# Design D asks for Bricolage Grotesque on the poster, the wordmark and the chrome, and JetBrains
# Mono on values and code. Both are OFL. The upstream files carry every script and every axis, so
# they are far too heavy for a wasm site. This script cuts them down to Latin and to the one axis
# the builder actually varies, weight.
#
# The output is TTF, not WOFF2. Compose resources decode fonts through Skiko, which reads TTF and
# OTF only, so a WOFF2 here would load in a browser and fail in the app. The site serves the TTF
# with brotli, which lands within a kilobyte or so of what WOFF2 would have given us anyway.
#
# Needs pyftsubset and fonttools (pip install fonttools brotli) and, for the size report, brotli.
#
# Usage: builder/tools/fonts/subset.sh
#
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
out_dir="$repo_root/builder/kit/src/commonMain/composeResources/font"
license_dir="$repo_root/builder/tools/fonts/licenses"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

# Sources. Both are the upstream variable builds, pinned so a rerun gives the same bytes.
bricolage_version="2024-09-17"
bricolage_url="https://raw.githubusercontent.com/google/fonts/main/ofl/bricolagegrotesque/BricolageGrotesque%5Bopsz%2Cwdth%2Cwght%5D.ttf"
bricolage_license_url="https://raw.githubusercontent.com/google/fonts/main/ofl/bricolagegrotesque/OFL.txt"
jetbrains_version="2.304"
jetbrains_url="https://github.com/JetBrains/JetBrainsMono/releases/download/v${jetbrains_version}/JetBrainsMono-${jetbrains_version}.zip"

# The Google Fonts "latin" range. Basic Latin, Latin-1, the punctuation and symbol strays a UI
# needs, and the combining marks the two faces compose accented glyphs from.
latin_range='U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0304,U+0308,U+0329,U+2000-206F,U+2074,U+20AC,U+2122,U+2191,U+2193,U+2212,U+2215,U+FEFF,U+FFFD'

# Axes. Bricolage ships opsz 12 to 96, wdth 75 to 100 and wght 200 to 800. Keeping all three costs
# about 70 KB brotli, which is over budget, and the builder never varies width or optical size, so
# both are pinned and only weight stays live.
#
# Watch out for one upstream quirk. The Google Fonts build defaults every axis to its maximum, so
# the default instance is 96pt ExtraBold rather than 14pt Regular. Rebasing the weight axis to a
# 400 default costs about 6 KB brotli, which puts the face over budget, so the default is left
# alone and every caller passes a weight. Compose resources do that for us, the `Font` overload
# the kit uses always sends a `wght` variation.
bricolage_pins=(opsz=14 wdth=100)
bricolage_axes="wght 200 to 800, default 800 upstream"
# JetBrains Mono ships wght 100 to 800 on the upright face. The builder never sets mono italic.
jetbrains_axes="wght 100 to 800"

# Features. Kerning, the mark attachments the combining marks need, and the localised forms. The
# brand face keeps its ligatures, the mono face drops them so code reads literally.
brand_features='kern,liga,calt,ccmp,mark,mkmk,locl'
mono_features='kern,ccmp,mark,mkmk,locl'

# Budget from architecture 6.11, per face, brotli compressed.
budget_bytes=$((40 * 1024))

echo "Working in $work_dir"
mkdir -p "$out_dir" "$license_dir"

echo "Fetching Bricolage Grotesque ($bricolage_version)"
curl -sSL --fail -o "$work_dir/bricolage.ttf" "$bricolage_url"
curl -sSL --fail -o "$license_dir/OFL-BricolageGrotesque.txt" "$bricolage_license_url"

echo "Fetching JetBrains Mono ($jetbrains_version)"
curl -sSL --fail -o "$work_dir/jetbrains.zip" "$jetbrains_url"
unzip -q -o "$work_dir/jetbrains.zip" -d "$work_dir/jetbrains"
cp "$work_dir/jetbrains/fonts/variable/JetBrainsMono[wght].ttf" "$work_dir/jetbrains.ttf"
cp "$work_dir/jetbrains/OFL.txt" "$license_dir/OFL-JetBrainsMono.txt"

echo "Pinning Bricolage axes (${bricolage_pins[*]}), keeping $bricolage_axes"
fonttools varLib.instancer \
    -o "$work_dir/bricolage-pinned.ttf" \
    "$work_dir/bricolage.ttf" \
    "${bricolage_pins[@]}" >/dev/null

echo "Subsetting to Latin"
pyftsubset "$work_dir/bricolage-pinned.ttf" \
    --output-file="$out_dir/BricolageGrotesque_Variable.ttf" \
    --unicodes="$latin_range" \
    --layout-features="$brand_features" \
    --no-hinting \
    --name-IDs='*' \
    --name-legacy \
    --notdef-outline

echo "Subsetting JetBrains Mono to Latin, keeping $jetbrains_axes"
pyftsubset "$work_dir/jetbrains.ttf" \
    --output-file="$out_dir/JetBrainsMono_Variable.ttf" \
    --unicodes="$latin_range" \
    --layout-features="$mono_features" \
    --no-hinting \
    --name-IDs='*' \
    --name-legacy \
    --notdef-outline

status=0
for face in "$out_dir/BricolageGrotesque_Variable.ttf" "$out_dir/JetBrainsMono_Variable.ttf"; do
    raw=$(wc -c <"$face" | tr -d ' ')
    brotli -f -q 11 -o "$work_dir/size.br" "$face"
    compressed=$(wc -c <"$work_dir/size.br" | tr -d ' ')
    printf '%-34s %7s bytes raw, %7s bytes brotli\n' "$(basename "$face")" "$raw" "$compressed"
    if [ "$compressed" -gt "$budget_bytes" ]; then
        echo "  over the ${budget_bytes} byte budget, pin another axis or trim the range" >&2
        status=1
    fi
done

exit "$status"
