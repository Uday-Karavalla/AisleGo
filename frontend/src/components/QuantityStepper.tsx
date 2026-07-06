import { MinusIcon, PlusIcon } from './icons'

interface QuantityStepperProps {
  quantity: number
  onIncrement: () => void
  onDecrement: () => void
  min?: number
  max?: number
  size?: 'sm' | 'md'
  ariaLabel?: string
}

/** Large-touch-target +/- stepper used on product cards and cart lines. */
export function QuantityStepper({
  quantity,
  onIncrement,
  onDecrement,
  min = 0,
  max = 99,
  size = 'md',
  ariaLabel = 'Quantity',
}: QuantityStepperProps) {
  const dims = size === 'sm' ? 'h-9 w-9' : 'h-11 w-11'
  const textSize = size === 'sm' ? 'text-sm' : 'text-base'

  return (
    <div
      className="inline-flex items-center gap-1 rounded-2xl bg-brand-50"
      role="group"
      aria-label={ariaLabel}
    >
      <button
        type="button"
        onClick={onDecrement}
        disabled={quantity <= min}
        aria-label="Decrease quantity"
        className={`${dims} flex items-center justify-center rounded-2xl text-brand-700 transition active:scale-90 disabled:opacity-30`}
      >
        <MinusIcon className="h-4 w-4" />
      </button>
      <span className={`${textSize} min-w-[1.5rem] text-center font-semibold text-ink`}>{quantity}</span>
      <button
        type="button"
        onClick={onIncrement}
        disabled={quantity >= max}
        aria-label="Increase quantity"
        className={`${dims} flex items-center justify-center rounded-2xl text-brand-700 transition active:scale-90 disabled:opacity-30`}
      >
        <PlusIcon className="h-4 w-4" />
      </button>
    </div>
  )
}
