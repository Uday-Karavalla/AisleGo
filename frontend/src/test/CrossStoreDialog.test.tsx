import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { CartProvider, useCart } from '../context/CartContext'
import { CrossStoreDialog } from '../components/CrossStoreDialog'
import type { Product } from '../api/products'

// The cart-conflict rule must hold even before the backend is reachable, so we stub
// the network layer entirely and assert on local cart state + dialog visibility.
vi.mock('../api/cart', () => ({
  cartApi: {
    addItem: vi.fn().mockResolvedValue({}),
    updateItem: vi.fn().mockResolvedValue({}),
    removeItem: vi.fn().mockResolvedValue({}),
    clear: vi.fn().mockResolvedValue({}),
    get: vi.fn().mockResolvedValue({}),
    applyCoupon: vi.fn().mockResolvedValue({}),
  },
  CROSS_STORE_CONFLICT_CODE: 'CROSS_STORE_CONFLICT',
}))

const productA: Product = {
  id: 'prod-a',
  storeId: 'store-a',
  name: 'Apples',
  price: 50,
  unit: '1 kg',
  category: 'Produce',
  inStock: true,
}

const productB: Product = {
  id: 'prod-b',
  storeId: 'store-b',
  name: 'Bananas',
  price: 30,
  unit: '1 kg',
  category: 'Produce',
  inStock: true,
}

function Harness() {
  const { cart, addItem } = useCart()
  return (
    <>
      <button onClick={() => addItem({ product: productA, storeId: 'store-a', storeName: 'Store A', quantity: 1 })}>
        Add Apples (Store A)
      </button>
      <button onClick={() => addItem({ product: productB, storeId: 'store-b', storeName: 'Store B', quantity: 1 })}>
        Add Bananas (Store B)
      </button>
      <div data-testid="cart-store">{cart.storeName ?? 'none'}</div>
      <div data-testid="cart-count">{cart.items.length}</div>
    </>
  )
}

function renderApp() {
  return render(
    <CartProvider>
      <Harness />
      <CrossStoreDialog />
    </CartProvider>,
  )
}

beforeEach(() => {
  localStorage.clear()
})

describe('cross-store cart conflict', () => {
  it('adds normally and keeps the dialog hidden when everything is from the same store', async () => {
    const user = userEvent.setup()
    renderApp()

    await user.click(screen.getByRole('button', { name: /add apples/i }))
    await user.click(screen.getByRole('button', { name: /add apples/i }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByTestId('cart-store')).toHaveTextContent('Store A')
    expect(screen.getByTestId('cart-count')).toHaveTextContent('1')
  })

  it('opens the conflict dialog (without touching the cart) when adding from a different store', async () => {
    const user = userEvent.setup()
    renderApp()

    await user.click(screen.getByRole('button', { name: /add apples/i }))
    await user.click(screen.getByRole('button', { name: /add bananas/i }))

    const dialog = screen.getByRole('dialog')
    expect(dialog).toBeInTheDocument()
    expect(dialog).toHaveTextContent('Store A')
    expect(dialog).toHaveTextContent('Store B')

    // Cart must not have been silently merged or mutated while the dialog is open.
    expect(screen.getByTestId('cart-store')).toHaveTextContent('Store A')
    expect(screen.getByTestId('cart-count')).toHaveTextContent('1')
  })

  it('clears the cart and switches stores when the user confirms', async () => {
    const user = userEvent.setup()
    renderApp()

    await user.click(screen.getByRole('button', { name: /add apples/i }))
    await user.click(screen.getByRole('button', { name: /add bananas/i }))
    await user.click(screen.getByRole('button', { name: /clear cart & switch store/i }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByTestId('cart-store')).toHaveTextContent('Store B')
    expect(screen.getByTestId('cart-count')).toHaveTextContent('1')
  })

  it('keeps the original cart untouched when the user cancels', async () => {
    const user = userEvent.setup()
    renderApp()

    await user.click(screen.getByRole('button', { name: /add apples/i }))
    await user.click(screen.getByRole('button', { name: /add bananas/i }))
    await user.click(screen.getByRole('button', { name: /cancel/i }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByTestId('cart-store')).toHaveTextContent('Store A')
    expect(screen.getByTestId('cart-count')).toHaveTextContent('1')
  })
})
