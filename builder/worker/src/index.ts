// The Worker in front of the builder site. wrangler.jsonc sends only `/t/*` and `/og/*` here, and
// every other path is plain static assets with the single page fallback, never touching this code.
import { defaultPage, themePage } from './meta';
import { defaultCard, themeCard } from './og';

// The base64url alphabet, the only characters a share code is written in.
const THEME_PATH = /^\/t\/([A-Za-z0-9_-]+)$/;
const CARD_PATH = /^\/og\/([A-Za-z0-9_-]+)\.png$/;

export default {
  async fetch(request, env, context): Promise<Response> {
    const { pathname } = new URL(request.url);
    if (request.method !== 'GET' && request.method !== 'HEAD') return env.ASSETS.fetch(request);
    try {
      if (pathname.startsWith('/t/')) return await themePage(request, env, THEME_PATH.exec(pathname)?.[1] ?? null);
      if (pathname.startsWith('/og/')) {
        return await themeCard(request, env, context, CARD_PATH.exec(pathname)?.[1] ?? null);
      }
    } catch {
      // A preview is never worth an error page, so a failure serves the site's own page or default card.
      return pathname.startsWith('/og/') ? defaultCard(request, env) : defaultPage(request, env);
    }
    return env.ASSETS.fetch(request);
  },
} satisfies ExportedHandler<Env>;
