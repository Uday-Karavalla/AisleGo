import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { OrderStatusStepper } from '../components/OrderStatusStepper'
import { ORDER_STAGE_LABELS } from '../api/orders'

function stageListItem(label: string): HTMLElement {
  const node = screen.getByText(label).closest('li')
  if (!node) throw new Error(`Could not find <li> ancestor for "${label}"`)
  return node
}

describe('OrderStatusStepper', () => {
  it('marks the current stage with aria-current and data-state="current"', () => {
    render(<OrderStatusStepper currentStage="ACCEPTED_BY_STORE" />)

    const currentItem = stageListItem(ORDER_STAGE_LABELS.ACCEPTED_BY_STORE)
    expect(currentItem).toHaveAttribute('aria-current', 'step')
    expect(currentItem).toHaveAttribute('data-state', 'current')
  })

  it('marks earlier stages as done and later stages as upcoming, without a11y "current" leaking onto them', () => {
    render(<OrderStatusStepper currentStage="PACKING" />)

    const doneItem = stageListItem(ORDER_STAGE_LABELS.PLACED)
    expect(doneItem).toHaveAttribute('data-state', 'done')
    expect(doneItem).not.toHaveAttribute('aria-current')

    const upcomingItem = stageListItem(ORDER_STAGE_LABELS.OUT_FOR_DELIVERY)
    expect(upcomingItem).toHaveAttribute('data-state', 'upcoming')
    expect(upcomingItem).not.toHaveAttribute('aria-current')
  })

  it('re-highlights the correct stage when the order advances', () => {
    const { rerender } = render(<OrderStatusStepper currentStage="PLACED" />)
    expect(stageListItem(ORDER_STAGE_LABELS.PLACED)).toHaveAttribute('data-state', 'current')

    rerender(<OrderStatusStepper currentStage="DELIVERED" />)
    expect(stageListItem(ORDER_STAGE_LABELS.PLACED)).toHaveAttribute('data-state', 'done')
    expect(stageListItem(ORDER_STAGE_LABELS.DELIVERED)).toHaveAttribute('data-state', 'current')
  })

  it('renders a cancelled banner instead of the stepper when the order was cancelled', () => {
    render(<OrderStatusStepper currentStage="CANCELLED" />)
    expect(screen.getByText(/this order was cancelled/i)).toBeInTheDocument()
    expect(screen.queryByText(ORDER_STAGE_LABELS.PLACED)).not.toBeInTheDocument()
  })
})
