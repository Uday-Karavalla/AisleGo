package com.aislego.delivery.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectDeliveryPartnerRequest(@NotBlank String reason) {
}
