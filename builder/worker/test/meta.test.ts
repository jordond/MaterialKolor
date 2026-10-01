import { env, SELF } from 'cloudflare:test';
import { describe, expect, it } from 'vitest';
import plugin from '../../../build-logic/convention/src/main/kotlin/com/materialkolor/convention/plugin/BuilderWebPlugin.kt?raw';
import { decodeShareCode } from '../src/code';
import { cardAlt, themeDescription } from '../src/copy';
import { CONTENT_SECURITY_POLICY } from '../src/headers';
import { BAD_CODES, expectSiteHeaders, namedCode, vectors } from './support';

const ORIGIN = 'https://materialkolor.test';

describe('/t/<code>', () => {
  it.each(vectors.map((vector) => [vector.label, vector] as const))('rewrites the preview meta for %s', async (_, vector) => {
    const response = await SELF.fetch(`${ORIGIN}/t/${vector.code}`);
    expect(response.status).toBe(200);
    const meta = await metaOf(response);
    const theme = decodeShareCode(vector.code)!;
    const card = `${ORIGIN}/og/${vector.code}.png`;
    const title = vector.projectName === null ? 'MaterialKolor Builder' : `${vector.projectName}, a MaterialKolor theme`;
    expect(meta.get('title')).toEqual([title]);
    expect(meta.get('og:title')).toEqual([title]);
    expect(meta.get('description')).toEqual([themeDescription(theme)]);
    expect(meta.get('og:description')).toEqual([themeDescription(theme)]);
    expect(meta.get('og:description')![0]).toContain(vector.seedHex);
    expect(meta.get('og:url')).toEqual([`${ORIGIN}/t/${vector.code}`]);
    expect(meta.get('canonical')).toEqual([`${ORIGIN}/t/${vector.code}`]);
    expect(meta.get('og:image')).toEqual([card]);
    expect(meta.get('twitter:image')).toEqual([card]);
    expect(meta.get('og:image:alt')).toEqual([cardAlt(theme)]);
    expect(meta.get('twitter:image:alt')).toEqual([cardAlt(theme)]);
    expect(meta.get('theme-color')).toEqual([vector.seedHex, vector.seedHex]);
  });

  it('builds every URL from the origin it was asked on', async () => {
    const code = vectors[0]!.code;
    const meta = await metaOf(await SELF.fetch(`https://next.materialkolor.test/t/${code}`));
    expect(meta.get('og:url')).toEqual([`https://next.materialkolor.test/t/${code}`]);
    expect(meta.get('og:image')).toEqual([`https://next.materialkolor.test/og/${code}.png`]);
  });

  it('escapes a project name that tries to leave its attribute', async () => {
    // The page title holds the same name as plain text, where a quote is harmless, so this looks
    // for the attribute it would add rather than the characters.
    const name = [...new TextEncoder().encode('x" onload="alert(1)')];
    const html = await (await SELF.fetch(`${ORIGIN}/t/${namedCode(name)}`)).text();
    let injected = false;
    const found = () => {
      injected = true;
    };
    await new HTMLRewriter().on('[onload]', { element: found }).transform(new Response(html)).text();
    expect(injected).toBe(false);
    expect(html).toContain('og:title');
  });

  it('escapes a project name that tries to leave the page title', async () => {
    // The meta attributes hold the same name, where a bracket is harmless, so this reads the title alone.
    const name = [...new TextEncoder().encode('</title><script>alert(1)</script>')];
    const html = await (await SELF.fetch(`${ORIGIN}/t/${namedCode(name)}`)).text();
    const title = /<title>([\s\S]*?)<\/title>/.exec(html)?.[1] ?? '';
    expect(title).toContain('&lt;/title');
    expect(title).toContain('&lt;script');
    expect(title).toContain(', a MaterialKolor theme');
  });

  it.each([...BAD_CODES, ['missing', ''], ['not base64url', 'Adll%21'], ['nested', 'AdllOwAAAAAT/x']])(
    'serves the page unchanged for a %s code',
    async (_, code) => {
      const response = await SELF.fetch(`${ORIGIN}/t/${code}`);
      const page = await env.ASSETS.fetch(`${ORIGIN}/`);
      expect(response.status).toBe(200);
      expectSiteHeaders(response, 'no-cache');
      expect(await response.text()).toBe(await page.text());
    },
  );

  it('sets the site headers on a rewritten page', async () => {
    const response = await SELF.fetch(`${ORIGIN}/t/${vectors[0]!.code}`);
    expectSiteHeaders(response, 'no-cache');
    expect(response.headers.get('Content-Type')).toContain('text/html');
    await response.body?.cancel();
  });

  it('answers HEAD with the headers and no body', async () => {
    const response = await SELF.fetch(`${ORIGIN}/t/${vectors[0]!.code}`, { method: 'HEAD' });
    expectSiteHeaders(response, 'no-cache');
    expect(await response.text()).toBe('');
  });
});

describe('site headers', () => {
  it('copy the policy and headers the site build writes', () => {
    const literal = /CONTENT_SECURITY_POLICY =\s*((?:"[^"]*"\s*\+\s*)*"[^"]*")/.exec(plugin)?.[1] ?? '';
    const policy = [...literal.matchAll(/"([^"]*)"/g)].map((match) => match[1]).join('');
    expect(policy).toBe(CONTENT_SECURITY_POLICY);
    expect(plugin).toContain('X-Content-Type-Options: nosniff');
    expect(plugin).toContain('Referrer-Policy: strict-origin-when-cross-origin');
    expect(plugin).toMatch(/appendLine\("\/index\.html"\)\s+appendLine\(" {2}Cache-Control: no-cache"\)/);
    expect(plugin).toContain('Cache-Control: public, max-age=31536000, immutable');
  });
});

/** Every meta tag's content by its property or name, the canonical link and the title, in page order. */
async function metaOf(response: Response): Promise<Map<string, string[]>> {
  const found = new Map<string, string[]>();
  const add = (key: string, value: string) => found.set(key, [...(found.get(key) ?? []), value]);
  let title = '';
  await new HTMLRewriter()
    .on('head > title', {
      text(chunk) {
        title += chunk.text;
      },
    })
    .on('meta', {
      element(element) {
        const key = element.getAttribute('property') ?? element.getAttribute('name');
        if (key !== null) add(key, element.getAttribute('content') ?? '');
      },
    })
    .on('link[rel="canonical"]', {
      element(element) {
        add('canonical', element.getAttribute('href') ?? '');
      },
    })
    .transform(response)
    .text();
  add('title', title);
  return found;
}
