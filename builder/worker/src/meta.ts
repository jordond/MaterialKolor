// The page a `/t/<code>` link serves, the app's own index.html with the preview meta set per theme.
//
// index.html holds one tag per rewritten property, so each selector matches by attribute. Every
// value goes through setAttribute or setInnerContent as text, which escape it. URLs come from the
// request's own origin, so links made on next point at next.
import { decodeShareCode, type SharedTheme } from './code';
import { cardAlt, themeDescription, themeTitle } from './copy';
import { NO_CACHE, withSiteHeaders } from './headers';

/** The app page for [code], with per-theme meta when it reads, or unchanged when it does not. */
export async function themePage(request: Request, env: Env, code: string | null): Promise<Response> {
  const url = new URL(request.url);
  // A fresh request without the caller's validators, so the page always comes back whole.
  const page = await env.ASSETS.fetch(new URL('/', url));
  const body = request.method === 'HEAD' ? null : page.body;
  const theme = code === null ? null : decodeShareCode(code);
  if (theme === null || code === null || !page.ok) return withSiteHeaders(body, page.headers, NO_CACHE, page.status);
  const rewritten = rewriter(theme, url.origin, code).transform(new Response(body, page));
  return withSiteHeaders(rewritten.body, page.headers, NO_CACHE);
}

/** What the site itself serves for [request], with the site headers, for when the theme page fails. */
export async function defaultPage(request: Request, env: Env): Promise<Response> {
  const page = await env.ASSETS.fetch(request);
  return withSiteHeaders(request.method === 'HEAD' ? null : page.body, page.headers, NO_CACHE, page.status);
}

function rewriter(theme: SharedTheme, origin: string, code: string): HTMLRewriter {
  const pageUrl = `${origin}/t/${code}`;
  const cardUrl = `${origin}/og/${code}.png`;
  const alt = cardAlt(theme);
  const title = themeTitle(theme);
  const description = themeDescription(theme);
  const rewriter = new HTMLRewriter()
    .on('meta[name="description"]', setting('content', description))
    .on('meta[property="og:description"]', setting('content', description))
    .on('meta[property="og:url"]', setting('content', pageUrl))
    .on('link[rel="canonical"]', setting('href', pageUrl))
    .on('meta[property="og:image"]', setting('content', cardUrl))
    .on('meta[property="og:image:alt"]', setting('content', alt))
    .on('meta[name="twitter:image"]', setting('content', cardUrl))
    .on('meta[name="twitter:image:alt"]', setting('content', alt))
    // The seed as it is, not the app chrome, which only the Kotlin engine can work out.
    .on('meta[name="theme-color"]', setting('content', theme.seed));
  if (title === null) return rewriter;
  return rewriter.on('head > title', titled(title)).on('meta[property="og:title"]', setting('content', title));
}

function setting(attribute: string, value: string): HTMLRewriterElementContentHandlers {
  return {
    element(element) {
      element.setAttribute(attribute, value);
    },
  };
}

function titled(text: string): HTMLRewriterElementContentHandlers {
  return {
    element(element) {
      element.setInnerContent(text, { html: false });
    },
  };
}
