import { env, SELF } from 'cloudflare:test';
import { describe, expect, it } from 'vitest';
import { printable } from '../src/card';
import { decodeShareCode } from '../src/code';
import { renderCard } from '../src/og';
import { BAD_CODES, expectSiteHeaders, vector, vectors } from './support';

const ORIGIN = 'https://materialkolor.test';
const IMMUTABLE = 'public, max-age=31536000, immutable';
const PNG_SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
const CARD_LABELS = ['default', 'style Monochrome', 'three accents', 'non ascii project name'];

describe('/og/<code>.png', () => {
  it('draws a 1200 by 630 PNG with the site headers, cached for good', async () => {
    const response = await SELF.fetch(`${ORIGIN}/og/${vector('every section at once').code}.png`);
    expect(response.status).toBe(200);
    expect(response.headers.get('Content-Type')).toBe('image/png');
    expectSiteHeaders(response, IMMUTABLE);
    expectCard(new Uint8Array(await response.arrayBuffer()));
  });

  it('draws a different card for each of a few distinct themes', async () => {
    const cards = await Promise.all(CARD_LABELS.map((label) => renderCard(decodeShareCode(vector(label).code)!)));
    cards.forEach(expectCard);
    const digests = await Promise.all(cards.map(digestOf));
    expect(new Set(digests).size).toBe(CARD_LABELS.length);
  });

  it('draws the same bytes for the same code', async () => {
    const theme = decodeShareCode(vector('three accents').code)!;
    expect(await renderCard(theme)).toEqual(await renderCard(theme));
  });

  it.each([...BAD_CODES, ['missing', '.png'], ['not a png', 'AdllOwAAAAAT.jpg']])(
    'serves the default card for a %s code',
    async (_, path) => {
      const response = await SELF.fetch(`${ORIGIN}/og/${path.includes('.') ? path : `${path}.png`}`);
      const fallback = await env.ASSETS.fetch(`${ORIGIN}/og-default.png`);
      expect(response.status).toBe(200);
      expect(response.headers.get('Content-Type')).toBe('image/png');
      expectSiteHeaders(response, 'no-cache');
      expect(new Uint8Array(await response.arrayBuffer())).toEqual(new Uint8Array(await fallback.arrayBuffer()));
    },
  );

  it('answers HEAD with the headers and no body', async () => {
    const response = await SELF.fetch(`${ORIGIN}/og/${vectors[0]!.code}.png`, { method: 'HEAD' });
    expectSiteHeaders(response, IMMUTABLE);
    expect(await response.text()).toBe('');
  });
});

describe('card text', () => {
  it('keeps what the atlas can draw', () => {
    expect(printable('Café Olé ☕')).toBe('Cafe Ole');
    expect(printable('  Plant\tshop ')).toBe('Plant shop');
  });
});

function expectCard(png: Uint8Array): void {
  expect([...png.subarray(0, 8)]).toEqual(PNG_SIGNATURE);
  const header = new DataView(png.buffer, png.byteOffset + 16, 8);
  expect([header.getUint32(0), header.getUint32(4)]).toEqual([1200, 630]);
  expect(new TextDecoder().decode(png.subarray(png.length - 8, png.length - 4))).toBe('IEND');
}

async function digestOf(png: Uint8Array): Promise<string> {
  const digest = new Uint8Array(await crypto.subtle.digest('SHA-256', png));
  return [...digest].map((byte) => byte.toString(16).padStart(2, '0')).join('');
}
