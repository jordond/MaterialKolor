import { createServer } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import path from 'node:path';

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
  // b-501b
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

/**
 * Serve the built site in `root` on a free local port, the way the host does.
 *
 * A path that is a file gets the file and any other path without an extension gets `index.html`, so
 * `/t/<code>` opens the app. The page loads everything from root-absolute addresses, so nothing
 * under `/t/` is ever a file. The headers in the site's `_headers` apply too, the content security
 * policy among them, except `Cache-Control`. Every answer is `no-store` so a rebuilt site is never
 * served from a cache.
 */
export async function serveSite(root: string): Promise<Site> {
  const base = path.resolve(root);
  const rules = await readHeaderRules(base);
  const server = createServer(async (request, response) => {
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
    const body = await readFile(file);
    response.writeHead(200, {
      ...headersFor(rules, pathname),
      'Content-Type': TYPES[path.extname(file)] ?? 'application/octet-stream',
      'Cache-Control': 'no-store',
    });
    response.end(body);
  });
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  if (address === null || typeof address === 'string') throw new Error('The site server has no port');
  return {
    url: `http://127.0.0.1:${address.port}`,
    close: () => new Promise<void>((resolve) => server.close(() => resolve())),
  };
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
