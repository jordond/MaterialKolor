import { env, SELF } from 'cloudflare:test';
import { describe, expect, it } from 'vitest';
import { printable } from '../src/card';
import { decodeShareCode } from '../src/code';
import { renderCard } from '../src/og';
import { BAD_CODES, expectSiteHeaders, vector, vectors } from './support';

const ORIGIN = 'https://materialkolor.test';
const IMMUTABLE = 'public, max-age=31536000, immutable';
const PNG_SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];

describe('/og/<code>.png', () => {
  it('draws a 1200 by 630 PNG with the site headers, cached for good', async () => {
    const response = await SELF.fetch(`${ORIGIN}/og/${vector('every section at once').code}.png`);
    expect(response.status).toBe(200);
    expect(response.headers.get('Content-Type')).toBe('image/png');
    expectSiteHeaders(response, IMMUTABLE);
    expectCard(new Uint8Array(await response.arrayBuffer()));
  });

  it.each(vectors.map((vector) => [vector.label, vector] as const))('draws the card for %s', async (_, vector) => {
    expectCard(await renderCard(decodeShareCode(vector.code)!));
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
}
