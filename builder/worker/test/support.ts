// The shared share code vectors, read straight from the file the Kotlin codec tests write and check,
// and a way to spell codes the builder would never write.
import { expect } from 'vitest';
import json from '../../fixtures/share-codes.json';
import { crc8, VERSION } from '../src/code';
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

/** The code the builder writes for the default theme, and some the builder would never read. */
export const DEFAULT_CODE = 'AdllOwAAAAAT';
export const BAD_CODES: readonly [string, string][] = [
  ['corrupt', 'AdllOwAAAAAU'],
  ['truncated', 'AdllOwAAAA'],
  ['newer version', codeOf([VERSION + 1, 0xd9, 0x65, 0x3b, 0, 0, 0, 0])],
];
