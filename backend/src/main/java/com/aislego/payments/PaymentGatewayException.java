package com.aislego.payments;

import com.aislego.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a real gateway call fails in a way the caller can't recover from in-request. */
public class PaymentGatewayException extends ApiException {
    public PaymentGatewayException(String message) {
        super(HttpStatus.BAD_GATEWAY, "PAYMENT_GATEWAY_ERROR", message);
    }
}
