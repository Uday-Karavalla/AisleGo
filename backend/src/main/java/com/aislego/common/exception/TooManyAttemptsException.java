package com.aislego.common.exception;

import org.springframework.http.HttpStatus;

/** 429: too many failed login attempts for one account in a short window - see
 *  {@code LoginRateLimiter}. */
public class TooManyAttemptsException extends ApiException {
    public TooManyAttemptsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS", message);
    }
}
