package com.aislego.catalogue.dto;

import com.aislego.catalogue.domain.Coupon;
import com.aislego.catalogue.domain.DiscountType;
import com.aislego.common.money.Money;

import java.math.BigDecimal;
import java.time.Instant;

/** Shopper-safe coupon terms plus the discount this cart would receive right now. */
public record AvailableCouponResponse(
        String code,
        DiscountType discountType,
        Integer percentOff,
        BigDecimal amountOff,
        String currency,
        Instant expiresAt,
        String scope,
        BigDecimal estimatedDiscount
) {
    public static AvailableCouponResponse from(Coupon coupon, Money estimatedDiscount) {
        return new AvailableCouponResponse(
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getPercentOff(),
                coupon.getAmountOff() == null ? null : coupon.getAmountOff().getAmount(),
                coupon.getAmountOff() == null ? null : coupon.getAmountOff().getCurrencyCode(),
                coupon.getExpiresAt(),
                coupon.getSupermarket() == null ? "PLATFORM" : "STORE",
                estimatedDiscount.getAmount()
        );
    }
}
