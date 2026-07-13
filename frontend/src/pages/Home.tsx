import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { trackEvent } from '../api/growth'
import { Link, useNavigate } from 'react-router-dom'
import { useGeolocation } from '../hooks/useGeolocation'
import { useUserLocation } from '../context/LocationContext'
import { storesApi } from '../api/stores'
import {
  CartIcon,
  CheckIcon,
  ClipboardIcon,
  ClockIcon,
  MapPinIcon,
  SearchIcon,
  ShareIcon,
  ShieldCheckIcon,
  StarIcon,
  StoreIcon,
  TagIcon,
  TruckIcon,
} from '../components/icons'

/** One photo tile in the hero's grid - a real product photo (see public/images/) with a dark
 *  gradient scrim at the bottom so the label stays readable regardless of the photo's own
 *  brightness/colour. Clickable once a location is known, straight into that category's
 *  cross-store browse page (see CategoryBrowse.tsx) - before that, there's nowhere useful to
 *  send it, so it stays decorative. */
function PhotoTile({
  src,
  label,
  className,
  onClick,
}: {
  src: string
  label: string
  className?: string
  onClick?: () => void
}) {
  const content = (
    <>
      <img src={src} alt="" className="h-full w-full object-cover" />
      <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/70 to-transparent p-4 pt-10">
        <p className="text-sm font-bold text-white">{label}</p>
      </div>
    </>
  )
  const sharedClassName = `relative block w-full overflow-hidden rounded-3xl text-left shadow-pop transition active:scale-[0.98] ${className ?? ''}`

  return onClick ? (
    <button type="button" onClick={onClick} className={sharedClassName}>
      {content}
    </button>
  ) : (
    <div className={sharedClassName}>{content}</div>
  )
}

const TICKER_CATEGORIES = [
  { emoji: '🍌', label: 'Bananas' },
  { emoji: '🧃', label: 'Juices' },
  { emoji: '🥬', label: 'Veggies' },
  { emoji: '🫘', label: 'Pulses' },
  { emoji: '🍿', label: 'Snacks' },
  { emoji: '🧹', label: 'Household' },
  { emoji: '🍅', label: 'Tomatoes' },
  { emoji: '🍚', label: 'Rice' },
  { emoji: '🥛', label: 'Milk' },
  { emoji: '🍞', label: 'Bread' },
  { emoji: '🧴', label: 'Care' },
  { emoji: '🌶️', label: 'Spices' },
]

const SHOP_CATEGORIES = [
  { emoji: '🥬', label: 'Fresh vegetables', slug: 'vegetable', colour: 'bg-emerald-50' },
  { emoji: '🍎', label: 'Fruits', slug: 'fruit', colour: 'bg-rose-50' },
  { emoji: '🥛', label: 'Dairy & eggs', slug: 'dairy', colour: 'bg-sky-50' },
  { emoji: '🍞', label: 'Bakery', slug: 'bakery', colour: 'bg-amber-50' },
  { emoji: '🍿', label: 'Snacks', slug: 'snack', colour: 'bg-orange-50' },
  { emoji: '🧹', label: 'Household', slug: 'household', colour: 'bg-violet-50' },
]

export default function Home() {
  const navigate = useNavigate()
  const { state, detect } = useGeolocation()
  const { location, setGpsLocation, setManualLocation } = useUserLocation()
  const [manualAddress, setManualAddress] = useState('')
  const [manualFormOpen, setManualFormOpen] = useState(false)
  const [isGeocoding, setIsGeocoding] = useState(false)
  const [manualError, setManualError] = useState<string | null>(null)
  const [shareStatus, setShareStatus] = useState<string | null>(null)

  useEffect(() => {
    if (state.status === 'success') {
      setGpsLocation(state.coords.lat, state.coords.lng)
      navigate('/stores')
    }
  }, [state, setGpsLocation, navigate])

  const showManualForm = manualFormOpen || state.status === 'error'

  async function handleManualSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmed = manualAddress.trim()
    if (!trimmed || isGeocoding) return
    setManualError(null)
    setIsGeocoding(true)
    try {
      const coords = await storesApi.geocode(trimmed)
      if (!coords) {
        setManualError("Couldn't find that address — try enabling location instead.")
        return
      }
      setManualLocation(trimmed, coords.lat, coords.lng)
      navigate('/stores')
    } catch {
      setManualError("Couldn't find that address — try enabling location instead.")
    } finally {
      setIsGeocoding(false)
    }
  }

  function openCategory(slug: string) {
    if (location) {
      navigate(`/category/${slug}`)
      return
    }
    detect()
  }

  async function shareAisleGo() {
    trackEvent('share', { channel: navigator.share ? 'native' : 'clipboard', referral: false })
    const url = 'https://aislego-frontend.onrender.com/'
    const data = {
      title: 'AisleGo',
      text: 'Shop nearby supermarkets with coupons, delivery, or free store pickup.',
      url,
    }
    try {
      if (navigator.share) {
        await navigator.share(data)
        setShareStatus('Thanks for sharing AisleGo!')
      } else if (navigator.clipboard) {
        await navigator.clipboard.writeText(url)
        setShareStatus('AisleGo link copied!')
      } else {
        setShareStatus(url)
      }
    } catch (error) {
      if (!(error instanceof DOMException && error.name === 'AbortError')) {
        setShareStatus('Could not share right now. Please try again.')
      }
    }
  }

  return (
    <div className="flex flex-col">
      <section className="relative overflow-hidden bg-gradient-to-br from-brand-800 via-brand-700 to-brand-500 px-5 py-14 text-white md:px-10 lg:py-20">
        <div
          className="pointer-events-none absolute -right-24 -top-24 h-80 w-80 rounded-full bg-white/10 blur-3xl"
          aria-hidden="true"
        />
        <div
          className="pointer-events-none absolute -bottom-32 left-1/3 h-72 w-72 rounded-full bg-brand-300/20 blur-3xl"
          aria-hidden="true"
        />

        <div className="page-wide relative z-10 flex flex-col gap-10 lg:flex-row lg:items-center">
          <div className="flex flex-col gap-5 lg:w-1/2">
            <span className="inline-flex w-fit items-center gap-1.5 rounded-full border border-white/15 bg-white/15 px-3 py-1.5 text-xs font-semibold backdrop-blur">
              <StarIcon className="h-3.5 w-3.5 text-warning-500" />
              Your neighbourhood supermarkets, online
            </span>

            <h1 className="text-3xl font-extrabold leading-tight md:text-5xl">
              Groceries you trust. <span className="text-brand-200">Savings you can see.</span>
            </h1>
            <p className="max-w-xl text-sm leading-relaxed text-white/85 md:text-lg">
              Shop real local supermarkets, apply coupons at checkout, and choose doorstep delivery or free store
              pickup—all in one simple order.
            </p>

            {location && (
              <button
                type="button"
                onClick={() => navigate('/stores')}
                className="flex items-center gap-3 rounded-2xl bg-white/10 px-4 py-3 text-left backdrop-blur transition active:scale-[0.99]"
              >
                <MapPinIcon className="h-5 w-5 shrink-0" />
                <span className="text-sm">
                  Continue with <strong>{location.label}</strong>
                </span>
              </button>
            )}

            <div className="flex flex-col gap-3 sm:flex-row">
              <button
                type="button"
                onClick={detect}
                disabled={state.status === 'loading'}
                className="inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-5 py-3.5 text-base font-semibold text-brand-700 shadow-pop transition active:scale-[0.98] disabled:opacity-70"
              >
                <MapPinIcon className="h-5 w-5" />
                {state.status === 'loading' ? 'Detecting your location…' : 'Find stores near me'}
              </button>

              <button
                type="button"
                onClick={() => setManualFormOpen((prev) => !prev)}
                className="inline-flex items-center justify-center gap-2 rounded-2xl border-2 border-white/40 px-5 py-3.5 text-base font-semibold text-white transition active:scale-[0.98] active:bg-white/10"
              >
                <SearchIcon className="h-5 w-5" />
                Enter address manually
              </button>
            </div>

            <div className="flex flex-wrap gap-x-5 gap-y-2 text-xs font-medium text-white/80">
              <span className="inline-flex items-center gap-1.5"><TagIcon className="h-4 w-4" /> Coupons at checkout</span>
              <span className="inline-flex items-center gap-1.5"><StoreIcon className="h-4 w-4" /> Free store pickup</span>
              <span className="inline-flex items-center gap-1.5"><ClipboardIcon className="h-4 w-4" /> Live order tracking</span>
            </div>

            {state.status === 'error' && (
              <p role="alert" className="text-sm text-warning-500">
                {state.message}
              </p>
            )}
          </div>

          <div className="w-full lg:w-[46%]">
            <div className="grid h-[260px] grid-cols-2 grid-rows-2 gap-3 sm:h-[320px] lg:h-[380px] lg:gap-4">
              <PhotoTile
                src="/images/vegetables.jpg"
                label="Fresh veggies daily"
                className="row-span-2"
                onClick={location ? () => navigate('/category/vegetable') : undefined}
              />
              <PhotoTile
                src="/images/milk.jpg"
                label="Dairy & more"
                onClick={location ? () => navigate('/category/dairy') : undefined}
              />
              <PhotoTile
                src="/images/fruit.jpg"
                label="Fresh fruit"
                onClick={location ? () => navigate('/category/fruit') : undefined}
              />
            </div>
          </div>
        </div>
      </section>

      {showManualForm && (
        <div className="page-shell justify-center px-5 py-6">
          <div className="page-narrow">
            <form onSubmit={handleManualSubmit} className="card flex flex-col gap-3">
              <label htmlFor="manual-address" className="text-sm font-semibold text-ink">
                Delivery address
              </label>
              <input
                id="manual-address"
                className="input-field"
                placeholder="House no, street, area, city"
                value={manualAddress}
                onChange={(event) => {
                  setManualAddress(event.target.value)
                  if (manualError) setManualError(null)
                }}
              />
              {manualError && (
                <p role="alert" className="text-sm text-danger-500">
                  {manualError}
                </p>
              )}
              <button type="submit" className="btn-primary" disabled={!manualAddress.trim() || isGeocoding}>
                {isGeocoding ? 'Finding address…' : 'Continue'}
              </button>
            </form>
          </div>
        </div>
      )}

      <div className="overflow-hidden border-y border-black/5 bg-white py-3">
        <div className="flex w-max animate-marquee gap-6">
          {[...TICKER_CATEGORIES, ...TICKER_CATEGORIES].map((item, index) => (
            <span
              key={`${item.label}-${index}`}
              className="flex shrink-0 items-center gap-1.5 whitespace-nowrap text-sm font-semibold text-ink-muted"
            >
              <span aria-hidden="true">{item.emoji}</span>
              {item.label}
            </span>
          ))}
        </div>
      </div>

      <section className="px-5 py-12 md:px-8 md:py-16">
        <div className="page-wide">
          <div className="mb-7 flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-brand-600">Start with an aisle</p>
              <h2 className="mt-2 text-2xl font-extrabold text-ink md:text-3xl">What are you shopping for?</h2>
              <p className="mt-1 text-sm text-ink-muted">Find products across supermarkets near your location.</p>
            </div>
            {location && (
              <Link to="/stores" className="text-sm font-bold text-brand-700 hover:text-brand-800">
                See all nearby stores →
              </Link>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
            {SHOP_CATEGORIES.map((category) => (
              <button
                key={category.slug}
                type="button"
                onClick={() => openCategory(category.slug)}
                className={`${category.colour} group rounded-3xl border border-black/5 p-4 text-left shadow-card transition hover:-translate-y-1 hover:shadow-pop active:scale-[0.98]`}
              >
                <span className="text-4xl transition group-hover:scale-110" aria-hidden="true">{category.emoji}</span>
                <span className="mt-4 block text-sm font-bold text-ink">{category.label}</span>
                <span className="mt-1 block text-xs font-semibold text-brand-700">Shop now →</span>
              </button>
            ))}
          </div>
        </div>
      </section>

      <section className="border-y border-black/5 bg-white px-5 py-12 md:px-8 md:py-16">
        <div className="page-wide">
          <div className="mx-auto max-w-2xl text-center">
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-brand-600">Why AisleGo</p>
            <h2 className="mt-2 text-2xl font-extrabold text-ink md:text-3xl">More control over every grocery run</h2>
            <p className="mt-2 text-sm leading-relaxed text-ink-muted md:text-base">
              Pick the store, understand the total, and follow your order from payment to handoff.
            </p>
          </div>

          <div className="mt-8 grid gap-4 md:grid-cols-3">
            {[
              {
                icon: <TagIcon className="h-6 w-6" />,
                title: 'Real savings at checkout',
                description: 'Apply an active store or platform coupon and see the discount before you place the order.',
              },
              {
                icon: <TruckIcon className="h-6 w-6" />,
                title: 'Delivery on your terms',
                description: 'Choose ASAP or scheduled delivery—or collect from the store with no delivery fee.',
              },
              {
                icon: <ShieldCheckIcon className="h-6 w-6" />,
                title: 'Clear, protected checkout',
                description: 'Your subtotal, fee, coupon savings, and final payment amount stay consistent end to end.',
              },
            ].map((benefit) => (
              <article key={benefit.title} className="rounded-3xl border border-black/5 bg-surface-muted p-6">
                <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-600 text-white shadow-card">
                  {benefit.icon}
                </span>
                <h3 className="mt-5 text-lg font-extrabold text-ink">{benefit.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-ink-muted">{benefit.description}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="px-5 py-12 md:px-8 md:py-16">
        <div className="page-wide grid gap-10 lg:grid-cols-[0.9fr_1.1fr] lg:items-center">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-brand-600">Simple by design</p>
            <h2 className="mt-2 text-2xl font-extrabold text-ink md:text-3xl">From nearby shelf to your home in three steps</h2>
            <p className="mt-3 max-w-lg text-sm leading-relaxed text-ink-muted md:text-base">
              No endless setup. Tell us where you are, choose a supermarket, and check out with the option that fits your day.
            </p>
            <button type="button" onClick={detect} className="btn-primary mt-6">
              <MapPinIcon className="h-5 w-5" />
              Start shopping nearby
            </button>
          </div>

          <ol className="grid gap-3 sm:grid-cols-3">
            {[
              { icon: <MapPinIcon className="h-5 w-5" />, title: 'Set location', copy: 'See supermarkets and arrival estimates near you.' },
              { icon: <CartIcon className="h-5 w-5" />, title: 'Fill your cart', copy: 'Browse one trusted store and apply a valid coupon.' },
              { icon: <ClockIcon className="h-5 w-5" />, title: 'Choose handoff', copy: 'Get it ASAP, schedule delivery, or pick it up free.' },
            ].map((step, index) => (
              <li key={step.title} className="relative rounded-3xl bg-white p-5 shadow-card">
                <span className="absolute right-4 top-4 text-3xl font-black text-brand-100">0{index + 1}</span>
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-700">{step.icon}</span>
                <h3 className="mt-5 font-extrabold text-ink">{step.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-ink-muted">{step.copy}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="px-5 pb-12 md:px-8 md:pb-16">
        <div className="page-wide overflow-hidden rounded-[2rem] bg-gradient-to-r from-brand-900 via-brand-800 to-brand-600 px-6 py-9 text-white shadow-pop md:px-10 md:py-12">
          <div className="grid gap-8 md:grid-cols-[1fr_auto] md:items-center">
            <div>
              <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-xs font-bold">
                <CheckIcon className="h-4 w-4 text-brand-200" /> Built for local shopping
              </span>
              <h2 className="mt-4 max-w-2xl text-2xl font-extrabold md:text-3xl">Ready to make your next grocery run easier?</h2>
              <p className="mt-2 max-w-xl text-sm leading-relaxed text-white/75 md:text-base">
                Discover nearby catalogues, keep one clear cart, and choose exactly how you receive the order.
              </p>
            </div>
            <div className="flex flex-col gap-2 sm:flex-row md:flex-col">
              <button type="button" onClick={detect} className="inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-6 py-3.5 font-bold text-brand-800 shadow-card transition hover:-translate-y-0.5">
                Find my stores <MapPinIcon className="h-5 w-5" />
              </button>
              <button type="button" onClick={shareAisleGo} className="inline-flex items-center justify-center gap-2 rounded-2xl border border-white/25 px-6 py-3 text-sm font-bold text-white transition hover:bg-white/10">
                Share AisleGo <ShareIcon className="h-4 w-4" />
              </button>
              {shareStatus && <p role="status" className="max-w-xs text-center text-xs text-white/70">{shareStatus}</p>}
            </div>
          </div>
        </div>
      </section>

      <section className="border-t border-black/5 bg-white px-5 py-12 md:px-8 md:py-16">
        <div className="mx-auto w-full max-w-3xl">
          <div className="text-center">
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-brand-600">Good to know</p>
            <h2 className="mt-2 text-2xl font-extrabold text-ink md:text-3xl">Questions before your first order?</h2>
          </div>
          <div className="mt-7 flex flex-col gap-3">
            {[
              {
                question: 'How do coupons work?',
                answer: 'When you are signed in, the cart shows active coupons eligible for that supermarket. Apply one with a tap and review the exact savings before checkout.',
              },
              {
                question: 'Can I avoid the delivery fee?',
                answer: 'Yes. Choose store pickup at checkout and the delivery fee becomes zero. Delivery orders show the fee clearly before payment.',
              },
              {
                question: 'Can one order include several supermarkets?',
                answer: 'No. Each cart stays with one supermarket so its pricing, inventory, coupon eligibility, and fulfilment remain clear and reliable.',
              },
              {
                question: 'Can I choose when the order arrives?',
                answer: 'Yes. Choose ASAP delivery, schedule a future delivery time, or collect the order from the store.',
              },
            ].map((item) => (
              <details key={item.question} className="group rounded-2xl border border-black/5 bg-surface-muted px-5 py-4">
                <summary className="flex cursor-pointer list-none items-center justify-between gap-4 font-bold text-ink">
                  {item.question}
                  <span className="text-xl text-brand-700 transition group-open:rotate-45" aria-hidden="true">+</span>
                </summary>
                <p className="mt-3 max-w-2xl text-sm leading-relaxed text-ink-muted">{item.answer}</p>
              </details>
            ))}
          </div>
        </div>
      </section>

      <section className="border-t border-black/5 bg-surface-muted px-5 py-9 md:px-8">
        <div className="page-wide flex flex-col justify-between gap-6 sm:flex-row sm:items-center">
          <div>
            <p className="font-extrabold text-ink">Own a supermarket?</p>
            <p className="mt-1 text-sm text-ink-muted">Bring your catalogue online and manage branches, stock, coupons, and orders.</p>
          </div>
          <Link to="/register-store" className="btn-secondary shrink-0">List your store</Link>
        </div>
      </section>
    </div>
  )
}
