package com.aislego.orders.dto;

import com.aislego.common.money.Money;
import com.aislego.orders.domain.Cart;
import com.aislego.orders.domain.OrderPricing;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        Long supermarketId,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        /** The coupon code currently applied, or null - see {@code CartService#toResponse},
         *  which silently clears this if the coupon has since expired/been deactivated/deleted
         *  rather than surfacing an error on an unrelated cart read. */
        String couponCode,
        BigDecimal discount,
        BigDecimal total,
        String currency
) {
    /** {@code discount} is computed by the caller (via {@code CouponService#calculateDiscount})
     *  rather than here, since resolving/pricing a coupon needs repository access this DTO
     *  deliberately doesn't have. */
    public static CartResponse from(Cart cart, Money discount) {
        List<CartItemResponse> items = cart.getItems().stream().map(CartItemResponse::from).toList();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = items.isEmpty() ? "INR" : items.get(0).currency();
        BigDecimal discountAmount = discount == null ? BigDecimal.ZERO : discount.getAmount();
        BigDecimal deliveryFee = OrderPricing.deliveryFee(currency, !items.isEmpty()).getAmount();
        BigDecimal total = subtotal.subtract(discountAmount).max(BigDecimal.ZERO).add(deliveryFee);
        return new CartResponse(cart.getId(), cart.getSupermarketId(), items, subtotal, deliveryFee,
                cart.getCouponCode(), discountAmount, total, currency);
    }
}
