import path from 'node:path';
import { defineConfig, devices } from '@playwright/test';

// The nightly perf run, `npm run perf`. One worker on Chromium, so the timings do not compete with
// each other. It reuses the e2e global setup, which checks the built site and names its folder.
export default defineConfig({
  testDir: '.',
  testMatch: /perf\.spec\.ts/,
  globalSetup: path.join(__dirname, '../global-setup.ts'),
  outputDir: path.join(__dirname, 'report/test-results'),
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'list',
  timeout: 300_000,
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } } }],
});
