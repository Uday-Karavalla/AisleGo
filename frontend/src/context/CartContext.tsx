import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { useLocalStorage } from '../hooks/useLocalStorage'
import { cartApi, CROSS_STORE_CONFLICT_CODE } from '../api/cart'
import type { Cart, CartItem } from '../api/cart'
import type { CartCouponState } from '../api/cart'
import { ApiError, getAuthToken } from '../api/client'
import type { Product } from '../api/products'
import type { Order } from '../api/orders'
import { useOptionalAuth } from './AuthContext'
import { useRef } from 'react'
import { trackEvent } from '../api/growth'

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
  applyCoupon: (code: string) => Promise<void>
  replaceWithOrder: (order: Order, serverCart: CartCouponState) => void
  pendingConflict: PendingConflict | null
  confirmSwitchStore: () => void
  cancelSwitchStore: () => void
}

const CartContext = createContext<CartContextValue | undefined>(undefined)

export function CartProvider({ children }: { children: ReactNode }) {
  // Optimistic/local-first lets signed-out shoppers build a cart. Authenticated responses
  // replace temporary UUID line ids with server ids, and the login handoff below copies a
  // guest cart into the real backend before checkout.
  const [cart, setCart] = useLocalStorage<Cart>('aislego.cart', EMPTY_CART)
  const [pendingConflict, setPendingConflict] = useState<PendingConflict | null>(null)
  const user = useOptionalAuth()?.user ?? null
  const syncedGuestCartForUser = useRef<number | null>(null)

  const syncServerPricing = useCallback(
    (serverCart: CartCouponState) => {
      setCart((current) => {
        const serverItems = serverCart.items?.map((item) => {
          const local = current.items.find((candidate) => candidate.productId === String(item.productId))
          return {
            id: String(item.id),
            productId: String(item.productId),
            storeId: local?.storeId ?? current.storeId ?? '',
            storeName: local?.storeName ?? current.storeName ?? 'Selected store',
            name: item.productName,
            price: item.unitPrice,
            unit: local?.unit ?? '',
            imageUrl: local?.imageUrl,
            quantity: item.quantity,
            allowSubstitution: local?.allowSubstitution ?? true,
          }
        })
        return ({
          ...current,
          id: serverCart.id ? String(serverCart.id) : current.id,
          items: serverItems ?? current.items,
          subtotal: serverCart.subtotal,
          deliveryFee: serverCart.deliveryFee,
          couponCode: serverCart.couponCode,
          discount: serverCart.discount,
          total: serverCart.total,
        })
      })
    },
    [setCart],
  )

  // A coupon can expire or be disabled while the cart is idle. Fetching the cart asks the
  // backend to re-resolve it and makes that freshly calculated discount authoritative.
  useEffect(() => {
    if (!getAuthToken()) return
    let cancelled = false
    cartApi
      .get()
      .then((serverCart) => {
        if (!cancelled) syncServerPricing(serverCart)
      })
      .catch(() => {
        // Keep the local cart usable offline; an explicit Apply action still surfaces errors.
      })
    return () => {
      cancelled = true
    }
  }, [syncServerPricing])

  // A signed-out shopper can build a real local cart. On their first login in this browser,
  // copy UUID-backed guest lines to the authenticated server cart so checkout does not lose it.
  useEffect(() => {
    if (!user?.roles.includes('CUSTOMER') || syncedGuestCartForUser.current === user.id) return
    const guestItems = cart.items.filter((item) => item.id.includes('-'))
    if (guestItems.length === 0) {
      syncedGuestCartForUser.current = user.id
      return
    }
    syncedGuestCartForUser.current = user.id
    ;(async () => {
      try {
        await cartApi.clear()
        let response: CartCouponState | null = null
        for (const item of guestItems) {
          response = await cartApi.addItem({ productId: item.productId, storeId: item.storeId, quantity: item.quantity })
        }
        if (response) syncServerPricing(response)
      } catch {
        syncedGuestCartForUser.current = null
      }
    })()
  }, [user, cart.items, syncServerPricing])

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
      trackEvent('add_to_cart', { productId: input.product.id, storeId: input.storeId, quantity: input.quantity, value: input.product.price })

      cartApi
        .addItem({ productId: input.product.id, storeId: input.storeId, quantity: input.quantity })
        .then(syncServerPricing)
        .catch((error: unknown) => {
          if (error instanceof ApiError && error.code === CROSS_STORE_CONFLICT_CODE) {
            // The backend caught a conflict our local check missed (e.g. stale state across tabs).
            setCart(baseCart)
            setPendingConflict({ input, currentStoreName: baseCart.storeName ?? 'your current store' })
          }
          // Network errors are swallowed here — no backend yet is an expected state.
        })
    },
    [setCart, syncServerPricing],
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
      cartApi.updateItem(itemId, { quantity }).then(syncServerPricing).catch(() => {})
    },
    [setCart, syncServerPricing],
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
      cartApi.removeItem(itemId).then(syncServerPricing).catch(() => {})
    },
    [setCart, syncServerPricing],
  )

  const clearCart = useCallback(() => {
    setCart(EMPTY_CART)
    cartApi.clear().then(syncServerPricing).catch(() => {})
  }, [setCart, syncServerPricing])

  const applyCoupon = useCallback(
    async (code: string) => {
      const trimmed = code.trim()
      const serverCart = trimmed ? await cartApi.applyCoupon(trimmed) : await cartApi.removeCoupon()
      syncServerPricing(serverCart)
      if (trimmed) trackEvent('coupon_apply', { code: trimmed, discount: serverCart.discount })
    },
    [syncServerPricing],
  )

  const replaceWithOrder = useCallback((order: Order, serverCart: CartCouponState) => {
    setCart({
      id: serverCart.id ? String(serverCart.id) : 'reorder-cart',
      storeId: String(order.branchId),
      storeName: `Store from order #${order.id}`,
      items: (serverCart.items ?? []).map((item) => ({
        id: String(item.id),
        productId: String(item.productId),
        storeId: String(order.branchId),
        storeName: `Store from order #${order.id}`,
        name: item.productName,
        price: item.unitPrice,
        unit: '',
        quantity: item.quantity,
        allowSubstitution: true,
      })),
      subtotal: serverCart.subtotal,
      deliveryFee: serverCart.deliveryFee,
      discount: serverCart.discount,
      total: serverCart.total,
      couponCode: serverCart.couponCode,
    })
  }, [setCart])

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
      replaceWithOrder,
      pendingConflict,
      confirmSwitchStore,
      cancelSwitchStore,
    }),
    [cart, addItem, updateQuantity, setSubstitution, removeItem, clearCart, applyCoupon, replaceWithOrder, pendingConflict, confirmSwitchStore, cancelSwitchStore],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart(): CartContextValue {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within a CartProvider')
  return ctx
}
