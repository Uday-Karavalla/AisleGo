package com.aislego.orders.web;

import com.aislego.common.exception.BadRequestException;
import com.aislego.common.security.AuthenticatedUser;
import com.aislego.orders.dto.CheckoutRequest;
import com.aislego.orders.dto.CheckoutResponse;
import com.aislego.orders.dto.OrderResponse;
import com.aislego.orders.service.CheckoutService;
import com.aislego.payments.PaymentVerificationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@PreAuthorize("hasRole('CUSTOMER')")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    /**
     * Places an order from the current cart and creates a payment intent with the active
     * gateway. {@code Idempotency-Key} is required so a client can safely retry (e.g. after a
     * network timeout) without risking a duplicate gateway order or a duplicate stock
     * reservation. Payment is not yet confirmed at this point - see
     * {@link #verifyPayment}.
     */
    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(@AuthenticationPrincipal AuthenticatedUser principal,
                                                       @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                       @Valid @RequestBody CheckoutRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required for checkout");
        }
        CheckoutResponse response = checkoutService.checkout(principal.userId(), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Completes the two-phase payment flow: Mock is called with an empty payload immediately
     * after {@link #checkout}; Razorpay is called with the {@code razorpay_order_id}/
     * {@code razorpay_payment_id}/{@code razorpay_signature} the client received from the
     * Checkout.js widget.
     */
    @PostMapping("/{orderId}/payment/verify")
    public ResponseEntity<OrderResponse> verifyPayment(@AuthenticationPrincipal AuthenticatedUser principal,
                                                         @PathVariable Long orderId,
                                                         @RequestBody(required = false) PaymentVerificationRequest request) {
        PaymentVerificationRequest body = request != null ? request : new PaymentVerificationRequest(null, null, null);
        OrderResponse response = checkoutService.verifyPayment(principal.userId(), orderId, body);
        return ResponseEntity.ok(response);
    }
}
