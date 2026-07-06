package com.aislego.orders.dto;

import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** A supermarket owner's view of one of their store's orders - includes the customer's
 *  contact details (needed to hand over or coordinate delivery) and the item list (needed
 *  to pick and pack), unlike the platform-admin's {@code AdminOrderResponse}. */
public record OwnerOrderResponse(
        Long id,
        String customerName,
        String customerPhone,
        Long branchId,
        String branchName,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        List<OrderItemResponse> items,
        String deliveryAddress,
        Instant createdAt
) {
    public static OwnerOrderResponse from(Order order) {
        return new OwnerOrderResponse(
                order.getId(),
                order.getUser().getFullName(),
                order.getUser().getPhone(),
                order.getBranch().getId(),
                order.getBranch().getName(),
                order.getStatus(),
                order.getTotalAmount().getAmount(),
                order.getTotalAmount().getCurrencyCode(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getDeliveryAddress(),
                order.getCreatedAt()
        );
    }
}
