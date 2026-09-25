// The Worker in front of the builder site. wrangler.jsonc sends only `/t/*` and `/og/*` here, and
// every other path is plain static assets with the single page fallback, never touching this code.
import { withRobotsTag } from './headers';
import { defaultPage, themePage } from './meta';
import { defaultCard, themeCard } from './og';

// The base64url alphabet, the only characters a share code is written in.
const THEME_PATH = /^\/t\/([A-Za-z0-9_-]+)$/;
const CARD_PATH = /^\/og\/([A-Za-z0-9_-]+)\.png$/;

export default {
  async fetch(request, env, context): Promise<Response> {
    const { pathname } = new URL(request.url);
    if (request.method !== 'GET' && request.method !== 'HEAD') return env.ASSETS.fetch(request);
    if (!pathname.startsWith('/t/') && !pathname.startsWith('/og/')) return env.ASSETS.fetch(request);
    return withRobotsTag(await linkPreview(request, env, context, pathname), env.ROBOTS_TAG);
  },
} satisfies ExportedHandler<Env>;

/** The theme page or the card for [pathname], which is under `/t/` or `/og/`. */
async function linkPreview(request: Request, env: Env, context: ExecutionContext, pathname: string): Promise<Response> {
  try {
    if (pathname.startsWith('/t/')) return await themePage(request, env, THEME_PATH.exec(pathname)?.[1] ?? null);
    return await themeCard(request, env, context, CARD_PATH.exec(pathname)?.[1] ?? null);
  } catch {
    // A preview is never worth an error page, so a failure serves the site's own page or default card.
    return pathname.startsWith('/og/') ? defaultCard(request, env) : defaultPage(request, env);
  }
}
