import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { favoritesApi } from '../api/growth'
import { useAuth } from './AuthContext'

interface FavoritesContextValue {
  productIds: Set<string>
  supermarketIds: Set<string>
  toggleProduct: (id: string) => Promise<void>
  toggleStore: (id: string) => Promise<void>
}

const FavoritesContext = createContext<FavoritesContextValue | undefined>(undefined)

export function FavoritesProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [productIds, setProductIds] = useState<Set<string>>(new Set())
  const [supermarketIds, setSupermarketIds] = useState<Set<string>>(new Set())

  useEffect(() => {
    if (!user?.roles.includes('CUSTOMER')) {
      setProductIds(new Set())
      setSupermarketIds(new Set())
      return
    }
    favoritesApi.list().then((result) => {
      setProductIds(new Set(result.productIds.map(String)))
      setSupermarketIds(new Set(result.supermarketIds.map(String)))
    }).catch(() => {})
  }, [user])

  const toggleProduct = useCallback(async (id: string) => {
    const removing = productIds.has(id)
    setProductIds((current) => {
      const next = new Set(current)
      if (removing) next.delete(id); else next.add(id)
      return next
    })
    try {
      if (removing) await favoritesApi.removeProduct(id); else await favoritesApi.addProduct(id)
    } catch (error) {
      setProductIds((current) => {
        const next = new Set(current)
        if (removing) next.add(id); else next.delete(id)
        return next
      })
      throw error
    }
  }, [productIds])

  const toggleStore = useCallback(async (id: string) => {
    const removing = supermarketIds.has(id)
    setSupermarketIds((current) => {
      const next = new Set(current)
      if (removing) next.delete(id); else next.add(id)
      return next
    })
    try {
      if (removing) await favoritesApi.removeStore(id); else await favoritesApi.addStore(id)
    } catch (error) {
      setSupermarketIds((current) => {
        const next = new Set(current)
        if (removing) next.add(id); else next.delete(id)
        return next
      })
      throw error
    }
  }, [supermarketIds])

  const value = useMemo(() => ({ productIds, supermarketIds, toggleProduct, toggleStore }),
    [productIds, supermarketIds, toggleProduct, toggleStore])
  return <FavoritesContext.Provider value={value}>{children}</FavoritesContext.Provider>
}

export function useFavorites() {
  const value = useContext(FavoritesContext)
  if (!value) throw new Error('useFavorites must be used within FavoritesProvider')
  return value
}
