package com.aislego.orders.dto;

import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.domain.FulfilmentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long supermarketId,
        Long branchId,
        OrderStatus status,
        FulfilmentType fulfilmentType,
        Instant scheduledFor,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        String currency,
        String couponCode,
        BigDecimal discountAmount,
        List<OrderItemResponse> items,
        String deliveryAddress,
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getSupermarket().getId(),
                order.getBranch().getId(),
                order.getStatus(),
                order.getFulfilmentType(),
                order.getScheduledFor(),
                order.getTotalAmount().getAmount().subtract(order.getDeliveryFee()).add(order.getDiscountAmount()),
                order.getDeliveryFee(),
                order.getTotalAmount().getAmount(),
                order.getTotalAmount().getCurrencyCode(),
                order.getCouponCode(),
                order.getDiscountAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getDeliveryAddress(),
                order.getCreatedAt()
        );
    }
}
