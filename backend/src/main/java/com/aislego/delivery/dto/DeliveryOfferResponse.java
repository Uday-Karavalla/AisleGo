package com.aislego.delivery.dto;

import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.FulfilmentType;
import com.aislego.orders.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record DeliveryOfferResponse(
        Long orderId,
        OrderStatus status,
        String supermarketName,
        String branchName,
        String pickupAddress,
        String deliveryAddress,
        FulfilmentType fulfilmentType,
        Instant scheduledFor,
        int itemCount,
        BigDecimal orderTotal,
        String currency
) {
    public static DeliveryOfferResponse from(Order order) {
        String pickup = order.getBranch().getAddressLine();
        if (order.getBranch().getCity() != null && !order.getBranch().getCity().isBlank()) {
            pickup = (pickup == null || pickup.isBlank()) ? order.getBranch().getCity()
                    : pickup + ", " + order.getBranch().getCity();
        }
        int itemCount = order.getItems().stream().mapToInt(item -> item.getQuantity()).sum();
        return new DeliveryOfferResponse(order.getId(), order.getStatus(), order.getSupermarket().getName(),
                order.getBranch().getName(), pickup, order.getDeliveryAddress(), order.getFulfilmentType(),
                order.getScheduledFor(), itemCount, order.getTotalAmount().getAmount(),
                order.getTotalAmount().getCurrencyCode());
    }
}
