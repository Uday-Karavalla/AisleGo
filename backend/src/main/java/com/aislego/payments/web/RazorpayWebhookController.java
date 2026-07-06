package com.aislego.payments.web;

import com.aislego.orders.domain.Order;
import com.aislego.orders.repository.OrderRepository;
import com.aislego.orders.service.CheckoutService;
import com.aislego.payments.PaymentVerificationResult;
import com.aislego.payments.domain.Payment;
import com.aislego.payments.repository.PaymentRepository;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-to-server callback from Razorpay - defense-in-depth for users who close the browser
 * before the client-side {@code POST /api/checkout/{orderId}/payment/verify} call fires.
 * Public (see {@link com.aislego.common.security.SecurityConfig}), so authenticity comes
 * entirely from the {@code X-Razorpay-Signature} HMAC check, not a logged-in user.
 *
 * <p>Always acknowledges with 200, even when the payload is unparseable or the signature is
 * bad - Razorpay retries aggressively on non-2xx, and there's no user request on the other end
 * of this call to report an error to. Every failure mode is logged instead.
 *
 * <p>Cannot be live-tested from this sandboxed dev box without a public URL - see the plan's
 * verification notes.
 */
@RestController
@RequestMapping("/api/payments/webhook")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CheckoutService checkoutService;
    private final String webhookSecret;

    public RazorpayWebhookController(PaymentRepository paymentRepository, OrderRepository orderRepository,
                                      CheckoutService checkoutService,
                                      @Value("${aislego.payments.razorpay.webhook-secret}") String webhookSecret) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.checkoutService = checkoutService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            if (!Utils.verifyWebhookSignature(payload, signature, webhookSecret)) {
                log.warn("Razorpay webhook signature verification failed");
                return ResponseEntity.ok().build();
            }

            JSONObject event = new JSONObject(payload);
            String eventType = event.optString("event", "");
            String gatewayOrderId = extractOrderId(event);

            if (gatewayOrderId == null) {
                log.warn("Razorpay webhook event {} did not contain a recognizable order id", eventType);
                return ResponseEntity.ok().build();
            }

            Payment payment = paymentRepository.findByGatewayReference(gatewayOrderId).orElse(null);
            if (payment == null) {
                log.warn("Razorpay webhook referenced unknown gateway order id {}", gatewayOrderId);
                return ResponseEntity.ok().build();
            }

            Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
            if (order == null) {
                log.warn("Razorpay webhook: order {} for payment {} not found", payment.getOrderId(), payment.getId());
                return ResponseEntity.ok().build();
            }

            if ("payment.captured".equals(eventType) || "payment.failed".equals(eventType)) {
                boolean success = "payment.captured".equals(eventType);
                PaymentVerificationResult result = new PaymentVerificationResult(success,
                        "Razorpay webhook event: " + eventType);
                checkoutService.applyVerificationResult(order, payment, result);
            } else {
                log.info("Ignoring unhandled Razorpay webhook event {}", eventType);
            }
        } catch (RazorpayException | JSONException ex) {
            log.warn("Could not process Razorpay webhook payload: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error processing Razorpay webhook", ex);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Razorpay's {@code payment.captured}/{@code payment.failed} webhook events nest the
     * order id differently depending on payload shape: newer payloads carry
     * {@code payload.payment.entity.order_id}; some events also include a top-level
     * {@code payload.order.entity.id}. Either reliably identifies the order the payment
     * belongs to.
     */
    private String extractOrderId(JSONObject event) {
        try {
            JSONObject nestedPayload = event.getJSONObject("payload");
            if (nestedPayload.has("payment")) {
                JSONObject paymentEntity = nestedPayload.getJSONObject("payment").getJSONObject("entity");
                if (paymentEntity.has("order_id") && !paymentEntity.isNull("order_id")) {
                    return paymentEntity.getString("order_id");
                }
            }
            if (nestedPayload.has("order")) {
                return nestedPayload.getJSONObject("order").getJSONObject("entity").getString("id");
            }
        } catch (JSONException ex) {
            log.warn("Unexpected Razorpay webhook payload shape: {}", ex.getMessage());
        }
        return null;
    }
}
