import { createServer, type IncomingMessage, type ServerResponse } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import path from 'node:path';
import { promisify } from 'node:util';
import { gzip } from 'node:zlib';

const gzipped = promisify(gzip);

const TYPES: Record<string, string> = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.wasm': 'application/wasm',
  '.json': 'application/json',
  '.map': 'application/json',
  '.css': 'text/css; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.ttf': 'font/ttf',
  '.otf': 'font/otf',
  '.woff2': 'font/woff2',
  '.txt': 'text/plain; charset=utf-8',
  '.webmanifest': 'application/manifest+json',
};

export interface Site {
  url: string;
  close: () => Promise<void>;
}

interface HeaderRule {
  pattern: RegExp;
  headers: Record<string, string>;
}

/** The types worth compressing. Images and woff2 are compressed already. */
const COMPRESSIBLE = new Set(['.html', '.js', '.mjs', '.wasm', '.json', '.map', '.css', '.svg', '.ttf', '.otf', '.txt', '.webmanifest']);

/**
 * Serve the built site in `root` on a free local port, the way the host does.
 *
 * A path that is a file gets the file and any other path without an extension gets `index.html`, so
 * `/t/<code>` opens the app. The page loads everything from root-absolute addresses, so nothing
 * under `/t/` is ever a file. The headers in the site's `_headers` apply too, the content security
 * policy and `Cache-Control` among them, so the hashed assets are cached for good and a path with
 * no rule of its own is `no-store`. Each run serves on a new port, so no cache outlives the build it
 * came from. Text and wasm go out gzipped to a browser that takes it, as the host sends them.
 */
export async function serveSite(root: string): Promise<Site> {
  const base = path.resolve(root);
  const rules = await readHeaderRules(base);
  const bodies = new Map<string, Promise<Body>>();
  const server = createServer((request, response) => {
    answer(base, rules, bodies, request, response).catch((error: unknown) => {
      console.error(`The site server failed on ${request.url}`, error);
      if (!response.headersSent) response.writeHead(500);
      response.end();
    });
  });
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  if (address === null || typeof address === 'string') throw new Error('The site server has no port');
  return {
    url: `http://127.0.0.1:${address.port}`,
    close: () => new Promise<void>((resolve) => server.close(() => resolve())),
  };
}

interface Body {
  plain: Buffer;
  gzip: Buffer | null;
}

async function answer(
  base: string,
  rules: HeaderRule[],
  bodies: Map<string, Promise<Body>>,
  request: IncomingMessage,
  response: ServerResponse,
): Promise<void> {
  const pathname = decodePath(request.url ?? '/');
  if (pathname === null) {
    response.writeHead(400).end();
    return;
  }
  const file = await findFile(base, pathname);
  if (!file) {
    response.writeHead(404).end();
    return;
  }
  let body = bodies.get(file);
  if (!body) {
    body = readBody(file);
    bodies.set(file, body);
    body.catch(() => bodies.delete(file));
  }
  const { plain, gzip } = await body;
  const acceptsGzip = /\bgzip\b/.test(String(request.headers['accept-encoding'] ?? ''));
  const sent = gzip !== null && acceptsGzip ? gzip : plain;
  response.writeHead(200, {
    'Cache-Control': 'no-store',
    ...headersFor(rules, pathname),
    'Content-Type': TYPES[path.extname(file)] ?? 'application/octet-stream',
    'Content-Length': String(sent.length),
    ...(gzip === null ? {} : { Vary: 'Accept-Encoding' }),
    ...(sent === gzip ? { 'Content-Encoding': 'gzip' } : {}),
  });
  response.end(sent);
}

/** The bytes of [file], and gzipped too when its type is worth it. */
async function readBody(file: string): Promise<Body> {
  const plain = await readFile(file);
  const gzip = COMPRESSIBLE.has(path.extname(file)) ? await gzipped(plain) : null;
  return { plain, gzip };
}

/** The decoded path of [url], or null when its escapes are malformed, `/%E0%A4%A` for example. */
function decodePath(url: string): string | null {
  try {
    return decodeURIComponent(new URL(url, 'http://localhost').pathname);
  } catch {
    return null;
  }
}

async function findFile(base: string, pathname: string): Promise<string | null> {
  const candidate = path.resolve(base, `.${pathname}`);
  const hostFile = HOST_FILES.has(path.relative(base, candidate));
  if (!hostFile && candidate.startsWith(base + path.sep) && (await isFile(candidate))) return candidate;
  const last = pathname.split('/').at(-1) ?? '';
  return path.extname(last) === '' ? path.join(base, 'index.html') : null;
}

/** Files that configure the host and are never served. */
const HOST_FILES = new Set(['_headers']);

async function isFile(candidate: string): Promise<boolean> {
  try {
    return (await stat(candidate)).isFile();
  } catch {
    return false;
  }
}

/** The rules of the site's `_headers` file, a path pattern with `*` for any run of characters, then indented headers. */
async function readHeaderRules(base: string): Promise<HeaderRule[]> {
  let text: string;
  try {
    text = await readFile(path.join(base, '_headers'), 'utf8');
  } catch {
    return [];
  }
  const rules: HeaderRule[] = [];
  for (const line of text.split('\n')) {
    if (line.trim() === '' || line.trim().startsWith('#')) continue;
    if (!/^\s/.test(line)) {
      const source = line.trim().split('*').map(escapeRegExp).join('.*');
      rules.push({ pattern: new RegExp(`^${source}$`), headers: {} });
      continue;
    }
    const colon = line.indexOf(':');
    const rule = rules.at(-1);
    if (rule && colon > 0) rule.headers[line.slice(0, colon).trim()] = line.slice(colon + 1).trim();
  }
  return rules;
}

function headersFor(rules: HeaderRule[], pathname: string): Record<string, string> {
  return Object.assign({}, ...rules.filter((rule) => rule.pattern.test(pathname)).map((rule) => rule.headers));
}

function escapeRegExp(text: string): string {
  return text.replace(/[.+?^${}()|[\]\\]/g, '\\$&');
}
