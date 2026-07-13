import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { DownloadIcon, XIcon } from './icons'

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>
}

const DISMISSED_AT_KEY = 'aislego.installPromptDismissedAt'
const DISMISS_COOLDOWN_MS = 14 * 24 * 60 * 60 * 1000

function recentlyDismissed(): boolean {
  const value = Number(localStorage.getItem(DISMISSED_AT_KEY))
  return Number.isFinite(value) && value > 0 && Date.now() - value < DISMISS_COOLDOWN_MS
}

function isRunningStandalone(): boolean {
  const navigatorWithStandalone = navigator as Navigator & { standalone?: boolean }
  return navigatorWithStandalone.standalone === true
    || (typeof window.matchMedia === 'function' && window.matchMedia('(display-mode: standalone)').matches)
}

function isIosBrowser(): boolean {
  return /iPad|iPhone|iPod/.test(navigator.userAgent)
    || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
}

/** Browser-native PWA installation when supported, with honest iOS home-screen guidance. */
export function PwaInstallPrompt() {
  const location = useLocation()
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null)
  const [showIosHelp, setShowIosHelp] = useState(false)
  const [hidden, setHidden] = useState(() => isRunningStandalone() || recentlyDismissed())

  useEffect(() => {
    if (hidden) return
    if (isIosBrowser()) setShowIosHelp(true)

    function handleBeforeInstall(event: Event) {
      event.preventDefault()
      setDeferredPrompt(event as BeforeInstallPromptEvent)
    }

    function handleInstalled() {
      setDeferredPrompt(null)
      setShowIosHelp(false)
      setHidden(true)
    }

    window.addEventListener('beforeinstallprompt', handleBeforeInstall)
    window.addEventListener('appinstalled', handleInstalled)
    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstall)
      window.removeEventListener('appinstalled', handleInstalled)
    }
  }, [hidden])

  function dismiss() {
    localStorage.setItem(DISMISSED_AT_KEY, String(Date.now()))
    setHidden(true)
  }

  async function install() {
    if (!deferredPrompt) return
    await deferredPrompt.prompt()
    await deferredPrompt.userChoice
    dismiss()
  }

  const isDiscoveryPage = location.pathname === '/' || location.pathname === '/stores'
  if (hidden || !isDiscoveryPage || (!deferredPrompt && !showIosHelp)) return null

  return (
    <aside
      role="dialog"
      aria-label="Install AisleGo"
      className="fixed inset-x-3 bottom-20 z-40 mx-auto max-w-md rounded-3xl border border-brand-100 bg-white p-4 shadow-pop md:bottom-6"
    >
      <button
        type="button"
        onClick={dismiss}
        aria-label="Dismiss install prompt"
        className="absolute right-3 top-3 rounded-full p-1.5 text-ink-faint hover:bg-black/5"
      >
        <XIcon className="h-4 w-4" />
      </button>
      <div className="flex gap-3 pr-7">
        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-brand-600 text-white">
          <DownloadIcon className="h-5 w-5" />
        </span>
        <div>
          <p className="font-extrabold text-ink">Install AisleGo</p>
          <p className="mt-0.5 text-xs leading-relaxed text-ink-muted">
            {showIosHelp && !deferredPrompt
              ? 'For faster access, tap Share in Safari and choose “Add to Home Screen”.'
              : 'Add AisleGo to your home screen for faster access to stores, carts, and orders.'}
          </p>
        </div>
      </div>
      {deferredPrompt && (
        <button type="button" onClick={install} className="btn-primary mt-3 w-full py-2.5 text-sm">
          <DownloadIcon className="h-4 w-4" /> Install app
        </button>
      )}
      {showIosHelp && !deferredPrompt && (
        <button type="button" onClick={dismiss} className="btn-secondary mt-3 w-full py-2.5 text-sm">
          Got it
        </button>
      )}
    </aside>
  )
}
