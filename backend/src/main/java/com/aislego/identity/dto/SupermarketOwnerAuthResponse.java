package com.aislego.identity.dto;

/**
 * Response for {@code POST /api/auth/register-supermarket-owner}: wraps the usual token
 * payload plus enough about the newly created supermarket for the frontend to immediately
 * show a "pending review" state, without touching the customer-facing {@link AuthResponse}
 * shape used by plain {@code register}/{@code login}.
 */
public record SupermarketOwnerAuthResponse(
        AuthResponse auth,
        Long supermarketId,
        String supermarketStatus
) {
}
