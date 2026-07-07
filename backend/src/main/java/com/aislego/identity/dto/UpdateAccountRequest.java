package com.aislego.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Self-service "change my email/password." {@code currentPassword} must match before
 *  anything changes - a valid JWT alone shouldn't be enough to permanently take over an
 *  account's credentials. {@code newPassword} is optional; omit it to change only the
 *  email. */
public record UpdateAccountRequest(
        @NotBlank @Email String email,
        @NotBlank String currentPassword,
        String newPassword
) {
}
