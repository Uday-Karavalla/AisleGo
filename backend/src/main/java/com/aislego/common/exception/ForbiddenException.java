package com.aislego.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Generic 403: the caller is authenticated and the resource exists, but the action is
 * blocked by account state (e.g. an unverified email) rather than by role/ownership -
 * those cases stay 404/401 as elsewhere in this codebase.
 */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }
}
