import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

/** Shared shell for the legal/policy pages (Terms, Privacy, Refund & Cancellation) — plain
 *  prose in the app's usual card style, with cross-links to the other two so a reader lands
 *  on any one of them and can still reach the rest. */
export function LegalLayout({
  title,
  lastUpdated,
  children,
}: {
  title: string
  lastUpdated: string
  children: ReactNode
}) {
  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-6 px-5 py-8">
      <div>
        <h1 className="text-xl font-extrabold text-ink">{title}</h1>
        <p className="mt-1 text-xs text-ink-faint">Last updated {lastUpdated}</p>
      </div>

      <div className="card flex flex-col gap-4 text-sm leading-relaxed text-ink-muted [&_h2]:text-sm [&_h2]:font-bold [&_h2]:text-ink [&_p]:text-sm [&_ul]:list-disc [&_ul]:pl-5 [&_li]:text-sm">
        {children}
      </div>

      <div className="flex flex-col gap-1 text-xs text-ink-faint">
        <span>Related:</span>
        <div className="flex flex-wrap gap-x-3 gap-y-1">
          <Link to="/legal/terms" className="font-semibold text-brand-700">
            Terms of Service
          </Link>
          <Link to="/legal/privacy" className="font-semibold text-brand-700">
            Privacy Policy
          </Link>
          <Link to="/legal/refunds" className="font-semibold text-brand-700">
            Refund &amp; Cancellation Policy
          </Link>
        </div>
      </div>
    </div>
  )
}
