import { defineConfig, devices } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve('.env') });

// After the .env, which the configuration of the tests reads
const {
  appUrl,
  isApiMocked,
  mockApiBaseUrl,
  mockAppUrl,
  mockOidcAuthority,
  mockOidcClientId,
  sessionStatePath
} = await import('./e2e/support/config');

/**
 * Regression tests of the dashboard. E2E_MOCK_API=false runs them against the real core-api and OpenID Connect provider
 * of the .env; otherwise they run against a core-api and a provider simulated in the browser. See the README.
 */
export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/global-setup.ts',
  // With the mocks each test has its own data, so they run in parallel; the real backend is shared, so one at a time
  fullyParallel: isApiMocked,
  workers: isApiMocked ? undefined : 1,
  forbidOnly: !!process.env.CI,
  // A regression must fail at the first time, so a flaky test shows up instead of being hidden by a retry
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: appUrl,
    trace: 'retain-on-failure',
    storageState: isApiMocked ? undefined : sessionStatePath,
  },
  projects: [
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
  ],
  webServer: isApiMocked
    ? {
      // Its own server, pointed at the mocks: one of the real backend may be running on the default port
      command: `npm run dev -- --port ${new URL(mockAppUrl).port} --strictPort`,
      url: mockAppUrl,
      reuseExistingServer: false,
      env: {
        VITE_API_BASE_URL: mockApiBaseUrl,
        VITE_OIDC_AUTHORITY: mockOidcAuthority,
        VITE_OIDC_CLIENT_ID: mockOidcClientId,
      },
    }
    : {
      command: 'npm run dev',
      url: appUrl,
      reuseExistingServer: true,
    },
});
