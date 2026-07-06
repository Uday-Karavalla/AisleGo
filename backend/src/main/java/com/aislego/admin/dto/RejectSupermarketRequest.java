package com.aislego.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectSupermarketRequest(@NotBlank String reason) {
}
