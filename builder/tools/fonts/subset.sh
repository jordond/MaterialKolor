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
# The OFL texts land beside the faces rather than next to this script, so they travel into the wasm
# bundle the site serves. The about page is what has to credit both faces and link these two files.
license_dir="$repo_root/builder/kit/src/commonMain/composeResources/files"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

# Sources. Both are the upstream variable builds, pinned so a rerun gives the same bytes.
#
# Bricolage comes from google/fonts, which has no tags, so the pin is the commit that last touched
# the file. Reading it off `main` would have meant a rerun could quietly produce a different face
# from the one committed here.
bricolage_version="1.001"
bricolage_commit="b9f6c712059d72742282ebdf06eadfc264c827f3"
bricolage_dir="https://raw.githubusercontent.com/google/fonts/$bricolage_commit/ofl/bricolagegrotesque"
bricolage_url="$bricolage_dir/BricolageGrotesque%5Bopsz%2Cwdth%2Cwght%5D.ttf"
bricolage_license_url="$bricolage_dir/OFL.txt"
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
#
# The instancer runs without `--update-name-table`, so the shipped face still names itself "96pt
# ExtraBold". Only the family name matters to `FontFamily`, so this is cosmetic and staying.
bricolage_pins=(opsz=14 wdth=100)
bricolage_axes="wght 200 to 800, default 800 upstream"
# JetBrains Mono ships wght 100 to 800 on the upright face. The builder never sets mono italic.
jetbrains_axes="wght 100 to 800"

# Features. Kerning, the mark attachments the combining marks need, and the localised forms.
#
# Neither upstream face carries liga or calt, so the brand face has no ligatures to keep and asking
# for them only made this list read as though it did. What the shipped faces end up with is ccmp
# and locl in GSUB, plus kern, mark and mkmk in Bricolage's GPOS. The mono list stays separate so a
# future JetBrains Mono that does ship calt is still subset without it, because code has to read
# literally.
brand_features='kern,ccmp,mark,mkmk,locl'
mono_features='kern,ccmp,mark,mkmk,locl'

# Budget from architecture 6.11, per face, brotli compressed.
budget_bytes=$((40 * 1024))

echo "Working in $work_dir"
mkdir -p "$out_dir" "$license_dir"

echo "Fetching Bricolage Grotesque $bricolage_version (google/fonts ${bricolage_commit:0:12})"
curl -sSL --fail -o "$work_dir/bricolage.ttf" "$bricolage_url"
curl -sSL --fail -o "$license_dir/OFL-BricolageGrotesque.txt" "$bricolage_license_url"

# The pin is a commit rather than a release, so make sure it still carries the version the
# committed subset was cut from.
if ! fonttools ttx -q -t name -o - "$work_dir/bricolage.ttf" | grep -q "Version $bricolage_version"; then
    echo "  the pinned commit no longer reports version $bricolage_version" >&2
    exit 1
fi

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

# B-404
# Selawik stands in for Segoe UI on the Fluent skin (architecture 6.11). It ships as static faces,
# and Fluent's type scale only sets Regular and SemiBold, so those two are cut to the same Latin
# range. The release is a tag with a zip, pinned by its checksum.
#
# Selawik carries a Reserved Font Name, and the OFL counts a subset as a Modified Version, which may
# not present that name to users. So the subsets are renamed in their name tables. Only the file
# names still say Selawik, which is where it came from and not a name anyone sees.
selawik_version="1.01"
selawik_url="https://github.com/microsoft/Selawik/releases/download/${selawik_version}/Selawik_Release.zip"
selawik_sha256="3f62c51e05e3b5a1e6241cf92a371f0be2ea1183aa87b30718bbd40832a8d423"
selawik_license_commit="1908e0b053079879b2afdc03334521dc991b6de9"
selawik_license_url="https://raw.githubusercontent.com/microsoft/Selawik/$selawik_license_commit/LICENSE.txt"
selawik_family="Builder Fluent Sans"
selawik_features='kern,ccmp,mark,mkmk,locl'

echo "Fetching Selawik $selawik_version"
curl -sSL --fail -o "$work_dir/selawik.zip" "$selawik_url"
if ! echo "$selawik_sha256  $work_dir/selawik.zip" | shasum -a 256 -c - >/dev/null; then
    echo "  the Selawik $selawik_version zip no longer matches its pinned checksum" >&2
    exit 1
fi
unzip -q -o "$work_dir/selawik.zip" -d "$work_dir/selawik"
curl -sSL --fail -o "$license_dir/OFL-Selawik.txt" "$selawik_license_url"

for pair in "selawk:Regular" "selawksb:Semibold"; do
    source_name="${pair%%:*}"
    style="${pair##*:}"
    echo "Subsetting Selawik $style to Latin"
    pyftsubset "$work_dir/selawik/$source_name.ttf" \
        --output-file="$work_dir/selawik-$style.ttf" \
        --unicodes="$latin_range" \
        --layout-features="$selawik_features" \
        --no-hinting \
        --name-IDs='*' \
        --name-legacy \
        --notdef-outline
    python3 - "$work_dir/selawik-$style.ttf" "$out_dir/Selawik_$style.ttf" "$selawik_family" "$style" <<'PY'
import sys
from fontTools.ttLib import TTFont

source, target, family, style = sys.argv[1:]
font = TTFont(source)
names = font["name"]
# Only the records that name the font itself. The copyright, trademark and license records keep
# Microsoft's wording, the Reserved Font Name included, as the OFL asks.
renamed = {1: family, 3: family, 4: family, 6: family.replace(" ", ""), 16: family, 18: family, 21: family}
for record in list(names.names):
    text = record.toUnicode()
    if record.nameID not in renamed or "Selawik" not in text:
        continue
    names.setName(
        text.replace("Selawik", renamed[record.nameID]),
        record.nameID,
        record.platformID,
        record.platEncID,
        record.langID,
    )
font.save(target)
PY
done

status=0
selawik_bytes=0
for face in "$out_dir/Selawik_Regular.ttf" "$out_dir/Selawik_Semibold.ttf"; do
    brotli -f -q 11 -o "$work_dir/size.br" "$face"
    selawik_bytes=$((selawik_bytes + $(wc -c <"$work_dir/size.br" | tr -d ' ')))
done
# The two Selawik weights load together, so they share the one face budget.
printf '%-34s %7s bytes brotli for both weights\n' "Selawik" "$selawik_bytes"
if [ "$selawik_bytes" -gt "$budget_bytes" ]; then
    echo "  over the ${budget_bytes} byte budget, drop the second weight or trim the range" >&2
    status=1
fi

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
