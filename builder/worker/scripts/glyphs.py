"""Writes src/glyphs.bin, the glyph atlas the theme card draws its text with.

Rasterizes printable ASCII from the kit's Bricolage Grotesque at the two sizes the card uses, the
way og-default.png sets its title and subtitle. Needs Pillow. Run it from builder/worker with
`python3 scripts/glyphs.py` after changing a size or the font, and commit the output.

Layout, little endian, stored as is so the Worker reads it without inflating anything first.
  u8 font count
  per font  u16 pixel size, u16 glyph count, then per glyph 16 bytes
            u16 code point, u16 advance in 64ths of a pixel, i16 left, i16 top above the baseline,
            u16 width, u16 height, u32 offset of its coverage bytes
  coverage  one byte per pixel, 0 to 255, rows top down, every glyph back to back
"""

import pathlib
import struct

from PIL import Image, ImageDraw, ImageFont

HERE = pathlib.Path(__file__).resolve().parent
FONT = HERE.parent.parent / "kit/src/commonMain/composeResources/font/BricolageGrotesque_Variable.ttf"
OUTPUT = HERE.parent / "src/glyphs.bin"
# The title font first, then the body font, matching FONT_TITLE and FONT_BODY in src/card.ts.
FONTS = [(84, b"Bold"), (36, b"Regular")]
CODES = range(0x20, 0x7F)


def main() -> None:
    headers = bytearray([len(FONTS)])
    coverage = bytearray()
    for size, weight in FONTS:
        font = ImageFont.truetype(str(FONT), size)
        font.set_variation_by_name(weight)
        headers += struct.pack("<HH", size, len(CODES))
        for code in CODES:
            char = chr(code)
            advance = round(font.getlength(char) * 64)
            left, top, right, bottom = font.getbbox(char, anchor="ls")
            width, height = max(right - left, 0), max(bottom - top, 0)
            headers += struct.pack("<HHhhHHI", code, advance, left, -top, width, height, len(coverage))
            if width and height:
                image = Image.new("L", (width, height), 0)
                ImageDraw.Draw(image).text((-left, -top), char, font=font, fill=255, anchor="ls")
                coverage += image.tobytes()
    OUTPUT.write_bytes(bytes(headers + coverage))
    print(f"{OUTPUT.name}, {OUTPUT.stat().st_size} bytes")


if __name__ == "__main__":
    main()
