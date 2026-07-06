import type { ReactNode } from 'react'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  actions?: ReactNode
}

/** Generic bottom-sheet-on-mobile / centered-modal-on-desktop dialog. */
export function Dialog({ open, onClose, title, children, actions }: DialogProps) {
  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 sm:items-center sm:p-4"
      role="presentation"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="dialog-title"
        className="w-full max-w-app rounded-t-3xl bg-white p-6 shadow-pop sm:rounded-3xl"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="dialog-title" className="text-lg font-bold text-ink">
          {title}
        </h2>
        <div className="mt-2 text-sm leading-relaxed text-ink-muted">{children}</div>
        {actions && <div className="mt-6 flex flex-col gap-2">{actions}</div>}
      </div>
    </div>
  )
}
