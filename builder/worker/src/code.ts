// Reads a share code the way ShareCodec.decode in builder/domain does, for the link preview only.
//
// It walks every section in the same order and refuses everything the Kotlin reader refuses, so a
// code either reads the same on both sides or falls back to the default preview here. It keeps
// only what the preview shows and runs no color math. Base64Url.kt, Crc8.kt, ShareCodec.kt and the
// coded enums under domain/model are the source of truth for every constant below.

export type Style =
  | 'TonalSpot'
  | 'Neutral'
  | 'Vibrant'
  | 'Expressive'
  | 'Rainbow'
  | 'FruitSalad'
  | 'Monochrome'
  | 'Fidelity'
  | 'Content'
  | 'Cmf';

export type Library = 'Material3' | 'Unstyled' | 'Fluent' | 'Custom';

/** A named accent as the card draws it. */
export interface SharedAccent {
  readonly name: string;
  readonly seed: string;
}

/** What a share code carries that the link preview shows. Colors are `#RRGGBB`. */
export interface SharedTheme {
  readonly seed: string;
  readonly style: Style;
  readonly library: Library;
  readonly expressive: boolean;
  /** The contrast in hundredths, snapped to the named level the app opens the code at. */
  readonly contrast: number;
  /** The key color overrides in code order, primary first. */
  readonly keyColors: readonly string[];
  readonly cmfTertiarySeed: string | null;
  readonly accents: readonly SharedAccent[];
  readonly projectName: string | null;
}

/** The format version the builder writes, and the only one read here. */
export const VERSION = 1;

/** Longest project name in UTF-8 bytes, `MAX_PROJECT_NAME_BYTES` in the domain. */
export const MAX_PROJECT_NAME_BYTES = 48;

const MAX_ACCENTS = 8;
const MAX_ACCENT_NAME_BYTES = 24;
const MIN_CODE_BYTES = 9;
const MAX_TONE = 100;
const NO_TONE = 0xff;
const MAX_VARINT_BYTES = 4;

const STYLES: readonly Style[] = [
  'TonalSpot',
  'Neutral',
  'Vibrant',
  'Expressive',
  'Rainbow',
  'FruitSalad',
  'Monochrome',
  'Fidelity',
  'Content',
  'Cmf',
];
const LIBRARIES: readonly Library[] = ['Material3', 'Unstyled', 'Fluent', 'Custom'];
const SPEC_COUNT = 3;
const KEY_COLOR_COUNT = 6;
const ROLE_COUNT = 48;
const THRESHOLD_COUNT = 3;
const DEFAULT_THRESHOLD = 0;
// The named contrast levels in hundredths, ContrastLevel.Stops in the domain.
const CONTRAST_STOPS: readonly number[] = [-100, 0, 50, 100];
// Only Standard can be written, Expressive is the default and never travels.
const MOTION_STANDARD = 0;
const DEFAULT_THEME_NAME = 'AppTheme';
// CustomSlot codes, with the gaps left by retired slots.
const CUSTOM_SLOTS: ReadonlySet<number> = new Set([
  0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 30, 31, 32, 33, 34, 35, 36, 37, 38,
  39, 40, 46, 47, 48,
]);

const SECTION_KEY_COLORS = 0x01;
const SECTION_CMF_SEED = 0x02;
const SECTION_ACCENTS = 0x04;
const SECTION_PINS = 0x08;
const SECTION_PROJECT_NAME = 0x10;
const SECTION_TARGET_OPTIONS = 0x20;
const SECTION_RESERVED = 0xc0;
const TARGET_RESERVED = 0xf8;
const EXPRESSIVE_FLAG = 0x04;
const KEY_COLORS_RESERVED = 0xc0;
const ACCENT_TONES = 0x02;
const ACCENT_THRESHOLD = 0x04;
const ACCENT_RESERVED = 0xf8;
const PIN_LIGHT = 0x01;
const PIN_DARK = 0x02;
const PIN_RESERVED = 0xfc;
const OPTION_MOTION_SCHEME = 0x01;
const OPTION_CUSTOM_TONES = 0x02;
const OPTION_THEME_NAME = 0x04;
const OPTION_RESERVED = 0xf8;

// An accent's default light and dark tones, which the writer never spells out.
const DEFAULT_ACCENT_TONES = [40, 90, 80, 30];

/**
 * The theme name bytes allowed here. The codec caps every other field but not this one, so a code
 * with a longer theme name still opens in the app and only its preview falls back to the defaults.
 */
export const THEME_NAME_ALLOWANCE_BYTES = 255;

// The longest code the builder writes, every section present and at its cap, checksum included.
const MAX_CODE_BYTES =
  // Version, seed, scheme, target, contrast and section flags.
  8 +
  // Key color mask and every slot.
  (1 + KEY_COLOR_COUNT * 3) +
  // The CMF tertiary seed.
  3 +
  // Each accent is flags, seed, name, four tones and a threshold.
  (varintBytes(MAX_ACCENTS) +
    MAX_ACCENTS * (1 + 3 + varintBytes(MAX_ACCENT_NAME_BYTES) + MAX_ACCENT_NAME_BYTES + 4 + 1)) +
  // Each pinned role is its code, its modes and a light and a dark color.
  (1 + ROLE_COUNT * (1 + 1 + 3 + 3)) +
  (varintBytes(MAX_PROJECT_NAME_BYTES) + MAX_PROJECT_NAME_BYTES) +
  // Target option flags, motion scheme, theme name, then each custom slot with a light and a dark tone.
  (1 + 1 + varintBytes(THEME_NAME_ALLOWANCE_BYTES) + THEME_NAME_ALLOWANCE_BYTES + 1 + CUSTOM_SLOTS.size * 3) +
  // The CRC-8.
  1;

/** The longest code read here, MAX_CODE_BYTES spelled in unpadded base64url. Longer ones get the defaults. */
export const MAX_CODE_LENGTH = Math.ceil((MAX_CODE_BYTES * 4) / 3);

/** The theme [code] carries, or null for anything the builder would not read. Never throws. */
export function decodeShareCode(code: string): SharedTheme | null {
  if (code.length > MAX_CODE_LENGTH) return null;
  const bytes = decodeBase64Url(code);
  if (bytes === null || bytes.length < MIN_CODE_BYTES || bytes[0] !== VERSION) return null;
  const checksum = bytes.length - 1;
  if (crc8(bytes, checksum) !== bytes[checksum]) return null;
  return new Reader(bytes, checksum).theme();
}

/**
 * The named contrast level nearest [hundredths], as ContrastLevel.nearest picks it. A level halfway
 * between two goes to the one nearer Standard, so 25 and -50 open at Standard and 75 at Medium.
 */
export function nearestContrast(hundredths: number): number {
  let nearest = CONTRAST_STOPS[0]!;
  for (const stop of CONTRAST_STOPS) {
    const distance = Math.abs(stop - hundredths);
    const best = Math.abs(nearest - hundredths);
    if (distance < best || (distance === best && Math.abs(stop) < Math.abs(nearest))) nearest = stop;
  }
  return nearest;
}

/** The CRC-8 a share code ends with, polynomial 0x07, from zero, nothing reflected. */
export function crc8(bytes: Uint8Array, end: number = bytes.length): number {
  let crc = 0;
  for (let index = 0; index < end; index++) {
    crc ^= bytes[index]!;
    for (let bit = 0; bit < 8; bit++) {
      crc = crc & 0x80 ? ((crc << 1) ^ 0x07) & 0xff : (crc << 1) & 0xff;
    }
  }
  return crc;
}

/** The bytes [text] spells as canonical unpadded base64url, or null. */
export function decodeBase64Url(text: string): Uint8Array | null {
  if (text.length % 4 === 1) return null;
  const bytes = new Uint8Array(Math.floor((text.length * 3) / 4));
  let bits = 0;
  let bitCount = 0;
  let written = 0;
  for (let index = 0; index < text.length; index++) {
    const sextet = sextetOf(text.charCodeAt(index));
    if (sextet < 0) return null;
    bits = (bits << 6) | sextet;
    bitCount += 6;
    if (bitCount >= 8) {
      bitCount -= 8;
      bytes[written++] = (bits >> bitCount) & 0xff;
      bits &= (1 << bitCount) - 1;
    }
  }
  return bits === 0 ? bytes : null;
}

function sextetOf(char: number): number {
  if (char >= 65 && char <= 90) return char - 65;
  if (char >= 97 && char <= 122) return char - 97 + 26;
  if (char >= 48 && char <= 57) return char - 48 + 52;
  if (char === 45) return 62;
  if (char === 95) return 63;
  return -1;
}

// Fatal so malformed UTF-8 is refused, and the BOM kept, just as the Kotlin round trip does.
const UTF8 = new TextDecoder('utf-8', { fatal: true, ignoreBOM: true });

function hex(value: number): string {
  return `#${value.toString(16).padStart(6, '0').toUpperCase()}`;
}

/** Every read answers null once the bytes run out or hold something the writer never writes. */
class Reader {
  private position = 1;

  constructor(
    private readonly bytes: Uint8Array,
    private readonly end: number,
  ) {}

  theme(): SharedTheme | null {
    const seed = this.color();
    const scheme = this.byte();
    const target = this.byte();
    const contrast = this.byte();
    const sections = this.byte();
    if (seed === null || scheme === null || target === null || contrast === null || sections === null) return null;
    const style = STYLES[scheme & 0x0f];
    if (style === undefined || ((scheme >> 4) & 0x03) >= SPEC_COUNT) return null;
    if (target & TARGET_RESERVED) return null;
    const signed = contrast > 127 ? contrast - 256 : contrast;
    if (signed < -100 || signed > 100 || sections & SECTION_RESERVED) return null;

    const keyColors = sections & SECTION_KEY_COLORS ? this.keyColors() : [];
    if (keyColors === null) return null;
    const cmfTertiarySeed = sections & SECTION_CMF_SEED ? this.color() : null;
    if (sections & SECTION_CMF_SEED && cmfTertiarySeed === null) return null;
    const accents = sections & SECTION_ACCENTS ? this.accents() : [];
    if (accents === null) return null;
    if (sections & SECTION_PINS && !this.pins()) return null;
    const projectName = sections & SECTION_PROJECT_NAME ? this.text(MAX_PROJECT_NAME_BYTES) : null;
    if (sections & SECTION_PROJECT_NAME && !projectName) return null;
    if (sections & SECTION_TARGET_OPTIONS && !this.targetOptions()) return null;
    if (this.position !== this.end) return null;

    return {
      seed,
      style,
      library: LIBRARIES[target & 0x03]!,
      expressive: (target & EXPRESSIVE_FLAG) !== 0,
      contrast: nearestContrast(signed),
      keyColors,
      cmfTertiarySeed,
      accents,
      projectName,
    };
  }

  private byte(): number | null {
    return this.position < this.end ? this.bytes[this.position++]! : null;
  }

  private color(): string | null {
    const red = this.byte();
    const green = this.byte();
    const blue = this.byte();
    if (red === null || green === null || blue === null) return null;
    return hex((red << 16) | (green << 8) | blue);
  }

  private varint(): number | null {
    let value = 0;
    for (let index = 0; index < MAX_VARINT_BYTES; index++) {
      const next = this.byte();
      if (next === null) return null;
      value |= (next & 0x7f) << (7 * index);
      // A zero last byte after the first spells a smaller number the long way round.
      if (!(next & 0x80)) return index > 0 && next === 0 ? null : value;
    }
    return null;
  }

  private text(maxBytes: number = Number.MAX_SAFE_INTEGER): string | null {
    const length = this.varint();
    if (length === null || length > maxBytes || length > this.end - this.position) return null;
    const slice = this.bytes.subarray(this.position, this.position + length);
    this.position += length;
    try {
      return UTF8.decode(slice);
    } catch {
      return null;
    }
  }

  private tone(): number | null {
    const tone = this.byte();
    return tone !== null && tone <= MAX_TONE ? tone : null;
  }

  private keyColors(): string[] | null {
    const mask = this.byte();
    if (!mask || mask & KEY_COLORS_RESERVED) return null;
    const colors: string[] = [];
    for (let slot = 0; slot < KEY_COLOR_COUNT; slot++) {
      if (!(mask & (1 << slot))) continue;
      const color = this.color();
      if (color === null) return null;
      colors.push(color);
    }
    return colors;
  }

  private accents(): SharedAccent[] | null {
    const count = this.varint();
    if (!count || count > MAX_ACCENTS) return null;
    const accents: SharedAccent[] = [];
    for (let index = 0; index < count; index++) {
      const flags = this.byte();
      if (flags === null || flags & ACCENT_RESERVED) return null;
      const seed = this.color();
      const name = this.text(MAX_ACCENT_NAME_BYTES);
      if (seed === null || name === null) return null;
      if (flags & ACCENT_TONES) {
        const tones = [this.tone(), this.tone(), this.tone(), this.tone()];
        if (tones.includes(null)) return null;
        if (tones.every((tone, at) => tone === DEFAULT_ACCENT_TONES[at])) return null;
      }
      if (flags & ACCENT_THRESHOLD) {
        const threshold = this.byte();
        if (threshold === null || threshold >= THRESHOLD_COUNT || threshold === DEFAULT_THRESHOLD) return null;
      }
      accents.push({ name, seed });
    }
    return accents;
  }

  private pins(): boolean {
    const count = this.byte();
    if (!count) return false;
    let previous = -1;
    for (let index = 0; index < count; index++) {
      const role = this.byte();
      if (role === null || role >= ROLE_COUNT || role <= previous) return false;
      previous = role;
      const modes = this.byte();
      if (!modes || modes & PIN_RESERVED) return false;
      if (modes & PIN_LIGHT && this.color() === null) return false;
      if (modes & PIN_DARK && this.color() === null) return false;
    }
    return true;
  }

  private targetOptions(): boolean {
    const flags = this.byte();
    if (!flags || flags & OPTION_RESERVED) return false;
    if (flags & OPTION_MOTION_SCHEME && this.byte() !== MOTION_STANDARD) return false;
    if (flags & OPTION_THEME_NAME) {
      const themeName = this.text();
      if (themeName === null || themeName === DEFAULT_THEME_NAME) return false;
    }
    if (flags & OPTION_CUSTOM_TONES) {
      const count = this.byte();
      if (!count) return false;
      let previous = -1;
      for (let index = 0; index < count; index++) {
        const slot = this.byte();
        if (slot === null || !CUSTOM_SLOTS.has(slot) || slot <= previous) return false;
        previous = slot;
        const light = this.byte();
        const dark = this.byte();
        if (light === null || dark === null || !isToneOrNone(light) || !isToneOrNone(dark)) return false;
        // The writer drops a slot with neither tone moved.
        if (light === NO_TONE && dark === NO_TONE) return false;
      }
    }
    return true;
  }
}

/** How many bytes the writer's varint takes for [value]. */
function varintBytes(value: number): number {
  let bytes = 1;
  for (let rest = value >>> 7; rest > 0; rest >>>= 7) bytes++;
  return bytes;
}

function isToneOrNone(value: number): boolean {
  return value <= MAX_TONE || value === NO_TONE;
}
