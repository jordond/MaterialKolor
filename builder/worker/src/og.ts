// The card a `/og/<code>.png` link serves, drawn once per code and then served from the Cache API.
import { CARD_HEIGHT, CARD_WIDTH, drawCard } from './card';
import { decodeShareCode, type SharedTheme } from './code';
import { loadFonts } from './glyphs';
import { IMMUTABLE, NO_CACHE, withSiteHeaders } from './headers';
import { encodePng } from './png';

const DEFAULT_CARD = '/og-default.png';

/** The card for [code], or the site's default card when the code does not read. */
export async function themeCard(
  request: Request,
  env: Env,
  context: ExecutionContext,
  code: string | null,
): Promise<Response> {
  const url = new URL(request.url);
  const theme = code === null ? null : decodeShareCode(code);
  if (theme === null) {
    // Not cached for good, a code newer than this Worker may read after the next deploy.
    const fallback = await env.ASSETS.fetch(new URL(DEFAULT_CARD, url));
    return withSiteHeaders(bodyFor(request, fallback), fallback.headers, NO_CACHE, fallback.status);
  }
  // Keyed by the path alone, so a query string cannot make the Worker draw the same card again.
  const key = new Request(`${url.origin}${url.pathname}`);
  const cache = caches.default;
  const cached = await cache.match(key);
  if (cached !== undefined) return withBody(request, cached);
  const card = withSiteHeaders(await renderCard(theme), new Headers({ 'Content-Type': 'image/png' }), IMMUTABLE);
  context.waitUntil(cache.put(key, card.clone()));
  return withBody(request, card);
}

/** The card for [theme] as PNG bytes. */
export async function renderCard(theme: SharedTheme): Promise<Uint8Array> {
  const { palette, scanlines } = drawCard(theme, loadFonts());
  return encodePng(CARD_WIDTH, CARD_HEIGHT, palette, scanlines);
}

function bodyFor(request: Request, response: Response): ReadableStream | null {
  return request.method === 'HEAD' ? null : response.body;
}

function withBody(request: Request, response: Response): Response {
  if (request.method !== 'HEAD') return response;
  return new Response(null, { status: response.status, headers: response.headers });
}
