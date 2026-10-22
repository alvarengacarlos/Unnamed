/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // The unit tests (npm test). The e2e tests (e2e/) are Playwright's.
  test: {
    include: ['src/**/*.test.{ts,tsx}'],
    // The logs of the code, only for the tests that fail
    silent: 'passed-only',
  },
})
