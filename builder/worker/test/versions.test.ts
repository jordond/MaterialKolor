import { createExecutionContext, env, waitOnExecutionContext } from 'cloudflare:test';
import { describe, expect, it } from 'vitest';
import worker from '../src/index';
import {
  LIBRARY_METADATA,
  libraryVersions,
  parseMetadata,
  VERSIONS_CACHE_CONTROL,
  VERSIONS_KEY,
  VERSIONS_TTL_SECONDS,
} from '../src/versions';
import { expectSiteHeaders } from './support';

const ORIGIN = 'https://materialkolor.test';

const METADATA = `<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>io.github.compose-fluent</groupId>
  <artifactId>fluent</artifactId>
  <version>v0.1.0</version>
  <versioning>
    <latest>v0.2.0-alpha01</latest>
    <release>v0.2.0-alpha01</release>
    <versions>
      <version>v0.0.1-dev01</version>
      <version>v0.1.0</version>
      <version> v0.2.0-alpha01 </version>
    </versions>
    <lastUpdated>20260901000000</lastUpdated>
  </versioning>
</metadata>`;

describe('Maven metadata', () => {
  it('reads the published versions, oldest first', () => {
    expect(parseMetadata(METADATA)).toEqual(['v0.0.1-dev01', 'v0.1.0', 'v0.2.0-alpha01']);
  });

  it('reads nothing from metadata without a version list', () => {
    expect(parseMetadata('<metadata><version>1.0.0</version></metadata>')).toEqual([]);
    expect(parseMetadata('<!doctype html>')).toEqual([]);
  });
});

describe('/api/versions', () => {
  it('serves what KV holds without reading Maven', async () => {
    const kv = new FakeKv({ [VERSIONS_KEY]: '{"fluent":["v0.1.0"],"fetchedAt":"then"}' });
    const maven = new FakeMaven();
    const response = await libraryVersions(request(), withKv(kv), createExecutionContext(), maven.fetch);
    expect(await response.json()).toEqual({ fluent: ['v0.1.0'], fetchedAt: 'then' });
    expect(response.headers.get('Content-Type')).toBe('application/json; charset=utf-8');
    expectSiteHeaders(response, VERSIONS_CACHE_CONTROL);
    expect(maven.asked).toEqual([]);
  });

  it('reads every source on a KV miss and keeps the answer in KV for six hours', async () => {
    const kv = new FakeKv({});
    const maven = new FakeMaven();
    const context = createExecutionContext();
    const response = await libraryVersions(request(), withKv(kv), context, maven.fetch);
    await waitOnExecutionContext(context);
    const body = (await response.json()) as Record<string, unknown>;
    expect(Object.keys(body).sort()).toEqual([...Object.keys(LIBRARY_METADATA), 'fetchedAt'].sort());
    expect(body.fluent).toEqual(['v0.0.1-dev01', 'v0.1.0', 'v0.2.0-alpha01']);
    expect(typeof body.fetchedAt).toBe('string');
    expect(maven.asked.sort()).toEqual(Object.values(LIBRARY_METADATA).sort());
    expect(kv.puts).toEqual([{ key: VERSIONS_KEY, value: JSON.stringify(body), ttl: VERSIONS_TTL_SECONDS }]);
    expectSiteHeaders(response, VERSIONS_CACHE_CONTROL);
  });

  it('leaves out a library whose source failed', async () => {
    const maven = new FakeMaven({
      [LIBRARY_METADATA.fluent]: new Response('Not found', { status: 404 }),
      [LIBRARY_METADATA.composeUnstyled]: new Error('Timed out'),
      [LIBRARY_METADATA.materialKolor]: new Response('<!doctype html>'),
    });
    const response = await libraryVersions(request(), withKv(new FakeKv({})), createExecutionContext(), maven.fetch);
    const body = (await response.json()) as Record<string, unknown>;
    expect(Object.keys(body).sort()).toEqual(['androidxMaterial3', 'composeMaterial3', 'fetchedAt', 'inklet']);
  });

  it('keeps nothing when every source failed', async () => {
    const kv = new FakeKv({});
    const maven = new FakeMaven(
      Object.fromEntries(Object.values(LIBRARY_METADATA).map((url) => [url, new Error('Offline')])),
    );
    const context = createExecutionContext();
    const response = await libraryVersions(request(), withKv(kv), context, maven.fetch);
    await waitOnExecutionContext(context);
    expect(Object.keys((await response.json()) as object)).toEqual(['fetchedAt']);
    expectSiteHeaders(response, 'no-cache');
    expect(kv.puts).toEqual([]);
  });

  it('keeps the answer in the Cache API without a KV binding', async () => {
    const origin = 'https://no-kv.materialkolor.test';
    const first = new FakeMaven();
    const context = createExecutionContext();
    await libraryVersions(request(origin), withoutKv(), context, first.fetch);
    await waitOnExecutionContext(context);
    const second = new FakeMaven();
    const response = await libraryVersions(request(origin), withoutKv(), createExecutionContext(), second.fetch);
    expect(first.asked).toHaveLength(Object.keys(LIBRARY_METADATA).length);
    expect(second.asked).toEqual([]);
    expect(((await response.json()) as Record<string, unknown>).fluent).toEqual([
      'v0.0.1-dev01',
      'v0.1.0',
      'v0.2.0-alpha01',
    ]);
  });

  it('is routed to the Worker with the robots tag on next', async () => {
    const kv = new FakeKv({ [VERSIONS_KEY]: '{"fetchedAt":"then","fluent":["v0.1.0"]}' });
    const next = { ...withKv(kv), ROBOTS_TAG: 'noindex' } as Env;
    const response = await worker.fetch(request(), next, createExecutionContext());
    expect(await response.json()).toEqual({ fetchedAt: 'then', fluent: ['v0.1.0'] });
    expect(response.headers.get('X-Robots-Tag')).toBe('noindex');
  });

  it('leaves every other path to the site', async () => {
    for (const path of ['/', '/api/other', '/api/versions/extra']) {
      const response = await worker.fetch(request(ORIGIN, path), throwingKv(), createExecutionContext());
      const site = await env.ASSETS.fetch(`${ORIGIN}${path}`);
      expect(response.status).toBe(site.status);
      expect(await response.text()).toBe(await site.text());
    }
  });
});

function request(origin: string = ORIGIN, path = '/api/versions'): Request<unknown, IncomingRequestCfProperties> {
  return new Request<unknown, IncomingRequestCfProperties>(`${origin}${path}`);
}

function withKv(kv: FakeKv): Env {
  return { ...env, VERSIONS: kv as unknown as KVNamespace } as Env;
}

/** Bindings with no KV, the way a deploy without the namespace runs, whatever wrangler.jsonc binds. */
function withoutKv(): Env {
  return { ...env, VERSIONS: undefined } as Env;
}

/** Bindings whose KV throws on any use, so a path that reaches it fails the test. */
function throwingKv(): Env {
  const kv = {
    get(): never {
      throw new Error('KV read on a site path');
    },
    put(): never {
      throw new Error('KV write on a site path');
    },
  };
  return { ...env, VERSIONS: kv as unknown as KVNamespace } as Env;
}

/** A KV namespace in memory that keeps each write it is handed. */
class FakeKv {
  readonly puts: { key: string; value: string; ttl: number | undefined }[] = [];

  constructor(private readonly held: Record<string, string>) {}

  async get(key: string): Promise<string | null> {
    return this.held[key] ?? null;
  }

  async put(key: string, value: string, options?: { expirationTtl?: number }): Promise<void> {
    this.puts.push({ key, value, ttl: options?.expirationTtl });
    this.held[key] = value;
  }
}

/** Maven, answering every url with [METADATA] unless [answers] says otherwise, and keeping the urls asked for. */
class FakeMaven {
  readonly asked: string[] = [];

  constructor(private readonly answers: Record<string, Response | Error> = {}) {}

  readonly fetch = (async (input: RequestInfo | URL): Promise<Response> => {
    const url = input instanceof Request ? input.url : input.toString();
    this.asked.push(url);
    const answer = this.answers[url];
    if (answer instanceof Error) throw answer;
    return answer ?? new Response(METADATA);
  }) as typeof fetch;
}
