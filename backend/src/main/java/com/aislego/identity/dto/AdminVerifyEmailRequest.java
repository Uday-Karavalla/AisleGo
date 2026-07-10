package com.aislego.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Admin-only manual verification: marks any user's email as verified without them ever
 *  entering a code - a stopgap for real customers who can't receive the verification email
 *  yet (Resend's free tier only delivers to the account owner's own address until a custom
 *  domain is verified). */
public record AdminVerifyEmailRequest(
        @NotBlank @Email String email
) {
}
