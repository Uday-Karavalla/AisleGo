package com.aislego.orders.dto;

import com.aislego.orders.domain.Cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        Long supermarketId,
        List<CartItemResponse> items,
        BigDecimal total,
        String currency
) {
    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(CartItemResponse::from).toList();
        BigDecimal total = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = items.isEmpty() ? "INR" : items.get(0).currency();
        return new CartResponse(cart.getId(), cart.getSupermarketId(), items, total, currency);
    }
}
