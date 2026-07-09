import { LegalLayout } from '../../components/LegalLayout'

export default function RefundPolicy() {
  return (
    <LegalLayout title="Refund & Cancellation Policy" lastUpdated="9 July 2026">
      <h2>Cancellations</h2>
      <ul>
        <li>
          If your payment doesn't go through, your order is automatically cancelled and no charge is made — you can
          simply try again.
        </li>
        <li>
          Once payment succeeds, the order goes to the store. A store can still cancel it before packing it — for
          example, if an item turns out to be out of stock — and you'll be notified in the app if that happens.
        </li>
        <li>
          If you need to cancel an order yourself after payment, contact the store or reach out to us at{' '}
          <a href="mailto:AisleGo@gmail.com" className="font-semibold text-brand-700">
            AisleGo@gmail.com
          </a>{' '}
          as soon as possible — we can usually cancel it if the store hasn't started picking/packing it yet.
        </li>
      </ul>

      <h2>Refunds</h2>
      <ul>
        <li>
          If an order is cancelled after you've already paid — whether by the store or on your request — you'll get
          a full refund to the original payment method you used.
        </li>
        <li>Refunds are processed back through Razorpay and typically reach your account within 5-7 business days, depending on your bank.</li>
        <li>
          If an item was missing, wrong, or damaged when delivered, contact us with your order number and we'll work
          with the store to resolve it — usually a refund for that item or a replacement.
        </li>
      </ul>

      <h2>What isn't covered</h2>
      <p>
        Once an order has been delivered and accepted, we can't offer a refund for a simple change of mind. This
        policy covers payment failures, store-side cancellations, and delivery problems (missing, wrong, or damaged
        items).
      </p>

      <h2>Contact</h2>
      <p>
        For any cancellation or refund request, email{' '}
        <a href="mailto:AisleGo@gmail.com" className="font-semibold text-brand-700">
          AisleGo@gmail.com
        </a>{' '}
        with your order number and we'll help sort it out.
      </p>
    </LegalLayout>
  )
}
