package com.aislego.payments;

import com.aislego.common.money.Money;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Real gateway: creates a Razorpay order server-side (amount in paise) and hands it back to
 * the client so its Checkout.js widget can complete the charge directly with Razorpay - card/
 * UPI details never reach this backend. Verification uses Razorpay's HMAC-SHA256 signature
 * scheme so a forged "it succeeded" callback from the client can't be trusted blindly.
 *
 * <p>Opt-in via {@code aislego.payments.provider=razorpay}; {@link MockPaymentGateway} remains
 * the default so the demo works with zero setup.
 */
@Service
@ConditionalOnProperty(name = "aislego.payments.provider", havingValue = "razorpay")
public class RazorpayPaymentGateway implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentGateway.class);
    private static final BigDecimal MINOR_UNITS_PER_MAJOR_UNIT = BigDecimal.valueOf(100);

    private final RazorpayClient client;
    private final String keyId;
    private final String keySecret;

    public RazorpayPaymentGateway(
            @Value("${aislego.payments.razorpay.key-id}") String keyId,
            @Value("${aislego.payments.razorpay.key-secret}") String keySecret) throws RazorpayException {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.client = new RazorpayClient(keyId, keySecret);
    }

    @Override
    public PaymentIntent createIntent(Long orderId, Money amount) {
        try {
            long amountMinorUnits = amount.getAmount().multiply(MINOR_UNITS_PER_MAJOR_UNIT).longValueExact();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountMinorUnits);
            orderRequest.put("currency", amount.getCurrencyCode());
            orderRequest.put("receipt", "aislego-order-" + orderId);

            com.razorpay.Order rzpOrder = client.orders.create(orderRequest);
            String gatewayReference = rzpOrder.get("id");

            return new PaymentIntent(gatewayReference, true, keyId, amountMinorUnits, amount.getCurrencyCode());
        } catch (RazorpayException ex) {
            log.error("Failed to create Razorpay order for order {}", orderId, ex);
            throw new PaymentGatewayException("Could not create Razorpay order: " + ex.getMessage());
        }
    }

    @Override
    public PaymentVerificationResult verifyAndCapture(String gatewayReference, PaymentVerificationRequest request) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", gatewayReference);
            attributes.put("razorpay_payment_id", request.gatewayPaymentId());
            attributes.put("razorpay_signature", request.gatewaySignature());

            boolean valid = Utils.verifyPaymentSignature(attributes, keySecret);
            return valid
                    ? new PaymentVerificationResult(true, "Payment verified")
                    : new PaymentVerificationResult(false, "Payment signature verification failed");
        } catch (RazorpayException ex) {
            log.warn("Razorpay signature verification error for reference {}: {}", gatewayReference, ex.getMessage());
            return new PaymentVerificationResult(false, "Signature verification error: " + ex.getMessage());
        }
    }
}
