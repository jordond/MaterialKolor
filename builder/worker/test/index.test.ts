import { createExecutionContext, env, waitOnExecutionContext } from 'cloudflare:test';
import { describe, expect, it } from 'vitest';
import worker from '../src/index';
import { expectSiteHeaders, namedCode, vectors } from './support';

const ORIGIN = 'https://materialkolor.test';

describe('a route that throws', () => {
  it('serves what the site serves for a theme page', async () => {
    const path = `/t/${vectors[0]!.code}`;
    const response = await worker.fetch(request(path), throwingOn('/'), createExecutionContext());
    const site = await env.ASSETS.fetch(`${ORIGIN}${path}`);
    expect(response.status).toBeLessThan(500);
    expect(response.status).toBe(site.status);
    expectSiteHeaders(response, 'no-cache');
    expect(await response.text()).toBe(await site.text());
  });

  it('serves the default card for a theme card', async () => {
    // A code no other test draws, so the card is not in the cache and the Worker reaches waitUntil.
    const path = `/og/${namedCode([...new TextEncoder().encode('Guard')])}.png`;
    const pending: Promise<unknown>[] = [];
    const response = await worker.fetch(request(path), env, throwingContext(pending));
    // The cache write started before waitUntil threw, so it finishes inside this test.
    expect(pending).toHaveLength(1);
    await Promise.all(pending);
    const fallback = await env.ASSETS.fetch(`${ORIGIN}/og-default.png`);
    expect(response.status).toBe(200);
    expect(response.headers.get('Content-Type')).toBe('image/png');
    expectSiteHeaders(response, 'no-cache');
    expect(new Uint8Array(await response.arrayBuffer())).toEqual(new Uint8Array(await fallback.arrayBuffer()));
  });
});

describe('the robots tag', () => {
  const staging = { ...env, ROBOTS_TAG: 'noindex' } as Env;
  const card = `/og/${namedCode([...new TextEncoder().encode('Robots')])}.png`;

  it.each([
    ['a theme page', `/t/${vectors[0]!.code}`, 'no-cache'],
    ['a code that does not read', '/t/AdllOwAAAAAU', 'no-cache'],
    ['a theme card', card, 'public, max-age=31536000, immutable'],
  ])('is noindex on %s on staging', async (_, path, cacheControl) => {
    const context = createExecutionContext();
    const response = await worker.fetch(request(path), staging, context);
    await waitOnExecutionContext(context);
    expect(response.headers.get('X-Robots-Tag')).toBe('noindex');
    expectSiteHeaders(response, cacheControl);
  });

  it('is noindex on a card out of the cache on staging', async () => {
    const first = createExecutionContext();
    await worker.fetch(request(card), staging, first);
    await waitOnExecutionContext(first);
    // A miss would hand a cache write to waitUntil, which throws here and turns into the default card.
    const pending: Promise<unknown>[] = [];
    const response = await worker.fetch(request(card), staging, throwingContext(pending));
    expect(pending).toHaveLength(0);
    expect(response.headers.get('X-Robots-Tag')).toBe('noindex');
    expectSiteHeaders(response, 'public, max-age=31536000, immutable');
  });

  it('is left off on production', async () => {
    const response = await worker.fetch(request(`/t/${vectors[0]!.code}`), env, createExecutionContext());
    expect(response.headers.get('X-Robots-Tag')).toBeNull();
  });
});

function request(path: string): Request<unknown, IncomingRequestCfProperties> {
  return new Request<unknown, IncomingRequestCfProperties>(`${ORIGIN}${path}`);
}

/** The test bindings, except that the site throws when asked for [pathname]. */
function throwingOn(pathname: string): Env {
  const assets = {
    fetch(input: RequestInfo | URL): Promise<Response> {
      const url = new URL(input instanceof Request ? input.url : input);
      if (url.pathname === pathname) throw new Error(`Injected failure for ${pathname}`);
      return env.ASSETS.fetch(input);
    },
  };
  return { ...env, ASSETS: assets } as unknown as Env;
}

/** A context that keeps the promise it is handed in [pending], then throws. */
function throwingContext(pending: Promise<unknown>[]): ExecutionContext {
  const context = {
    waitUntil(promise: Promise<unknown>): void {
      pending.push(promise);
      throw new Error('Injected failure in waitUntil');
    },
    passThroughOnException(): void {},
    props: {},
  };
  return context as unknown as ExecutionContext;
}
