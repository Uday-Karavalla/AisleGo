package com.aislego.delivery.dto;

import jakarta.validation.constraints.Pattern;

public record VerifyHandoffOtpRequest(
        @Pattern(regexp = "^[0-9]{6}$", message = "Enter the 6-digit code") String code
) {
}
