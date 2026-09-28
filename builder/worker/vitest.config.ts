import { cloudflareTest } from '@cloudflare/vitest-pool-workers';
import { defineConfig } from 'vitest/config';

// The tests run inside workerd with the same wrangler.jsonc as the Worker, ASSETS included, so the
// site must be assembled first with `./gradlew :builder:apps:web:assembleSite`.
export default defineConfig({
  plugins: [cloudflareTest({ wrangler: { configPath: './wrangler.jsonc' } })],
});
