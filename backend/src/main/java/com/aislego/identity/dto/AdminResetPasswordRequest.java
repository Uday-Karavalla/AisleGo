package com.aislego.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin-only account recovery: resets any user's password by email, no current password
 *  needed - unlike {@code UpdateAccountRequest}'s self-service change, which requires it. */
public record AdminResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}
