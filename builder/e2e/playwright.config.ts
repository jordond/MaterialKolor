import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  globalSetup: './global-setup.ts',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  timeout: 60_000,
  use: {
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    // b-213
    // The production smoke runs on Chromium only for now.
    { name: 'webkit', use: { ...devices['Desktop Safari'] }, testIgnore: /smoke\.spec\.ts/ },
    // b-302
    // b-503
    // Firefox runs the whole suite except the production smoke. CI runs Chromium only, see builder.yml.
    { name: 'firefox', use: { ...devices['Desktop Firefox'] }, testIgnore: /smoke\.spec\.ts/ },
  ],
});
