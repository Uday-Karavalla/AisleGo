// Minimal ambient typing for the Razorpay Checkout.js widget, loaded via a <script>
// tag in index.html. Only the options/fields this app actually uses are declared.
export {}

declare global {
  interface RazorpayPaymentResponse {
    razorpay_payment_id: string
    razorpay_order_id: string
    razorpay_signature: string
  }

  interface RazorpayOptions {
    key: string
    order_id: string
    /** Amount in the currency's minor unit (e.g. paise for INR). */
    amount: number
    currency: string
    name: string
    handler: (response: RazorpayPaymentResponse) => void
    modal?: {
      ondismiss?: () => void
    }
  }

  interface RazorpayInstance {
    open: () => void
  }

  interface Window {
    Razorpay: new (options: RazorpayOptions) => RazorpayInstance
  }
}
