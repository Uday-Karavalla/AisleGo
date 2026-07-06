package com.aislego.common.web;

import java.time.Instant;
import java.util.List;

/**
 * Consistent shape for every error the API returns, whether it came from a business
 * exception, bean validation, or an unexpected failure.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors
) {
    public record FieldViolation(String field, String message) {
    }
}
