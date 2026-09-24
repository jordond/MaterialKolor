// The Worker's bindings as wrangler.jsonc declares them. `ASSETS` is the built site.
declare namespace Cloudflare {
  interface Env {
    ASSETS: Fetcher;
  }
}

interface Env extends Cloudflare.Env {}

declare module '*.bin' {
  const data: ArrayBuffer;
  export default data;
}
