// The versions each library an export depends on has published, for `GET /api/versions`, so a
// released builder names current versions instead of the ones it was built with. Read from Maven
// metadata and kept for six hours, in KV when the `VERSIONS` binding is there and in the Cache API
// when it is not.
import { NO_CACHE, withSiteHeaders } from './headers';

/** Where each library's Maven metadata lives, under the key the builder reads it by. */
export const LIBRARY_METADATA = {
  materialKolor: 'https://repo1.maven.org/maven2/com/materialkolor/material-kolor-core/maven-metadata.xml',
  composeMaterial3: 'https://repo1.maven.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml',
  androidxMaterial3: 'https://dl.google.com/android/maven2/androidx/compose/material3/material3/maven-metadata.xml',
  composeUnstyled: 'https://repo1.maven.org/maven2/com/composables/composeunstyled-theming/maven-metadata.xml',
  fluent: 'https://repo1.maven.org/maven2/io/github/compose-fluent/fluent/maven-metadata.xml',
  inklet: 'https://repo1.maven.org/maven2/dev/ggoggam/inklet/inklet/maven-metadata.xml',
} as const;

export type Library = keyof typeof LIBRARY_METADATA;

/** The body `/api/versions` serves. A library whose metadata could not be read is left out. */
export type LibraryVersions = Partial<Record<Library, string[]>> & { fetchedAt: string };

export const VERSIONS_KEY = 'versions:v1';
export const VERSIONS_TTL_SECONDS = 6 * 60 * 60;
export const VERSIONS_CACHE_CONTROL = 'public, max-age=3600';
const SOURCE_TIMEOUT_MS = 3000;

const VERSIONS_BLOCK = /<versions>([\s\S]*?)<\/versions>/;
const VERSION = /<version>\s*([^<\s]+)\s*<\/version>/g;

/** The versions listed in [xml], oldest first as the metadata lists them, or none when it lists none. */
export function parseMetadata(xml: string): string[] {
  const block = VERSIONS_BLOCK.exec(xml)?.[1];
  if (block === undefined) return [];
  return [...block.matchAll(VERSION)].map((match) => match[1]!);
}

/** The answer to `/api/versions`, from the cache when it holds one and from Maven when it does not. */
export async function libraryVersions(
  request: Request,
  env: Env,
  context: ExecutionContext,
  fetcher: typeof fetch = fetch,
): Promise<Response> {
  const cacheKey = new Request(new URL('/api/versions', request.url));
  const cached = await readCache(env, cacheKey);
  if (cached !== null) return respond(request, cached, VERSIONS_CACHE_CONTROL);
  const versions = await fetchVersions(fetcher);
  const body = JSON.stringify(versions);
  // Nothing read at all is worth neither keeping nor letting a browser hold on to.
  if (Object.keys(versions).length === 1) return respond(request, body, NO_CACHE);
  try {
    writeCache(env, context, cacheKey, body);
  } catch {
    // Not kept this time, the next request reads Maven again.
  }
  return respond(request, body, VERSIONS_CACHE_CONTROL);
}

/** Every library's versions, read in parallel, each source given a few seconds before it is left out. */
export async function fetchVersions(fetcher: typeof fetch = fetch): Promise<LibraryVersions> {
  const libraries = Object.keys(LIBRARY_METADATA) as Library[];
  const read = await Promise.all(libraries.map((library) => readSource(fetcher, LIBRARY_METADATA[library])));
  const found = libraries.flatMap((library, index) => {
    const versions = read[index];
    return versions === null || versions === undefined ? [] : [[library, versions] as const];
  });
  return { ...Object.fromEntries(found), fetchedAt: new Date().toISOString() };
}

async function readSource(fetcher: typeof fetch, url: string): Promise<string[] | null> {
  try {
    const response = await fetcher(url, { signal: AbortSignal.timeout(SOURCE_TIMEOUT_MS) });
    if (!response.ok) return null;
    const versions = parseMetadata(await response.text());
    return versions.length === 0 ? null : versions;
  } catch {
    return null;
  }
}

async function readCache(env: Env, key: Request): Promise<string | null> {
  try {
    if (env.VERSIONS !== undefined) return await env.VERSIONS.get(VERSIONS_KEY);
    const hit = await caches.default.match(key);
    return hit === undefined ? null : await hit.text();
  } catch {
    // A cache that cannot be read is a miss, Maven still answers.
    return null;
  }
}

function writeCache(env: Env, context: ExecutionContext, key: Request, body: string): void {
  if (env.VERSIONS !== undefined) {
    context.waitUntil(env.VERSIONS.put(VERSIONS_KEY, body, { expirationTtl: VERSIONS_TTL_SECONDS }));
    return;
  }
  const entry = new Response(body, {
    headers: { 'Content-Type': 'application/json', 'Cache-Control': `public, max-age=${VERSIONS_TTL_SECONDS}` },
  });
  context.waitUntil(caches.default.put(key, entry));
}

function respond(request: Request, body: string, cacheControl: string): Response {
  const headers = new Headers({ 'Content-Type': 'application/json; charset=utf-8' });
  return withSiteHeaders(request.method === 'HEAD' ? null : body, headers, cacheControl);
}
