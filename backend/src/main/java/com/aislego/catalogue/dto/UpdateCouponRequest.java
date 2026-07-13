package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.DiscountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

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
        boolean active,
        boolean firstOrderOnly,
        @Min(1) Integer maxRedemptions,
        @Min(1) Integer perUserLimit
) {
    public UpdateCouponRequest(DiscountType discountType, Integer percentOff, BigDecimal amountOff,
                               String currency, Instant expiresAt, boolean active) {
        this(discountType, percentOff, amountOff, currency, expiresAt, active, false, null, null);
    }
}
