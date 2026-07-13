import { beforeEach, describe, expect, it, vi } from 'vitest'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { PwaInstallPrompt } from '../components/PwaInstallPrompt'

describe('PwaInstallPrompt', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('uses the browser install prompt and hides after the choice', async () => {
    const prompt = vi.fn().mockResolvedValue(undefined)
    const installEvent = Object.assign(new Event('beforeinstallprompt'), {
      prompt,
      userChoice: Promise.resolve({ outcome: 'accepted' as const, platform: 'web' }),
    })
    const user = userEvent.setup()

    render(
      <MemoryRouter initialEntries={['/']}>
        <PwaInstallPrompt />
      </MemoryRouter>,
    )
    act(() => window.dispatchEvent(installEvent))

    expect(await screen.findByRole('dialog', { name: 'Install AisleGo' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /install app/i }))

    expect(prompt).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('dialog', { name: 'Install AisleGo' })).not.toBeInTheDocument()
    expect(Number(localStorage.getItem('aislego.installPromptDismissedAt'))).toBeGreaterThan(0)
  })
})
