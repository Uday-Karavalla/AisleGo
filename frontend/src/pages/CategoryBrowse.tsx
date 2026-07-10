import { useEffect, useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { useUserLocation } from '../context/LocationContext'
import { useCart } from '../context/CartContext'
import { productsApi } from '../api/products'
import type { CategoryProduct } from '../api/products'
import { EmptyState } from '../components/EmptyState'
import { MapPinIcon, StoreIcon } from '../components/icons'

type Status = 'loading' | 'success' | 'error'

/** Human-friendly title for a category keyword - falls back to title-casing the raw keyword
 *  for any category not in this list, so a new keyword never renders blank. */
const CATEGORY_TITLES: Record<string, string> = {
  vegetable: 'Fresh veggies & fruit',
  fruit: 'Fresh veggies & fruit',
  dairy: 'Dairy & more',
}

function titleFor(category: string): string {
  return CATEGORY_TITLES[category] ?? category.charAt(0).toUpperCase() + category.slice(1)
}

/** Cross-store category browse: one category's products mixed across every nearby supermarket
 *  (see `GET /api/stores/category-products`) - unlike Storefront, which is scoped to one
 *  already-chosen store. Each card is tagged with which store it's from and how far away, and
 *  "Add to cart" uses that product's nearest branch so checkout still has a real branch to
 *  fulfil from. */
export default function CategoryBrowse() {
  const { category = '' } = useParams<{ category: string }>()
  const { location } = useUserLocation()
  const { cart, addItem, updateQuantity, removeItem } = useCart()

  const [products, setProducts] = useState<CategoryProduct[]>([])
  const [status, setStatus] = useState<Status>('loading')

  useEffect(() => {
    if (!location) return
    setStatus('loading')
    productsApi
      .byCategory({ category, lat: location.lat, lng: location.lng })
      .then((response) => {
        setProducts(response.products)
        setStatus('success')
      })
      .catch(() => setStatus('error'))
  }, [category, location])

  if (!location) {
    return <Navigate to="/" replace />
  }

  function handleAdd(product: CategoryProduct) {
    addItem({
      product: {
        id: product.id,
        storeId: product.branchId,
        name: product.name,
        description: product.description,
        imageUrl: product.imageUrl,
        price: product.price,
        unit: '',
        category: product.categoryName,
        inStock: true,
      },
      storeId: product.branchId,
      storeName: product.supermarketName,
      quantity: 1,
    })
  }

  return (
    <div className="page-wide flex flex-col gap-4 px-5 py-6">
      <div>
        <h1 className="text-xl font-extrabold text-ink">{titleFor(category)}</h1>
        <p className="text-sm text-ink-muted">From every supermarket near {location.label}</p>
      </div>

      {status === 'loading' && (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5" aria-label="Loading products">
          {[0, 1, 2, 3].map((key) => (
            <div key={key} className="card h-40 animate-pulse bg-black/5" />
          ))}
        </div>
      )}

      {status === 'error' && (
        <EmptyState title="Couldn't load these products" description="Check your connection and try again." />
      )}

      {status === 'success' && products.length === 0 && (
        <EmptyState
          icon={<StoreIcon className="h-12 w-12" />}
          title="Nothing here yet"
          description="No nearby stores have products in this category right now."
        />
      )}

      {status === 'success' && products.length > 0 && (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
          {products.map((product) => {
            const cartItem = cart.items.find((item) => item.productId === product.id)
            const quantityInCart = cartItem?.quantity ?? 0
            return (
              <div key={product.id} className="card flex flex-col gap-2">
                {product.imageUrl ? (
                  <img
                    src={product.imageUrl}
                    alt={product.name}
                    className="h-28 w-full rounded-xl object-cover"
                    loading="lazy"
                  />
                ) : (
                  <div className="flex h-28 w-full items-center justify-center rounded-xl bg-brand-50 text-2xl font-black text-brand-600">
                    {product.name.charAt(0).toUpperCase()}
                  </div>
                )}

                <h3 className="truncate text-sm font-semibold text-ink">{product.name}</h3>
                <p className="flex items-center gap-1 truncate text-xs text-ink-faint">
                  <StoreIcon className="h-3 w-3 shrink-0" />
                  {product.supermarketName}
                  <span className="inline-flex shrink-0 items-center gap-0.5">
                    <MapPinIcon className="h-3 w-3" />
                    {product.distanceKm.toFixed(1)} km
                  </span>
                </p>

                <div className="mt-auto flex items-center justify-between gap-2">
                  <span className="text-base font-bold text-ink">₹{product.price.toFixed(0)}</span>
                  {quantityInCart > 0 ? (
                    <div className="flex items-center gap-2 text-sm font-semibold text-ink">
                      <button
                        type="button"
                        className="btn-secondary px-2.5 py-1"
                        onClick={() => {
                          if (!cartItem) return
                          if (cartItem.quantity <= 1) removeItem(cartItem.id)
                          else updateQuantity(cartItem.id, cartItem.quantity - 1)
                        }}
                      >
                        −
                      </button>
                      {quantityInCart}
                      <button
                        type="button"
                        className="btn-secondary px-2.5 py-1"
                        onClick={() => handleAdd(product)}
                      >
                        +
                      </button>
                    </div>
                  ) : (
                    <button type="button" className="btn-secondary px-3 py-1.5 text-sm" onClick={() => handleAdd(product)}>
                      Add
                    </button>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
