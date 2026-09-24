// The Worker in front of the builder site. wrangler.jsonc sends only `/t/*` and `/og/*` here, and
// every other path is plain static assets with the single page fallback, never touching this code.
import { themePage } from './meta';
import { themeCard } from './og';

// The base64url alphabet, the only characters a share code is written in.
const THEME_PATH = /^\/t\/([A-Za-z0-9_-]+)$/;
const CARD_PATH = /^\/og\/([A-Za-z0-9_-]+)\.png$/;

export default {
  async fetch(request, env, context): Promise<Response> {
    const { pathname } = new URL(request.url);
    if (request.method !== 'GET' && request.method !== 'HEAD') return env.ASSETS.fetch(request);
    if (pathname.startsWith('/t/')) return themePage(request, env, THEME_PATH.exec(pathname)?.[1] ?? null);
    if (pathname.startsWith('/og/')) return themeCard(request, env, context, CARD_PATH.exec(pathname)?.[1] ?? null);
    return env.ASSETS.fetch(request);
  },
} satisfies ExportedHandler<Env>;
