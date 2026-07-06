package com.aislego.orders.dto;

import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** Platform-wide order view for the admin oversight dashboard - unlike {@link OrderResponse},
 *  which is scoped to "my own orders," this exposes who placed the order and at which store. */
public record AdminOrderResponse(
        Long id,
        String customerName,
        String customerEmail,
        String supermarketName,
        String branchName,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        String deliveryAddress,
        Instant createdAt
) {
    public static AdminOrderResponse from(Order order) {
        return new AdminOrderResponse(
                order.getId(),
                order.getUser().getFullName(),
                order.getUser().getEmail(),
                order.getSupermarket().getName(),
                order.getBranch().getName(),
                order.getStatus(),
                order.getTotalAmount().getAmount(),
                order.getTotalAmount().getCurrencyCode(),
                order.getDeliveryAddress(),
                order.getCreatedAt()
        );
    }
}
