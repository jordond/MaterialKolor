// The headers every response the Worker builds carries, since `_headers` rules may not reach one.
//
// The policy is CONTENT_SECURITY_POLICY in
// build-logic/convention/src/main/kotlin/com/materialkolor/convention/plugin/BuilderWebPlugin.kt,
// copied verbatim. The two must stay identical, and a test reads the Kotlin file to hold them so.
export const CONTENT_SECURITY_POLICY =
  "default-src 'self'; script-src 'self' 'wasm-unsafe-eval' https://static.cloudflareinsights.com; " +
  "connect-src 'self' https://fonts.gstatic.com https://cloudflareinsights.com; " +
  "font-src 'self' https://fonts.gstatic.com; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline'; " +
  "frame-ancestors 'none'";

export const NO_CACHE = 'no-cache';
export const IMMUTABLE = 'public, max-age=31536000, immutable';

export function withSiteHeaders(
  body: BodyInit | null,
  source: Headers,
  cacheControl: string,
  status = 200,
): Response {
  const headers = new Headers();
  const contentType = source.get('Content-Type');
  if (contentType !== null) headers.set('Content-Type', contentType);
  headers.set('Content-Security-Policy', CONTENT_SECURITY_POLICY);
  headers.set('X-Content-Type-Options', 'nosniff');
  headers.set('Referrer-Policy', 'strict-origin-when-cross-origin');
  headers.set('Cache-Control', cacheControl);
  return new Response(body, { status, headers });
}

export function withRobotsTag(response: Response, tag: string | undefined): Response {
  if (tag === undefined) return response;
  const tagged = new Response(response.body, response);
  tagged.headers.set('X-Robots-Tag', tag);
  return tagged;
}
