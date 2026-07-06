package com.aislego.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for all deliberately-thrown business exceptions. Carries an HTTP status and a
 * machine-readable code so {@link com.aislego.common.web.GlobalExceptionHandler} can turn
 * any subclass into a consistent error response without a giant if/else chain.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
