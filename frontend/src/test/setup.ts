import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// RTL's automatic afterEach cleanup only self-registers when it detects global
// test hooks (vitest `globals: true`). We import `describe/it/expect` explicitly
// instead, so wire cleanup up manually to unmount between tests.
afterEach(() => {
  cleanup()
})
