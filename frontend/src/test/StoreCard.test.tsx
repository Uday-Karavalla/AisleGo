import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StoreCard } from '../components/StoreCard'
import type { Store } from '../api/stores'

// Mirrors what the real backend sends today: no rating/ratingCount/categories/logoUrl
// at all (no backing schema yet) — only the fields storesApi.nearby() actually maps.
const baseStore: Store = {
  id: 'store-1',
  name: 'Test Mart',
  address: '1 Main St',
  distanceKm: 1.2,
  etaMinutes: 15,
  isOpen: true,
}

describe('StoreCard', () => {
  it('renders without crashing and shows no bogus placeholder content when optional fields are absent', () => {
    const { container } = render(<StoreCard store={baseStore} onOpen={vi.fn()} />)

    expect(screen.getByText('Test Mart')).toBeInTheDocument()
    expect(screen.getByText(/1\.2 km/)).toBeInTheDocument()
    expect(screen.getByText(/15 min/)).toBeInTheDocument()

    // No logo -> fallback icon, not a broken/empty <img>.
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    // No rating -> no star value or "(count)" rendered.
    expect(container.textContent).not.toMatch(/\(\d+\)/)
    // No categories -> no bullet-separated category row.
    expect(container.textContent).not.toContain('·')
  })

  it('renders rating, rating count, logo, and categories when the backend/fixture data provides them', () => {
    const enrichedStore: Store = {
      ...baseStore,
      rating: 4.5,
      ratingCount: 120,
      categories: ['Groceries', 'Bakery'],
      logoUrl: 'https://example.com/logo.png',
    }

    render(<StoreCard store={enrichedStore} onOpen={vi.fn()} />)

    expect(screen.getByText('4.5')).toBeInTheDocument()
    expect(screen.getByText('(120)')).toBeInTheDocument()
    expect(screen.getByText('Groceries · Bakery')).toBeInTheDocument()
    expect(screen.getByRole('img')).toHaveAttribute('src', 'https://example.com/logo.png')
  })

  it('marks a closed store as disabled and shows "Opens later" instead of an ETA', () => {
    const closedStore: Store = { ...baseStore, isOpen: false }
    render(<StoreCard store={closedStore} onOpen={vi.fn()} />)

    expect(screen.getByRole('button')).toBeDisabled()
    expect(screen.getByText('Closed')).toBeInTheDocument()
    expect(screen.getByText('Opens later')).toBeInTheDocument()
  })
})
