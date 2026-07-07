package com.aislego.payments;

import com.aislego.common.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Zero-setup stand-in for a real payment gateway (Razorpay/Stripe/UPI/etc.). No card/UPI
 * details ever pass through this class or get persisted anywhere, only a generated gateway
 * reference and outcome, matching the "payment details must never be stored directly" system
 * requirement.
 *
 * <p>Unlike a real gateway there is no client-side widget to redirect to, so
 * {@link #createIntent} never requires client action - the caller can immediately call
 * {@link #verifyAndCapture} with an empty payload. Deterministic for demo/testing purposes:
 * verification always succeeds. This is a stand-in, not a simulation of real-world decline
 * rates.
 *
 * <p>Explicitly mutually exclusive with {@link RazorpayPaymentGateway} via the same property
 * (matching "mock" or unset) - without this, setting {@code aislego.payments.provider=razorpay}
 * would leave *both* beans active and crash the app at startup with a
 * {@code NoUniqueBeanDefinitionException}.
 */
@Service
@ConditionalOnProperty(name = "aislego.payments.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);
    private static final BigDecimal MINOR_UNITS_PER_MAJOR_UNIT = BigDecimal.valueOf(100);

    @Override
    public PaymentIntent createIntent(Long orderId, Money amount) {
        String reference = "MOCK-" + UUID.randomUUID();
        long amountMinorUnits = amount.getAmount().multiply(MINOR_UNITS_PER_MAJOR_UNIT).longValueExact();

        log.info("Mock payment gateway created intent for order {} ({}), reference {}",
                orderId, amount, reference);
        return new PaymentIntent(reference, false, null, amountMinorUnits, amount.getCurrencyCode());
    }

    @Override
    public PaymentVerificationResult verifyAndCapture(String gatewayReference, PaymentVerificationRequest request) {
        log.info("Mock payment gateway confirmed payment for reference {}", gatewayReference);
        return new PaymentVerificationResult(true, "Payment confirmed");
    }
}
