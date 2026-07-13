package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.Coupon;
import com.aislego.catalogue.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
        Long id,
        String code,
        /** Null = platform-wide, applies at any store. */
        Long supermarketId,
        DiscountType discountType,
        Integer percentOff,
        BigDecimal amountOff,
        String currency,
        Instant expiresAt,
        boolean active,
        boolean firstOrderOnly,
        Integer maxRedemptions,
        Integer perUserLimit
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getSupermarket() != null ? coupon.getSupermarket().getId() : null,
                coupon.getDiscountType(),
                coupon.getPercentOff(),
                coupon.getAmountOff() != null ? coupon.getAmountOff().getAmount() : null,
                coupon.getAmountOff() != null ? coupon.getAmountOff().getCurrencyCode() : null,
                coupon.getExpiresAt(),
                coupon.isActive(),
                coupon.isFirstOrderOnly(),
                coupon.getMaxRedemptions(),
                coupon.getPerUserLimit()
        );
    }
}
