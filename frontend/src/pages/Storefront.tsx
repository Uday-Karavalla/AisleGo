import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { storesApi } from '../api/stores'
import type { Store } from '../api/stores'
import { productsApi } from '../api/products'
import type { Product } from '../api/products'
import { reviewsApi } from '../api/reviews'
import type { MyReviewStatus, StoreReviews } from '../api/reviews'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { ProductCard } from '../components/ProductCard'
import { EmptyState } from '../components/EmptyState'
import { SearchIcon, StarIcon, ClockIcon } from '../components/icons'

const PAGE_SIZE = 12

type Status = 'loading' | 'success' | 'error'

export default function Storefront() {
  const { storeId } = useParams<{ storeId: string }>()
  const { cart, addItem, updateQuantity, removeItem } = useCart()
  const { user } = useAuth()

  const [store, setStore] = useState<Store | null>(null)
  const [storeStatus, setStoreStatus] = useState<Status>('loading')

  const [storeReviews, setStoreReviews] = useState<StoreReviews | null>(null)
  const [myReviewStatus, setMyReviewStatus] = useState<MyReviewStatus | null>(null)
  const [showReviewForm, setShowReviewForm] = useState(false)
  const [reviewDraft, setReviewDraft] = useState({ rating: 5, comment: '' })
  const [reviewSubmitting, setReviewSubmitting] = useState(false)
  const [reviewMessage, setReviewMessage] = useState<string | null>(null)

  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [category, setCategory] = useState<string | null>(null)
  const [categories, setCategories] = useState<string[]>([])

  const [products, setProducts] = useState<Product[]>([])
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [productsStatus, setProductsStatus] = useState<Status>('loading')

  useEffect(() => {
    if (!storeId) return
    setStoreStatus('loading')
    storesApi
      .getById(storeId)
      .then((result) => {
        setStore(result)
        setStoreStatus('success')
      })
      .catch(() => setStoreStatus('error'))
    productsApi
      .categories(storeId)
      .then(setCategories)
      .catch(() => {})
    reviewsApi
      .list(storeId)
      .then(setStoreReviews)
      .catch(() => {})
  }, [storeId])

  useEffect(() => {
    if (!storeId || !user?.roles.includes('CUSTOMER')) {
      setMyReviewStatus(null)
      return
    }
    reviewsApi
      .mine(storeId)
      .then(setMyReviewStatus)
      .catch(() => {})
  }, [storeId, user])

  function openReviewForm() {
    setReviewMessage(null)
    setReviewDraft({
      rating: myReviewStatus?.myReview?.rating ?? 5,
      comment: myReviewStatus?.myReview?.comment ?? '',
    })
    setShowReviewForm(true)
  }

  async function handleSubmitReview(event: FormEvent) {
    event.preventDefault()
    if (!storeId) return
    setReviewSubmitting(true)
    setReviewMessage(null)
    try {
      const review = await reviewsApi.submit(storeId, {
        rating: reviewDraft.rating,
        comment: reviewDraft.comment || undefined,
      })
      setMyReviewStatus((prev) => (prev ? { ...prev, myReview: review } : prev))
      setStoreReviews((prev) => {
        if (!prev) return prev
        const withoutMine = prev.reviews.filter((r) => r.id !== review.id)
        return {
          reviews: [review, ...withoutMine],
          reviewCount: withoutMine.length === prev.reviews.length ? prev.reviewCount + 1 : prev.reviewCount,
          averageRating: prev.averageRating,
        }
      })
      setShowReviewForm(false)
      if (storeId) reviewsApi.list(storeId).then(setStoreReviews).catch(() => {})
    } catch (error) {
      setReviewMessage(error instanceof Error ? error.message : 'Could not submit your review.')
    } finally {
      setReviewSubmitting(false)
    }
  }

  useEffect(() => {
    const timeout = setTimeout(() => setDebouncedSearch(search.trim()), 300)
    return () => clearTimeout(timeout)
  }, [search])

  useEffect(() => {
    setPage(1)
  }, [debouncedSearch, category])

  useEffect(() => {
    if (!storeId) return
    let cancelled = false
    setProductsStatus('loading')
    productsApi
      .list({
        storeId,
        search: debouncedSearch || undefined,
        category: category ?? undefined,
        page,
        pageSize: PAGE_SIZE,
      })
      .then((response) => {
        if (cancelled) return
        setProducts((prev) => (page === 1 ? response.products : [...prev, ...response.products]))
        setTotalPages(response.totalPages)
        setProductsStatus('success')
      })
      .catch(() => {
        if (!cancelled) setProductsStatus('error')
      })
    return () => {
      cancelled = true
    }
  }, [storeId, debouncedSearch, category, page])

  function handleAddOrIncrement(product: Product) {
    if (!store) return
    addItem({ product, storeId: store.id, storeName: store.name, quantity: 1 })
  }

  if (storeStatus === 'error') {
    return <EmptyState title="Couldn't load this store" description="Please go back and try another supermarket." />
  }

  return (
    <div className="flex flex-col gap-4 px-5 py-6">
      {store && (
        <div>
          <h1 className="text-xl font-extrabold text-ink">{store.name}</h1>
          <div className="mt-1 flex items-center gap-3 text-sm text-ink-muted">
            {typeof store.rating === 'number' && (
              <span className="inline-flex items-center gap-1">
                <StarIcon className="h-3.5 w-3.5 text-warning-500" />
                {store.rating.toFixed(1)}
              </span>
            )}
            <span className="inline-flex items-center gap-1">
              <ClockIcon className="h-3.5 w-3.5" />
              {store.etaMinutes} min
            </span>
          </div>
        </div>
      )}

      <div className="relative">
        <SearchIcon className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-faint" />
        <input
          className="input-field pl-10"
          placeholder="Search this store's products"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          aria-label="Search products"
        />
      </div>

      {categories.length > 0 && (
        <div className="-mx-5 flex gap-2 overflow-x-auto px-5 pb-1">
          <button
            type="button"
            onClick={() => setCategory(null)}
            className={`shrink-0 rounded-full border-2 px-4 py-1.5 text-xs font-semibold ${
              category === null ? 'border-brand-600 bg-brand-50 text-brand-700' : 'border-black/10 text-ink-muted'
            }`}
          >
            All
          </button>
          {categories.map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => setCategory(item)}
              className={`shrink-0 rounded-full border-2 px-4 py-1.5 text-xs font-semibold ${
                category === item ? 'border-brand-600 bg-brand-50 text-brand-700' : 'border-black/10 text-ink-muted'
              }`}
            >
              {item}
            </button>
          ))}
        </div>
      )}

      {productsStatus === 'error' && (
        <EmptyState title="Couldn't load products" description="Please try again in a moment." />
      )}

      {productsStatus === 'success' && products.length === 0 && (
        <EmptyState title="No products found" description="Try a different search or category." />
      )}

      <div className="grid grid-cols-2 gap-3">
        {products.map((product) => {
          const cartItem = cart.items.find((item) => item.productId === product.id)
          const quantityInCart = cartItem?.quantity ?? 0
          return (
            <ProductCard
              key={product.id}
              product={product}
              quantityInCart={quantityInCart}
              onAdd={() => handleAddOrIncrement(product)}
              onIncrement={() => handleAddOrIncrement(product)}
              onDecrement={() => {
                if (!cartItem) return
                if (cartItem.quantity <= 1) removeItem(cartItem.id)
                else updateQuantity(cartItem.id, cartItem.quantity - 1)
              }}
            />
          )
        })}
      </div>

      {productsStatus === 'loading' && page === 1 && (
        <div className="grid grid-cols-2 gap-3" aria-label="Loading products">
          {[0, 1, 2, 3].map((key) => (
            <div key={key} className="card h-36 animate-pulse bg-black/5" />
          ))}
        </div>
      )}

      {productsStatus === 'success' && page < totalPages && (
        <button type="button" className="btn-secondary" onClick={() => setPage((prev) => prev + 1)}>
          Load more products
        </button>
      )}

      <section className="mt-4 flex flex-col gap-3 border-t border-black/5 pt-6">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-ink">
            Reviews{storeReviews && storeReviews.reviewCount > 0 ? ` (${storeReviews.reviewCount})` : ''}
          </h2>
          {myReviewStatus?.eligible && !showReviewForm && (
            <button type="button" onClick={openReviewForm} className="text-xs font-semibold text-brand-700">
              {myReviewStatus.myReview ? 'Edit your review' : 'Write a review'}
            </button>
          )}
        </div>

        {user?.roles.includes('CUSTOMER') && myReviewStatus && !myReviewStatus.eligible && (
          <p className="text-xs text-ink-faint">You can review this store once an order from it has been delivered.</p>
        )}

        {showReviewForm && (
          <form onSubmit={handleSubmitReview} className="card flex flex-col gap-2">
            <div className="flex gap-1">
              {[1, 2, 3, 4, 5].map((value) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setReviewDraft((d) => ({ ...d, rating: value }))}
                  aria-label={`${value} star${value === 1 ? '' : 's'}`}
                  className="p-0.5"
                >
                  <StarIcon
                    className={`h-6 w-6 ${value <= reviewDraft.rating ? 'text-warning-500' : 'text-black/15'}`}
                  />
                </button>
              ))}
            </div>
            <textarea
              className="input-field"
              rows={3}
              placeholder="Share your experience with this store (optional)"
              value={reviewDraft.comment}
              onChange={(e) => setReviewDraft((d) => ({ ...d, comment: e.target.value }))}
            />
            {reviewMessage && (
              <p role="alert" className="text-sm text-danger-500">
                {reviewMessage}
              </p>
            )}
            <div className="flex gap-2">
              <button type="submit" className="btn-primary flex-1" disabled={reviewSubmitting}>
                {reviewSubmitting ? 'Saving…' : 'Submit review'}
              </button>
              <button type="button" className="btn-secondary flex-1" onClick={() => setShowReviewForm(false)}>
                Cancel
              </button>
            </div>
          </form>
        )}

        {storeReviews && storeReviews.reviews.length === 0 && (
          <p className="text-sm text-ink-muted">No reviews yet — be the first to shop here and leave one.</p>
        )}

        {storeReviews?.reviews.map((review) => (
          <div key={review.id} className="card flex flex-col gap-1">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold text-ink">{review.reviewerName}</span>
              <span className="inline-flex items-center gap-1 text-xs text-ink-muted">
                <StarIcon className="h-3.5 w-3.5 text-warning-500" />
                {review.rating}
              </span>
            </div>
            {review.comment && <p className="text-sm text-ink-muted">{review.comment}</p>}
          </div>
        ))}
      </section>
    </div>
  )
}
