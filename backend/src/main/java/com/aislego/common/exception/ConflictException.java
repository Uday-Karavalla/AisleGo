package com.aislego.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Generic 409. Used for the cross-store cart rule, idempotency-key replays that collide
 * mid-flight, and optimistic-locking conflicts surfaced to the caller.
 */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
