package com.aislego.orders.domain;

import com.aislego.common.money.Money;

import java.math.BigDecimal;

/** Shared pricing rules used by both cart previews and checkout. */
public final class OrderPricing {

    public static final BigDecimal DELIVERY_FEE_AMOUNT = BigDecimal.valueOf(25);

    private OrderPricing() {
    }

    public static Money deliveryFee(String currency, boolean hasItems) {
        return deliveryFee(currency, hasItems, FulfilmentType.IMMEDIATE);
    }

    public static Money deliveryFee(String currency, boolean hasItems, FulfilmentType fulfilmentType) {
        boolean chargeDelivery = hasItems && fulfilmentType != FulfilmentType.PICKUP;
        return Money.of(chargeDelivery ? DELIVERY_FEE_AMOUNT : BigDecimal.ZERO, currency);
    }
}
