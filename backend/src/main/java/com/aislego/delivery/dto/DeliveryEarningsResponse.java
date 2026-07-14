package com.aislego.delivery.dto;

import java.math.BigDecimal;

public record DeliveryEarningsResponse(BigDecimal today, BigDecimal total, long completedDeliveries, String currency) {
}
