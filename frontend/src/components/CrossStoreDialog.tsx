import { useCart } from '../context/CartContext'
import { Dialog } from './Dialog'
import { AlertIcon } from './icons'

/**
 * Enforces the core business rule: a cart can only hold products from one
 * supermarket. Rendered once near the app root; CartContext decides when it's visible.
 */
export function CrossStoreDialog() {
  const { pendingConflict, confirmSwitchStore, cancelSwitchStore } = useCart()

  return (
    <Dialog
      open={pendingConflict !== null}
      onClose={cancelSwitchStore}
      title="Switch supermarkets?"
      actions={
        <>
          <button type="button" className="btn-primary" onClick={confirmSwitchStore}>
            Clear cart &amp; switch store
          </button>
          <button type="button" className="btn-ghost" onClick={cancelSwitchStore}>
            Cancel
          </button>
        </>
      }
    >
      {pendingConflict && (
        <div className="flex items-start gap-3">
          <AlertIcon className="mt-0.5 h-5 w-5 shrink-0 text-warning-500" />
          <p>
            Your cart has items from <strong className="text-ink">{pendingConflict.currentStoreName}</strong>.
            AisleGo orders can only contain products from one supermarket at a time. Adding items from{' '}
            <strong className="text-ink">{pendingConflict.input.storeName}</strong> will clear your current cart.
          </p>
        </div>
      )}
    </Dialog>
  )
}
