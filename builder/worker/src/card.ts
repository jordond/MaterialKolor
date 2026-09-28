// Draws the 1200 by 630 theme card as palette pixels, laid out like og-default.png. The colors on
// it are the ones in the code, as they are, plus the default card's fixed paper and inks.
import type { SharedTheme } from './code';
import { targetName } from './copy';
import type { Font } from './glyphs';

export const CARD_WIDTH = 1200;
export const CARD_HEIGHT = 630;

export const FONT_TITLE = 0;
export const FONT_BODY = 1;

// og-default.png's paper, title ink and subtitle ink.
const PAPER = 0xfff8f6;
const INK = 0x231917;
const INK_MUTED = 0x53433f;

const MARGIN = 72;
const CONTENT_WIDTH = CARD_WIDTH - 2 * MARGIN;
const TITLE_BASELINE = 152;
const SUBTITLE_BASELINE = 222;
const SWATCH_TOP = 270;
const SWATCH_BOTTOM = 510;
const FOOTER_BASELINE = 574;
const SEED_MIN_WIDTH = 312;
const SWATCH_MAX_WIDTH = 108;
const SWATCH_GAP = 16;
const SWATCH_RADIUS = 24;
const TEXT_LEVELS = 15;
const EDGE_LEVELS = 7;
const ROW = CARD_WIDTH + 1;
const ELLIPSIS = '...';

/** The card's scanlines, each with a zero filter byte in front, and its palette. */
export interface CardPixels {
  readonly palette: Uint8Array;
  readonly scanlines: Uint8Array;
}

export function drawCard(theme: SharedTheme, fonts: readonly Font[]): CardPixels {
  const palette = new Palette();
  // Paper is index 0, so the zeroed buffer starts as blank paper with every filter byte None.
  palette.index(PAPER);
  const scanlines = new Uint8Array(CARD_HEIGHT * ROW);
  const title = fonts[FONT_TITLE]!;
  const body = fonts[FONT_BODY]!;
  const name = theme.projectName === null ? '' : printable(theme.projectName);
  const details = `${theme.style} style, for ${targetName(theme)}`;

  const ink = palette.ramp(INK, TEXT_LEVELS);
  const muted = palette.ramp(INK_MUTED, TEXT_LEVELS);
  drawText(scanlines, title, fit(title, name === '' ? theme.seed : name, CONTENT_WIDTH), TITLE_BASELINE, ink);
  const subtitle = name === '' ? details : `${theme.seed}, ${details}`;
  drawText(scanlines, body, fit(body, subtitle, CONTENT_WIDTH), SUBTITLE_BASELINE, muted);
  drawText(scanlines, body, 'MaterialKolor Builder', FOOTER_BASELINE, muted);

  const extras = [
    ...theme.keyColors,
    ...(theme.cmfTertiarySeed === null ? [] : [theme.cmfTertiarySeed]),
    ...theme.accents.map((accent) => accent.seed),
  ];
  const extraWidth =
    extras.length === 0
      ? 0
      : Math.min(SWATCH_MAX_WIDTH, Math.floor((CONTENT_WIDTH - SEED_MIN_WIDTH) / extras.length) - SWATCH_GAP);
  const radius = Math.min(SWATCH_RADIUS, Math.floor(extraWidth / 2) || SWATCH_RADIUS);
  let left = MARGIN + CONTENT_WIDTH;
  for (const color of extras.reverse()) {
    left -= extraWidth;
    drawSwatch(scanlines, palette, left, extraWidth, radius, color);
    left -= SWATCH_GAP;
  }
  drawSwatch(scanlines, palette, MARGIN, left - MARGIN, SWATCH_RADIUS, theme.seed);
  return { palette: palette.bytes(), scanlines };
}

/** The name as the atlas can draw it, accents dropped and anything past ASCII left out. */
export function printable(text: string): string {
  return text
    .normalize('NFD')
    .replace(/\s+/g, ' ')
    .replace(/[^\x20-\x7e]/g, '')
    .replace(/ {2,}/g, ' ')
    .trim();
}

function measure(font: Font, text: string): number {
  let width = 0;
  for (let index = 0; index < text.length; index++) width += font.glyphs[text.charCodeAt(index)]?.advance ?? 0;
  return width;
}

function fit(font: Font, text: string, maxWidth: number): string {
  if (measure(font, text) <= maxWidth) return text;
  let cut = text.length;
  while (cut > 0 && measure(font, text.slice(0, cut).trimEnd() + ELLIPSIS) > maxWidth) cut--;
  return text.slice(0, cut).trimEnd() + ELLIPSIS;
}

function drawText(scanlines: Uint8Array, font: Font, text: string, baseline: number, ramp: Uint8Array): void {
  let pen = MARGIN;
  for (let index = 0; index < text.length; index++) {
    const glyph = font.glyphs[text.charCodeAt(index)];
    if (glyph === undefined) continue;
    const x = Math.round(pen + glyph.left);
    const y = baseline - glyph.top;
    for (let row = 0; row < glyph.height; row++) {
      const line = (y + row) * ROW + 1;
      const source = glyph.offset + row * glyph.width;
      for (let column = 0; column < glyph.width; column++) {
        const coverage = font.coverage[source + column]!;
        if (coverage === 0 || x + column < 0 || x + column >= CARD_WIDTH) continue;
        const level = Math.round((coverage * TEXT_LEVELS) / 255);
        const at = line + x + column;
        // Where two glyphs overlap, the darker pixel wins.
        if (level > 0 && (scanlines[at] === 0 || level > ramp.indexOf(scanlines[at]!))) scanlines[at] = ramp[level]!;
      }
    }
    pen += glyph.advance;
  }
}

/** A rounded swatch from [left], [width] wide, with anti-aliased corners. */
function drawSwatch(
  scanlines: Uint8Array,
  palette: Palette,
  left: number,
  width: number,
  radius: number,
  color: string,
): void {
  const ramp = palette.ramp(Number.parseInt(color.slice(1), 16), EDGE_LEVELS);
  const full = ramp[EDGE_LEVELS]!;
  const right = left + width;
  for (let y = SWATCH_TOP; y < SWATCH_BOTTOM; y++) {
    const line = y * ROW + 1;
    const fromTop = y - SWATCH_TOP;
    const fromBottom = SWATCH_BOTTOM - 1 - y;
    const corner = Math.min(fromTop, fromBottom);
    if (corner >= radius) {
      scanlines.fill(full, line + left, line + right);
      continue;
    }
    scanlines.fill(full, line + left + radius, line + right - radius);
    const dy = radius - corner - 0.5;
    for (let offset = 0; offset < radius; offset++) {
      const dx = radius - offset - 0.5;
      const coverage = Math.min(1, Math.max(0, radius + 0.5 - Math.hypot(dx, dy)));
      const index = ramp[Math.round(coverage * EDGE_LEVELS)]!;
      scanlines[line + left + offset] = index;
      scanlines[line + right - 1 - offset] = index;
    }
  }
}

/** The card's palette, each color stored once, paper first. */
class Palette {
  private readonly colors: number[] = [];
  private readonly indices = new Map<number, number>();

  index(rgb: number): number {
    const known = this.indices.get(rgb);
    if (known !== undefined) return known;
    // Cannot happen with at most 16 swatches, but a full palette falls back to the nearest end.
    if (this.colors.length === 256) return 0;
    this.indices.set(rgb, this.colors.length);
    this.colors.push(rgb);
    return this.colors.length - 1;
  }

  /** Indices from paper at 0 to [rgb] at [levels], blended evenly in between. */
  ramp(rgb: number, levels: number): Uint8Array {
    const ramp = new Uint8Array(levels + 1);
    for (let level = 0; level <= levels; level++) {
      const amount = level / levels;
      let blended = 0;
      for (const shift of [16, 8, 0]) {
        const from = (PAPER >> shift) & 0xff;
        const to = (rgb >> shift) & 0xff;
        blended |= Math.round(from + (to - from) * amount) << shift;
      }
      ramp[level] = level === levels ? this.index(rgb) : this.index(blended);
    }
    return ramp;
  }

  bytes(): Uint8Array {
    const bytes = new Uint8Array(this.colors.length * 3);
    this.colors.forEach((rgb, index) => bytes.set([rgb >> 16, (rgb >> 8) & 0xff, rgb & 0xff], index * 3));
    return bytes;
  }
}
