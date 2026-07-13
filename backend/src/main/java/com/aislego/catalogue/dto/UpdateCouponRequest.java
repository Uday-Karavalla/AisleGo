package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.DiscountType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/** The code itself isn't editable - create a new coupon instead - only the discount terms,
 *  expiry and active flag can change. */
public record UpdateCouponRequest(
        @NotNull DiscountType discountType,
        Integer percentOff,
        BigDecimal amountOff,
        String currency,
        Instant expiresAt,
        boolean active
) {
}
