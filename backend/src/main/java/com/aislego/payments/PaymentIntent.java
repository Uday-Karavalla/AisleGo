package com.aislego.payments;

/**
 * What a gateway returned after we asked it to set up a charge. For a provider whose widget
 * the client must complete directly with the gateway (Razorpay), {@code requiresClientAction}
 * is {@code true} and {@code gatewayReference}/{@code providerKeyId} are what the client needs
 * to open that widget. {@link MockPaymentGateway} never requires client action, since there is
 * no real widget to redirect to.
 */
public record PaymentIntent(
        String gatewayReference,
        boolean requiresClientAction,
        String providerKeyId,
        long amountMinorUnits,
        String currency
) {
}
