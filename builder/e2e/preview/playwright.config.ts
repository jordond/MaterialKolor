import path from 'node:path';
import { defineConfig, devices } from '@playwright/test';

// The screenshot the pull request preview comment shows, `npm run pr-screenshot`. One test on
// Chromium, against the site the e2e global setup serves.
export default defineConfig({
  testDir: '.',
  testMatch: /screenshot\.spec\.ts/,
  globalSetup: path.join(__dirname, '../global-setup.ts'),
  outputDir: path.join(__dirname, 'test-results'),
  retries: 1,
  reporter: 'list',
  timeout: 90_000,
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } }],
});
