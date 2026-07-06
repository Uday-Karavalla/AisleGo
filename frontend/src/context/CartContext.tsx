import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { useLocalStorage } from '../hooks/useLocalStorage'
import { cartApi, CROSS_STORE_CONFLICT_CODE } from '../api/cart'
import type { Cart, CartItem } from '../api/cart'
import { ApiError } from '../api/client'
import type { Product } from '../api/products'

const DELIVERY_FEE = 25

const EMPTY_CART: Cart = {
  id: 'local-cart',
  storeId: null,
  storeName: null,
  items: [],
  subtotal: 0,
  deliveryFee: 0,
  discount: 0,
  total: 0,
  couponCode: null,
}

function recalculate(cart: Cart): Cart {
  const subtotal = cart.items.reduce((sum, item) => sum + item.price * item.quantity, 0)
  const deliveryFee = cart.items.length > 0 ? DELIVERY_FEE : 0
  const discount = cart.items.length > 0 ? cart.discount : 0
  const total = Math.max(0, subtotal + deliveryFee - discount)
  return { ...cart, subtotal, deliveryFee, discount, total }
}

export interface AddToCartInput {
  product: Product
  storeId: string
  storeName: string
  quantity: number
}

export interface PendingConflict {
  input: AddToCartInput
  currentStoreName: string
}

interface CartContextValue {
  cart: Cart
  isEmpty: boolean
  addItem: (input: AddToCartInput) => void
  updateQuantity: (itemId: string, quantity: number) => void
  setSubstitution: (itemId: string, allow: boolean) => void
  removeItem: (itemId: string) => void
  clearCart: () => void
  applyCoupon: (code: string) => void
  pendingConflict: PendingConflict | null
  confirmSwitchStore: () => void
  cancelSwitchStore: () => void
}

const CartContext = createContext<CartContextValue | undefined>(undefined)

export function CartProvider({ children }: { children: ReactNode }) {
  // NOTE: cart state is optimistic/local-first. Calls to cartApi are best-effort
  // (fire-and-forget) so the UI keeps working before the backend exists; once the
  // real endpoints are live, wire the returned Cart (with server-assigned item ids)
  // back into this state instead of trusting the local copy.
  const [cart, setCart] = useLocalStorage<Cart>('aislego.cart', EMPTY_CART)
  const [pendingConflict, setPendingConflict] = useState<PendingConflict | null>(null)

  const performAdd = useCallback(
    (input: AddToCartInput, baseCart: Cart) => {
      const existingItem = baseCart.items.find((item) => item.productId === input.product.id)
      const nextItems: CartItem[] = existingItem
        ? baseCart.items.map((item) =>
            item.id === existingItem.id ? { ...item, quantity: item.quantity + input.quantity } : item,
          )
        : [
            ...baseCart.items,
            {
              id: crypto.randomUUID(),
              productId: input.product.id,
              storeId: input.storeId,
              storeName: input.storeName,
              name: input.product.name,
              price: input.product.price,
              unit: input.product.unit,
              imageUrl: input.product.imageUrl,
              quantity: input.quantity,
              allowSubstitution: true,
            },
          ]

      setCart(
        recalculate({
          ...baseCart,
          storeId: input.storeId,
          storeName: input.storeName,
          items: nextItems,
        }),
      )

      cartApi
        .addItem({ productId: input.product.id, storeId: input.storeId, quantity: input.quantity })
        .catch((error: unknown) => {
          if (error instanceof ApiError && error.code === CROSS_STORE_CONFLICT_CODE) {
            // The backend caught a conflict our local check missed (e.g. stale state across tabs).
            setCart(baseCart)
            setPendingConflict({ input, currentStoreName: baseCart.storeName ?? 'your current store' })
          }
          // Network errors are swallowed here — no backend yet is an expected state.
        })
    },
    [setCart],
  )

  const addItem = useCallback(
    (input: AddToCartInput) => {
      const hasDifferentStoreItems = cart.storeId !== null && cart.storeId !== input.storeId
      if (hasDifferentStoreItems) {
        setPendingConflict({ input, currentStoreName: cart.storeName ?? 'your current store' })
        return
      }
      performAdd(input, cart)
    },
    [cart, performAdd],
  )

  const confirmSwitchStore = useCallback(() => {
    if (!pendingConflict) return
    const { input } = pendingConflict
    setPendingConflict(null)
    cartApi.clear().catch(() => {})
    performAdd(input, EMPTY_CART)
  }, [pendingConflict, performAdd])

  const cancelSwitchStore = useCallback(() => {
    setPendingConflict(null)
  }, [])

  const updateQuantity = useCallback(
    (itemId: string, quantity: number) => {
      setCart((current) => {
        if (quantity <= 0) {
          const nextItems = current.items.filter((item) => item.id !== itemId)
          const storeStillPresent = nextItems.length > 0
          return recalculate({
            ...current,
            items: nextItems,
            storeId: storeStillPresent ? current.storeId : null,
            storeName: storeStillPresent ? current.storeName : null,
          })
        }
        return recalculate({
          ...current,
          items: current.items.map((item) => (item.id === itemId ? { ...item, quantity } : item)),
        })
      })
      cartApi.updateItem(itemId, { quantity }).catch(() => {})
    },
    [setCart],
  )

  const setSubstitution = useCallback(
    (itemId: string, allow: boolean) => {
      setCart((current) =>
        recalculate({
          ...current,
          items: current.items.map((item) => (item.id === itemId ? { ...item, allowSubstitution: allow } : item)),
        }),
      )
      cartApi.updateItem(itemId, { allowSubstitution: allow }).catch(() => {})
    },
    [setCart],
  )

  const removeItem = useCallback(
    (itemId: string) => {
      setCart((current) => {
        const nextItems = current.items.filter((item) => item.id !== itemId)
        const storeStillPresent = nextItems.length > 0
        return recalculate({
          ...current,
          items: nextItems,
          storeId: storeStillPresent ? current.storeId : null,
          storeName: storeStillPresent ? current.storeName : null,
        })
      })
      cartApi.removeItem(itemId).catch(() => {})
    },
    [setCart],
  )

  const clearCart = useCallback(() => {
    setCart(EMPTY_CART)
    cartApi.clear().catch(() => {})
  }, [setCart])

  const applyCoupon = useCallback(
    (code: string) => {
      setCart((current) => {
        const trimmed = code.trim().toUpperCase()
        if (!trimmed) {
          return recalculate({ ...current, couponCode: null, discount: 0 })
        }
        // Stub only: any non-empty code applies a flat demo discount. Real validation
        // (and rejection of invalid/expired codes) happens once /cart/coupon exists.
        const stubDiscount = Math.min(current.subtotal, 30)
        return recalculate({ ...current, couponCode: trimmed, discount: stubDiscount })
      })
    },
    [setCart],
  )

  const value = useMemo<CartContextValue>(
    () => ({
      cart,
      isEmpty: cart.items.length === 0,
      addItem,
      updateQuantity,
      setSubstitution,
      removeItem,
      clearCart,
      applyCoupon,
      pendingConflict,
      confirmSwitchStore,
      cancelSwitchStore,
    }),
    [cart, addItem, updateQuantity, setSubstitution, removeItem, clearCart, applyCoupon, pendingConflict, confirmSwitchStore, cancelSwitchStore],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart(): CartContextValue {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within a CartProvider')
  return ctx
}
