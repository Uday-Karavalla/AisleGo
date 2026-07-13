import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { productsApi } from '../api/products'
import type { Product } from '../api/products'
import { storesApi } from '../api/stores'
import type { Store } from '../api/stores'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { useFavorites } from '../context/FavoritesContext'
import { Seo } from '../components/Seo'
import { HeartIcon, ShieldCheckIcon } from '../components/icons'

export default function ProductDetail() {
  const { storeId, productId } = useParams<{ storeId: string; productId: string }>()
  const [store, setStore] = useState<Store | null>(null)
  const [product, setProduct] = useState<Product | null>(null)
  const { addItem } = useCart()
  const { user } = useAuth()
  const { productIds, toggleProduct } = useFavorites()

  useEffect(() => {
    if (!storeId || !productId) return
    storesApi.getById(storeId).then((result) => {
      setStore(result)
      return productsApi.get(result.supermarketId ?? storeId, productId)
    }).then(setProduct).catch(() => {})
  }, [storeId, productId])

  if (!storeId || !product || !store) return <div className="page-narrow px-5 py-10 text-sm text-ink-muted">Loading product…</div>
  const description = product.description ?? `Order ${product.name} online from ${store.name} with AisleGo.`
  return (
    <div className="page-wide grid gap-6 px-5 py-6 md:grid-cols-2">
      <Seo title={`${product.name} from ${store.name} | AisleGo`} description={description} canonicalPath={`/stores/${storeId}/products/${product.id}`} imageUrl={product.imageUrl} structuredData={{ '@context': 'https://schema.org', '@type': 'Product', name: product.name, description, image: product.imageUrl, offers: { '@type': 'Offer', priceCurrency: 'INR', price: product.price, availability: product.inStock ? 'https://schema.org/InStock' : 'https://schema.org/OutOfStock', url: window.location.href }, brand: { '@type': 'Brand', name: store.name } }} />
      <div className="overflow-hidden rounded-3xl bg-brand-50">{product.imageUrl ? <img src={product.imageUrl} alt={product.name} className="h-full min-h-80 w-full object-cover" /> : <div className="flex min-h-80 items-center justify-center text-7xl font-black text-brand-600">{product.name[0]}</div>}</div>
      <div className="flex flex-col justify-center gap-4"><Link to={`/stores/${storeId}`} className="text-sm font-semibold text-brand-700">← Back to {store.name}</Link><div><p className="text-xs font-bold uppercase tracking-wider text-brand-700">{product.category}</p><h1 className="mt-1 text-3xl font-black text-ink">{product.name}</h1><p className="mt-2 text-2xl font-black text-ink">₹{product.price.toFixed(2)}</p></div><p className="text-ink-muted">{description}</p><p className="flex items-center gap-2 text-sm font-semibold text-brand-800"><ShieldCheckIcon className="h-5 w-5" /> Sold by a verified AisleGo supermarket</p><div className="flex gap-2"><button type="button" className="btn-primary flex-1" onClick={() => addItem({ product, storeId, storeName: store.name, quantity: 1 })}>Add to cart</button>{user?.roles.includes('CUSTOMER') && <button type="button" className="btn-secondary px-4" onClick={() => void toggleProduct(product.id)} aria-label="Toggle favourite"><HeartIcon className="h-5 w-5 text-danger-500" filled={productIds.has(product.id)} /></button>}</div></div>
    </div>
  )
}
