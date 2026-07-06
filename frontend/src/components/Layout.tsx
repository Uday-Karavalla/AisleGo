import { NavLink, Outlet, Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useUserLocation } from '../context/LocationContext'
import { useLocalStorage } from '../hooks/useLocalStorage'
import { HomeIcon, CartIcon, ClipboardIcon, MapPinIcon } from './icons'

function navLinkClass({ isActive }: { isActive: boolean }) {
  return `flex flex-1 flex-col items-center justify-center gap-0.5 py-2 text-xs font-medium ${
    isActive ? 'text-brand-700' : 'text-ink-faint'
  }`
}

/** Persistent mobile-first shell: brand header + bottom tab bar, matching the natural flow order. */
export function Layout() {
  const { cart } = useCart()
  const { location } = useUserLocation()
  const [lastOrderId] = useLocalStorage<string | null>('aislego.lastOrderId', null)
  const itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0)

  return (
    <div className="mx-auto flex min-h-full max-w-app flex-col bg-surface-muted">
      <header className="sticky top-0 z-30 flex items-center justify-between gap-3 border-b border-black/5 bg-white/95 px-4 py-3 backdrop-blur">
        <Link to="/" className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-sm font-black text-white">
            A
          </span>
          <span className="text-base font-extrabold tracking-tight text-ink">AisleGo</span>
        </Link>
        {location && (
          <Link to="/" className="flex max-w-[50%] items-center gap-1 truncate text-xs text-ink-muted">
            <MapPinIcon className="h-3.5 w-3.5 shrink-0" />
            <span className="truncate">{location.label}</span>
          </Link>
        )}
      </header>

      <main className="flex-1 pb-24">
        <Outlet />
      </main>

      <nav
        className="fixed inset-x-0 bottom-0 z-30 mx-auto flex max-w-app border-t border-black/5 bg-white"
        style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
        aria-label="Primary"
      >
        <NavLink to="/" end className={navLinkClass}>
          <HomeIcon className="h-6 w-6" />
          Home
        </NavLink>
        <NavLink to="/cart" className={navLinkClass}>
          <div className="relative">
            <CartIcon className="h-6 w-6" />
            {itemCount > 0 && (
              <span className="absolute -right-2 -top-1.5 flex h-4 min-w-[1rem] items-center justify-center rounded-full bg-brand-600 px-1 text-[10px] font-bold text-white">
                {itemCount}
              </span>
            )}
          </div>
          Cart
        </NavLink>
        <NavLink to={lastOrderId ? `/orders/${lastOrderId}` : '/'} className={navLinkClass}>
          <ClipboardIcon className="h-6 w-6" />
          Orders
        </NavLink>
      </nav>
    </div>
  )
}
