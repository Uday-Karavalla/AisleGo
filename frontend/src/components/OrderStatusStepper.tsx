import { ORDER_STAGES, ORDER_STAGE_LABELS } from '../api/orders'
import type { OrderStage } from '../api/orders'
import { CheckIcon } from './icons'

interface OrderStatusStepperProps {
  currentStage: OrderStage
}

type StepState = 'done' | 'current' | 'upcoming'

/** Vertical stepper for the fixed ORDER_STAGES workflow, highlighting the current stage. */
export function OrderStatusStepper({ currentStage }: OrderStatusStepperProps) {
  if (currentStage === 'CANCELLED') {
    return (
      <div className="card border-2 border-danger-500/30 bg-danger-50 text-center">
        <p className="font-semibold text-danger-600">This order was cancelled.</p>
      </div>
    )
  }

  const currentIndex = ORDER_STAGES.indexOf(currentStage)

  return (
    <ol aria-label="Order progress">
      {ORDER_STAGES.map((stage, index) => {
        const state: StepState = index < currentIndex ? 'done' : index === currentIndex ? 'current' : 'upcoming'
        const isLast = index === ORDER_STAGES.length - 1

        return (
          <li
            key={stage}
            data-state={state}
            aria-current={state === 'current' ? 'step' : undefined}
            className="flex gap-3"
          >
            <div className="flex flex-col items-center">
              <span
                className={
                  'flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 text-xs font-bold ' +
                  (state === 'done'
                    ? 'border-brand-600 bg-brand-600 text-white'
                    : state === 'current'
                      ? 'border-brand-600 bg-white text-brand-600 ring-4 ring-brand-100'
                      : 'border-black/10 bg-white text-ink-faint')
                }
              >
                {state === 'done' ? <CheckIcon className="h-4 w-4" /> : index + 1}
              </span>
              {!isLast && (
                <span
                  className={'min-h-[1.75rem] w-0.5 flex-1 ' + (state === 'done' ? 'bg-brand-600' : 'bg-black/10')}
                />
              )}
            </div>
            <div className={'pb-7 ' + (state === 'upcoming' ? 'text-ink-faint' : 'text-ink')}>
              <p className={'text-sm ' + (state === 'current' ? 'font-bold' : 'font-medium')}>
                {ORDER_STAGE_LABELS[stage]}
              </p>
              {state === 'current' && <p className="text-xs text-brand-700">In progress</p>}
            </div>
          </li>
        )
      })}
    </ol>
  )
}
