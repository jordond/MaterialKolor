import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { cloudflareTest } from '@cloudflare/vitest-pool-workers';
import { defineConfig } from 'vitest/config';

// The tests run inside workerd with the same wrangler.jsonc as the Worker, ASSETS included, so the
// site must be assembled first with `./gradlew :builder:apps:web:assembleSite`.
const SITE = fileURLToPath(new URL('../apps/web/build/site', import.meta.url));
if (!existsSync(SITE)) {
  throw new Error(`No site at ${SITE}, assemble it first with ./gradlew :builder:apps:web:assembleSite`);
}

export default defineConfig({
  plugins: [cloudflareTest({ wrangler: { configPath: './wrangler.jsonc' } })],
  test: { testTimeout: 15_000 },
});
