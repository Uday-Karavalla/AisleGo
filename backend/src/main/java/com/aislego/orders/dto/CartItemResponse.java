package com.aislego.orders.dto;

import com.aislego.orders.domain.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String currency
) {
    public static CartItemResponse from(CartItem item) {
        BigDecimal lineTotal = item.getUnitPrice().getAmount().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice().getAmount(),
                lineTotal,
                item.getUnitPrice().getCurrencyCode()
        );
    }
}
