package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Exactly one of {@code percentOff} / {@code amountOff} is required, chosen by
 * {@code discountType} - validated in {@code CouponService}, not here, since which field is
 * required depends on another field's value (bean validation doesn't express that cleanly for
 * a record).
 */
public record CreateCouponRequest(
        @NotBlank @Size(max = 32) String code,
        @NotNull DiscountType discountType,
        Integer percentOff,
        BigDecimal amountOff,
        String currency,
        Instant expiresAt,
        boolean firstOrderOnly,
        @Min(1) Integer maxRedemptions,
        @Min(1) Integer perUserLimit
) {
    public CreateCouponRequest(String code, DiscountType discountType, Integer percentOff,
                               BigDecimal amountOff, String currency, Instant expiresAt) {
        this(code, discountType, percentOff, amountOff, currency, expiresAt, false, null, null);
    }
}
