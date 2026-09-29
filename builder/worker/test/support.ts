// The shared share code vectors, read straight from the file the Kotlin codec tests write and check,
// and a way to spell codes the builder would never write.
import { expect } from 'vitest';
import json from '../../fixtures/share-codes.json';
import { crc8, THEME_NAME_ALLOWANCE_BYTES, VERSION } from '../src/code';
import { CONTENT_SECURITY_POLICY } from '../src/headers';

/** The headers every Worker response carries, with [cacheControl] for its route. */
export function expectSiteHeaders(response: Response, cacheControl: string): void {
  expect(response.headers.get('Content-Security-Policy')).toBe(CONTENT_SECURITY_POLICY);
  expect(response.headers.get('X-Content-Type-Options')).toBe('nosniff');
  expect(response.headers.get('Referrer-Policy')).toBe('strict-origin-when-cross-origin');
  expect(response.headers.get('Cache-Control')).toBe(cacheControl);
}

export interface Vector {
  readonly label: string;
  readonly code: string;
  readonly seedHex: string;
  readonly library: string;
  readonly style: string;
  readonly projectName: string | null;
  readonly document: {
    readonly expressive: boolean;
    readonly contrast: number;
    readonly keyColors: Readonly<Record<string, string | null>>;
    readonly cmfTertiarySeed: string | null;
    readonly accents: readonly { readonly name: string; readonly seed: string }[];
    readonly pins: Readonly<Record<string, unknown>>;
  };
}

export const vectors: readonly Vector[] = json;

export function vector(label: string): Vector {
  const found = vectors.find((vector) => vector.label === label);
  if (found === undefined) throw new Error(`No share vector labelled '${label}'`);
  return found;
}

/** The share code for [bytes], a checksum added, spelled the way the Kotlin writer spells it. */
export function codeOf(bytes: readonly number[]): string {
  const body = Uint8Array.from(bytes);
  return spell(Uint8Array.from([...body, crc8(body)]));
}

/** [bytes] as unpadded base64url. */
export function spell(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

/** A code with the default seed and [name] as its project name, in raw bytes. */
export function namedCode(name: readonly number[]): string {
  return codeOf([VERSION, 0xd9, 0x65, 0x3b, 0, 0, 0, 0x10, name.length, ...name]);
}

// Every custom slot code, the gaps left by retired slots skipped.
const CUSTOM_SLOTS = [...range(18), ...range(11).map((slot) => slot + 30), 46, 47, 48];

/**
 * The longest code the builder writes, every section present and at its cap, with a theme name of
 * [themeNameBytes]. Spelled out by hand here so it checks the sum behind `MAX_CODE_LENGTH`.
 */
export function longestCode(themeNameBytes: number = THEME_NAME_ALLOWANCE_BYTES): string {
  // Every flag set, a seed, a distinct 24 byte name, tones off the defaults and threshold 1.
  const accents = range(8).flatMap((index) => [
    ...[0x07, 0x10, 0x20, 0x30],
    ...[24, ...Array<number>(23).fill(0x61), 0x61 + index],
    ...[41, 90, 80, 30, 1],
  ]);
  const pins = range(48).flatMap((role) => [role, 0x03, 1, 2, 3, 4, 5, 6]);
  const options = [0x07, 0, ...varint(themeNameBytes), ...Array<number>(themeNameBytes).fill(0x54), 32];
  return codeOf([
    ...[VERSION, 0xd9, 0x65, 0x3b, 0, 0, 0, 0x3f],
    ...[0x3f, ...Array<number>(18).fill(0x11)],
    ...[0x22, 0x33, 0x44],
    ...[8, ...accents],
    ...[48, ...pins],
    ...[48, ...Array<number>(48).fill(0x62)],
    ...options,
    ...CUSTOM_SLOTS.flatMap((slot) => [slot, 50, 0xff]),
  ]);
}

function range(count: number): number[] {
  return Array.from({ length: count }, (_, index) => index);
}

function varint(value: number): number[] {
  const bytes: number[] = [];
  for (; value >= 0x80; value >>>= 7) bytes.push((value & 0x7f) | 0x80);
  return [...bytes, value];
}

/** The code the builder writes for the default theme, and some the builder would never read. */
export const DEFAULT_CODE = 'AdllOyAAAADd';
export const BAD_CODES: readonly [string, string][] = [
  ['corrupt', 'AdllOwAAAAAU'],
  ['truncated', 'AdllOwAAAA'],
  ['newer version', codeOf([VERSION + 1, 0xd9, 0x65, 0x3b, 0, 0, 0, 0])],
  ['over the length cap', longestCode(THEME_NAME_ALLOWANCE_BYTES + 1)],
  ['custom slot with neither tone', codeOf([VERSION, 0xd9, 0x65, 0x3b, 0, 0, 0, 0x20, 0x02, 1, 0, 0xff, 0xff])],
];
