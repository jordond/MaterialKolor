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
};

export interface Site {
  url: string;
  close: () => Promise<void>;
}

/**
 * Serve the built site in `root` on a free local port.
 *
 * Any path that is not a file gets `index.html`, the way the host sends `/t/<code>` to the app. The
 * page asks for its scripts relative to the address it was opened on, so `/t/abc/builder.js` is
 * served as `/builder.js` by dropping leading segments until a file matches.
 */
export async function serveSite(root: string): Promise<Site> {
  const base = path.resolve(root);
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
  const segments = pathname.split('/').filter((segment) => segment.length > 0);
  for (let start = 0; start < segments.length; start++) {
    const candidate = path.resolve(base, ...segments.slice(start));
    if (candidate.startsWith(base + path.sep) && (await isFile(candidate))) return candidate;
  }
  const last = segments.at(-1) ?? '';
  return path.extname(last) === '' ? path.join(base, 'index.html') : null;
}

async function isFile(candidate: string): Promise<boolean> {
  try {
    return (await stat(candidate)).isFile();
  } catch {
    return false;
  }
}
