import { describe, expect, it } from 'vitest';
import {
  decodeBase64Url,
  decodeShareCode,
  MAX_CODE_LENGTH,
  nearestContrast,
  THEME_NAME_ALLOWANCE_BYTES,
  VERSION,
} from '../src/code';
import { BAD_CODES, codeOf, DEFAULT_CODE, longestCode, namedCode, spell, vector, vectors } from './support';

// The Kotlin encoder writes these, so this is where the two sides are held to one format.
describe('share vectors', () => {
  it.each(vectors.map((vector) => [vector.label, vector] as const))('%s', (_, vector) => {
    const theme = decodeShareCode(vector.code);
    expect(theme).not.toBeNull();
    expect(theme!.seed).toBe(vector.seedHex);
    expect(theme!.library).toBe(vector.library);
    expect(theme!.style).toBe(vector.style);
    expect(theme!.projectName).toBe(vector.projectName);
    expect(theme!.expressive).toBe(vector.document.expressive);
    // The vector holds the level it was written with, the app opens it at the nearest named one.
    expect(theme!.contrast).toBe(nearestContrast(vector.document.contrast));
    expect(theme!.keyColors).toEqual(Object.values(vector.document.keyColors).filter((color) => color !== null));
    expect(theme!.cmfTertiarySeed).toBe(vector.document.cmfTertiarySeed);
    expect(theme!.accents).toEqual(vector.document.accents.map(({ name, seed }) => ({ name, seed })));
  });

  it('hold a code with every section before the project name', () => {
    const everything = vectors.find(
      ({ projectName, document }) =>
        projectName !== null &&
        document.cmfTertiarySeed !== null &&
        document.accents.length > 0 &&
        Object.keys(document.pins).length > 0 &&
        Object.values(document.keyColors).some((color) => color !== null),
    );
    expect(everything).toBeDefined();
  });

  it('open a level in between at the named level the app picks', () => {
    expect(decodeShareCode(vector('contrast -37').code)!.contrast).toBe(0);
    expect(decodeShareCode(vector('contrast 50').code)!.contrast).toBe(50);
    expect(decodeShareCode(vector('contrast -100').code)!.contrast).toBe(-100);
  });

  it('start with the default theme', () => {
    expect(vector('default').code).toBe(DEFAULT_CODE);
    // New themes ask for the newest spec, the 0x20 in the flags.
    expect(codeOf([VERSION, 0xd9, 0x65, 0x3b, 0x20, 0, 0, 0])).toBe(DEFAULT_CODE);
  });
});

describe('nearestContrast', () => {
  it('breaks a tie toward Standard, the way ContrastLevel.nearest does', () => {
    expect(nearestContrast(25)).toBe(0);
    expect(nearestContrast(-50)).toBe(0);
    expect(nearestContrast(75)).toBe(50);
    expect(nearestContrast(76)).toBe(100);
    expect(nearestContrast(-51)).toBe(-100);
    for (const level of [-100, 0, 50, 100]) expect(nearestContrast(level)).toBe(level);
  });
});

describe('codes the builder would not read', () => {
  const code = vector('every section at once').code;

  it.each(BAD_CODES)('%s', (_, bad) => {
    expect(decodeShareCode(bad)).toBeNull();
  });

  it('refuses every single flipped bit', () => {
    const bytes = decodeBase64Url(code)!;
    for (let bit = 0; bit < bytes.length * 8; bit++) {
      const flipped = bytes.slice();
      flipped[bit >> 3]! ^= 1 << (bit & 7);
      expect(decodeShareCode(spell(flipped)), `bit ${bit}`).toBeNull();
    }
  });

  it('refuses every truncation', () => {
    for (let length = 0; length < code.length; length++) {
      expect(decodeShareCode(code.slice(0, length)), `${length} characters`).toBeNull();
    }
  });

  it('refuses reserved bits and bytes the writer never writes', () => {
    const header = [VERSION, 0xd9, 0x65, 0x3b];
    expect(decodeShareCode(codeOf([...header, 0x0a, 0, 0, 0])), 'style 10').toBeNull();
    expect(decodeShareCode(codeOf([...header, 0x30, 0, 0, 0])), 'spec 3').toBeNull();
    expect(decodeShareCode(codeOf([...header, 0, 0x08, 0, 0])), 'target bit').toBeNull();
    expect(decodeShareCode(codeOf([...header, 0, 0, 101, 0])), 'contrast 101').toBeNull();
    expect(decodeShareCode(codeOf([...header, 0, 0, 0, 0x40])), 'section bit').toBeNull();
    expect(decodeShareCode(codeOf([...header, 0, 0, 0, 0, 0])), 'trailing byte').toBeNull();
    expect(decodeShareCode(codeOf([...header, 0, 0, 0, 0x20, 0x01, 1])), 'default motion').toBeNull();
    expect(decodeShareCode(codeOf([...header, 0, 0, 0, 0x20, 0x01, 0]))).not.toBeNull();
  });

  it('refuses a project name over the cap, empty or in broken UTF-8', () => {
    expect(decodeShareCode(namedCode(Array(48).fill(0x61)))?.projectName).toBe('a'.repeat(48));
    expect(decodeShareCode(namedCode(Array(49).fill(0x61)))).toBeNull();
    expect(decodeShareCode(namedCode([0xc3, 0x28]))).toBeNull();
    expect(decodeShareCode(namedCode([]))).toBeNull();
  });

  it('reads the longest code up to the length cap and nothing longer', () => {
    const longest = longestCode();
    expect(longest.length).toBe(MAX_CODE_LENGTH);
    expect(decodeShareCode(longest)?.projectName).toBe('b'.repeat(48));
    expect(decodeShareCode(longest)?.accents).toHaveLength(8);
    const over = longestCode(THEME_NAME_ALLOWANCE_BYTES + 1);
    expect(over.length).toBeGreaterThan(MAX_CODE_LENGTH);
    expect(decodeShareCode(over)).toBeNull();
    expect(decodeShareCode(DEFAULT_CODE.padEnd(MAX_CODE_LENGTH + 1, 'A'))).toBeNull();
  });

  it('refuses text that is not canonical base64url', () => {
    for (const text of ['', 'A', '!!!!', `${DEFAULT_CODE}=`, 'AdllOwAAAAA+', 'AdllOwAAAAAT0']) {
      expect(decodeShareCode(text), text).toBeNull();
    }
  });
});
