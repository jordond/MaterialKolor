// The glyph atlas in glyphs.bin, which scripts/glyphs.py writes. Its layout is described there.
import packed from './glyphs.bin';

export interface Glyph {
  /** How far the pen moves after the glyph, in pixels. */
  readonly advance: number;
  readonly left: number;
  /** Rows from the top of the glyph down to the baseline. */
  readonly top: number;
  readonly width: number;
  readonly height: number;
  readonly offset: number;
}

export interface Font {
  readonly size: number;
  /** Indexed by code point, printable ASCII only. */
  readonly glyphs: readonly (Glyph | undefined)[];
  readonly coverage: Uint8Array;
}

const GLYPH_BYTES = 16;

// Kept once read, so only the first card an isolate draws pays for the few hundred headers.
let fonts: readonly Font[] | null = null;

/** The atlas fonts, title first. */
export function loadFonts(): readonly Font[] {
  fonts ??= parse(new Uint8Array(packed));
  return fonts;
}

function parse(bytes: Uint8Array): Font[] {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const count = view.getUint8(0);
  const tables: { size: number; glyphs: Glyph[]; codes: number[] }[] = [];
  let position = 1;
  for (let font = 0; font < count; font++) {
    const size = view.getUint16(position, true);
    const glyphCount = view.getUint16(position + 2, true);
    position += 4;
    const glyphs: Glyph[] = [];
    const codes: number[] = [];
    for (let index = 0; index < glyphCount; index++, position += GLYPH_BYTES) {
      codes.push(view.getUint16(position, true));
      glyphs.push({
        advance: view.getUint16(position + 2, true) / 64,
        left: view.getInt16(position + 4, true),
        top: view.getInt16(position + 6, true),
        width: view.getUint16(position + 8, true),
        height: view.getUint16(position + 10, true),
        offset: view.getUint32(position + 12, true),
      });
    }
    tables.push({ size, glyphs, codes });
  }
  const coverage = bytes.subarray(position);
  return tables.map(({ size, glyphs, codes }) => {
    const byCode: (Glyph | undefined)[] = [];
    codes.forEach((code, index) => (byCode[code] = glyphs[index]));
    return { size, glyphs: byCode, coverage };
  });
}
