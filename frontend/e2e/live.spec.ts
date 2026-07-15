import { expect, test } from '@playwright/test'

const backendHealthUrl = process.env.E2E_BACKEND_HEALTH_URL
  ?? 'https://aislego-backend.onrender.com/actuator/health'

test.describe('production availability', () => {
  test.describe.configure({ timeout: 120_000 })

  test('serves the public shopping experience', async ({ page }) => {
    const response = await page.goto('/', { waitUntil: 'domcontentloaded' })

    expect(response?.ok()).toBe(true)
    await expect(page).toHaveTitle(/AisleGo/i)
    await expect(page.getByRole('heading', { name: /Groceries you trust/i })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Find stores near me' })).toBeEnabled()
  })

  test('serves client-side routes after a direct navigation', async ({ page }) => {
    const response = await page.goto('/login', { waitUntil: 'domcontentloaded' })

    expect(response?.ok()).toBe(true)
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Create an account' })).toBeVisible()
  })

  test('reports the backend as live and ready', async ({ request }) => {
    const response = await request.get(backendHealthUrl, { timeout: 90_000 })

    expect(response.ok()).toBe(true)
    await expect(response.json()).resolves.toMatchObject({
      status: 'UP',
      groups: expect.arrayContaining(['liveness', 'readiness']),
    })
  })
})
