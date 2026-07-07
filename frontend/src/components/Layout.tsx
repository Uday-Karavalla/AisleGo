import { NavLink, Outlet, Link, useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useUserLocation } from '../context/LocationContext'
import { useAuth } from '../context/AuthContext'
import { HomeIcon, CartIcon, ClipboardIcon, MapPinIcon, UserIcon } from './icons'

function navLinkClass({ isActive }: { isActive: boolean }) {
  return `flex flex-1 flex-col items-center justify-center gap-0.5 py-2 text-xs font-medium ${
    isActive ? 'text-brand-700' : 'text-ink-faint'
  }`
}

/** Header's account control — a plain "Log in" link when signed out, a role-aware menu when signed in. */
function AccountMenu() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  if (!user) {
    return (
      <div className="flex shrink-0 items-center gap-2 text-xs font-semibold">
        <Link to="/login" className="text-brand-700">
          Log in
        </Link>
        <span className="text-ink-faint">·</span>
        <Link to="/register" className="text-brand-700">
          Sign up
        </Link>
      </div>
    )
  }

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <details className="relative shrink-0">
      <summary
        className="flex h-8 w-8 cursor-pointer list-none items-center justify-center rounded-full bg-black/5 text-ink-muted"
        aria-label="Account menu"
      >
        <UserIcon className="h-4 w-4" />
      </summary>
      <div className="absolute right-0 top-10 z-40 w-52 rounded-xl border border-black/5 bg-white p-2 shadow-card">
        <p className="truncate px-2 py-1 text-xs text-ink-muted">{user.email}</p>
        {user.roles.includes('ADMIN') && (
          <Link to="/admin" className="block rounded-lg px-2 py-1.5 text-sm font-medium text-ink hover:bg-black/5">
            Admin — pending stores
          </Link>
        )}
        {user.roles.includes('ADMIN') && (
          <Link to="/admin/orders" className="block rounded-lg px-2 py-1.5 text-sm font-medium text-ink hover:bg-black/5">
            Admin — all orders
          </Link>
        )}
        {user.roles.includes('SUPERMARKET_OWNER') && (
          <Link to="/my-store" className="block rounded-lg px-2 py-1.5 text-sm font-medium text-ink hover:bg-black/5">
            My store
          </Link>
        )}
        {user.roles.includes('SUPERMARKET_OWNER') && (
          <Link
            to="/my-store/orders"
            className="block rounded-lg px-2 py-1.5 text-sm font-medium text-ink hover:bg-black/5"
          >
            My store — orders
          </Link>
        )}
        {user.roles.includes('CUSTOMER') && (
          <Link to="/orders" className="block rounded-lg px-2 py-1.5 text-sm font-medium text-ink hover:bg-black/5">
            My orders
          </Link>
        )}
        {user.roles.includes('CUSTOMER') && (
          <Link to="/addresses" className="block rounded-lg px-2 py-1.5 text-sm font-medium text-ink hover:bg-black/5">
            My addresses
          </Link>
        )}
        <button
          type="button"
          onClick={handleLogout}
          className="mt-1 block w-full rounded-lg px-2 py-1.5 text-left text-sm font-medium text-danger-500 hover:bg-black/5"
        >
          Log out
        </button>
      </div>
    </details>
  )
}

/** Persistent mobile-first shell: brand header + bottom tab bar, matching the natural flow order. */
export function Layout() {
  const { cart } = useCart()
  const { location } = useUserLocation()
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
        <div className="flex min-w-0 items-center gap-3">
          {location && (
            <Link to="/" className="flex min-w-0 items-center gap-1 truncate text-xs text-ink-muted">
              <MapPinIcon className="h-3.5 w-3.5 shrink-0" />
              <span className="truncate">{location.label}</span>
            </Link>
          )}
          <AccountMenu />
        </div>
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
        <NavLink to="/orders" className={navLinkClass}>
          <ClipboardIcon className="h-6 w-6" />
          Orders
        </NavLink>
      </nav>
    </div>
  )
}
