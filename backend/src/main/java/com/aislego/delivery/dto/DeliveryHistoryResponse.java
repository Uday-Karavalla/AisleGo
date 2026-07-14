package com.aislego.delivery.dto;

import com.aislego.orders.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;

public record DeliveryHistoryResponse(Long orderId, String supermarketName, String branchName,
                                      BigDecimal earning, String currency, Instant deliveredAt) {
    public static DeliveryHistoryResponse from(Order order) {
        return new DeliveryHistoryResponse(order.getId(), order.getSupermarket().getName(),
                order.getBranch().getName(), order.getDeliveryFee(), order.getTotalAmount().getCurrencyCode(),
                order.getUpdatedAt());
    }
}
