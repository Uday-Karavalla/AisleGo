import type { Product } from '../api/products'
import { QuantityStepper } from './QuantityStepper'
import { HeartIcon } from './icons'

interface ProductCardProps {
  product: Product
  quantityInCart: number
  onAdd: () => void
  onIncrement: () => void
  onDecrement: () => void
  favorite?: boolean
  onToggleFavorite?: () => void
  detailUrl?: string
}

export function ProductCard({ product, quantityInCart, onAdd, onIncrement, onDecrement, favorite, onToggleFavorite, detailUrl }: ProductCardProps) {
  const hasDiscount = product.mrp !== undefined && product.mrp > product.price

  return (
    <div className="card relative flex flex-col gap-3">
      {onToggleFavorite && (
        <button type="button" className="absolute right-3 top-3 z-10 rounded-full bg-white/90 p-2 text-danger-500 shadow-card" onClick={onToggleFavorite} aria-label={favorite ? `Remove ${product.name} from favourites` : `Save ${product.name} to favourites`}>
          <HeartIcon className="h-4 w-4" filled={favorite} />
        </button>
      )}
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

      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <h3 className="truncate text-sm font-semibold text-ink">{detailUrl ? <a href={detailUrl} className="hover:text-brand-700">{product.name}</a> : product.name}</h3>
          <p className="text-xs text-ink-faint">{product.unit}</p>
        </div>
        {!product.inStock && (
          <span className="shrink-0 rounded-full bg-danger-50 px-2 py-0.5 text-[11px] font-semibold text-danger-500">
            Out of stock
          </span>
        )}
      </div>

      <div className="flex items-center gap-2">
        <span className="text-base font-bold text-ink">₹{product.price.toFixed(0)}</span>
        {hasDiscount && (
          <span className="text-xs text-ink-faint line-through">₹{product.mrp?.toFixed(0)}</span>
        )}
      </div>

      <div className="mt-auto">
        {!product.inStock ? (
          <button type="button" disabled className="btn-secondary w-full py-2 text-sm opacity-50">
            Unavailable
          </button>
        ) : quantityInCart > 0 ? (
          <QuantityStepper
            size="sm"
            quantity={quantityInCart}
            onIncrement={onIncrement}
            onDecrement={onDecrement}
            ariaLabel={`Quantity of ${product.name}`}
          />
        ) : (
          <button
            type="button"
            onClick={onAdd}
            className="btn-secondary w-full py-2 text-sm"
            aria-label={`Add ${product.name} to cart`}
          >
            Add
          </button>
        )}
      </div>
    </div>
  )
}
