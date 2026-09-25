// The Worker's bindings as wrangler.jsonc declares them. `ASSETS` is the built site.
declare namespace Cloudflare {
  interface Env {
    ASSETS: Fetcher;
    /** The `X-Robots-Tag` every response built here carries, `noindex` on staging and unset on production. */
    ROBOTS_TAG?: string;
  }
}

interface Env extends Cloudflare.Env {}

declare module '*.bin' {
  const data: ArrayBuffer;
  export default data;
}
