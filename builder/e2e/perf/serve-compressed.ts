import { createServer } from 'node:http';
import { readdir, readFile, stat } from 'node:fs/promises';
import path from 'node:path';
import { brotliCompressSync, constants, gzipSync } from 'node:zlib';

// The site the way the host hands it to a browser, for the perf run only. Each file goes out brotli
// or gzip compressed by what the browser accepts, with the host's cache rules, so a throttled load
// moves the bytes a real one would and a repeat visit comes from the cache. The e2e server in
// `serve.ts` gzips on the fly and keeps no brotli, which suits tests and not timing.

const TYPES: Record<string, string> = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.wasm': 'application/wasm',
  '.json': 'application/json',
  '.css': 'text/css; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.ttf': 'font/ttf',
  '.otf': 'font/otf',
  '.woff2': 'font/woff2',
  '.txt': 'text/plain; charset=utf-8',
  '.webmanifest': 'application/manifest+json',
};

/** Types worth compressing. Images and woff2 are compressed already. */
const COMPRESSIBLE = new Set(['.html', '.js', '.mjs', '.wasm', '.json', '.css', '.svg', '.ttf', '.otf', '.txt', '.webmanifest']);

export interface PerfSite {
  url: string;
  /** The encodings the browser was sent, `br`, `gzip` or `identity`. */
  encodings: Set<string>;
  close: () => Promise<void>;
}

/**
 * Serve the built site in [root] on a free local port. Every compressible file is brotli compressed
 * up front at the quality `builder/apps/web/budget.json` measures with, so no request waits on it.
 */
export async function servePerfSite(root: string): Promise<PerfSite> {
  const base = path.resolve(root);
  const brotli = new Map<string, Buffer>();
  const gzip = new Map<string, Buffer>();
  for (const file of await compressibleFiles(base)) {
    brotli.set(
      file,
      brotliCompressSync(await readFile(file), {
        params: { [constants.BROTLI_PARAM_QUALITY]: 11, [constants.BROTLI_PARAM_LGWIN]: 22 },
      }),
    );
  }
  const encodings = new Set<string>();
  const server = createServer(async (request, response) => {
    const pathname = new URL(request.url ?? '/', 'http://localhost').pathname;
    const file = await findFile(base, decodeURIComponent(pathname));
    if (!file) {
      response.writeHead(404).end();
      return;
    }
    const accepted = String(request.headers['accept-encoding'] ?? '');
    let body = await readFile(file);
    let encoding = 'identity';
    if (brotli.has(file) && /\bbr\b/.test(accepted)) {
      body = brotli.get(file)!;
      encoding = 'br';
    } else if (brotli.has(file) && /\bgzip\b/.test(accepted)) {
      if (!gzip.has(file)) gzip.set(file, gzipSync(body, { level: 9 }));
      body = gzip.get(file)!;
      encoding = 'gzip';
    }
    encodings.add(encoding);
    response.writeHead(200, {
      'Content-Type': TYPES[path.extname(file)] ?? 'application/octet-stream',
      'Cache-Control': cacheControl(pathname),
      ...(encoding === 'identity' ? {} : { 'Content-Encoding': encoding }),
      Vary: 'Accept-Encoding',
      // Lets the page read transfer sizes in its resource timing.
      'Timing-Allow-Origin': '*',
    });
    response.end(body);
  });
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  if (address === null || typeof address === 'string') throw new Error('The perf server has no port');
  return {
    url: `http://127.0.0.1:${address.port}`,
    encodings,
    close: () => new Promise<void>((resolve) => server.close(() => resolve())),
  };
}

/** The host's rules from the `_headers` that `writeHeaders` writes. */
function cacheControl(pathname: string): string {
  if (pathname.startsWith('/assets/')) return 'public, max-age=31536000, immutable';
  if (pathname.startsWith('/composeResources/')) return 'public, max-age=86400, stale-while-revalidate=604800';
  return 'no-cache';
}

async function compressibleFiles(directory: string): Promise<string[]> {
  const files: string[] = [];
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const full = path.join(directory, entry.name);
    if (entry.isDirectory()) files.push(...(await compressibleFiles(full)));
    else if (COMPRESSIBLE.has(path.extname(entry.name)) && !entry.name.endsWith('.LICENSE.txt')) files.push(full);
  }
  return files;
}

async function findFile(base: string, pathname: string): Promise<string | null> {
  const candidate = path.resolve(base, `.${pathname}`);
  if (candidate.startsWith(base + path.sep) && path.basename(candidate) !== '_headers') {
    try {
      if ((await stat(candidate)).isFile()) return candidate;
    } catch {
      // Not a file, so maybe a page.
    }
  }
  return path.extname(pathname) === '' ? path.join(base, 'index.html') : null;
}
