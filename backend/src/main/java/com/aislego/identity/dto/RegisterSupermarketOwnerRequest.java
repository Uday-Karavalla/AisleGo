package com.aislego.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterSupermarketOwnerRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank String fullName,
        String phone,
        @NotBlank String supermarketName,
        String supermarketDescription,
        String supermarketPhone
) {
}
