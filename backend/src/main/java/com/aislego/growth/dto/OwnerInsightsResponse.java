package com.aislego.growth.dto;

import java.math.BigDecimal;
import java.util.List;

public record OwnerInsightsResponse(long ordersLast30Days, BigDecimal revenueLast30Days,
                                    BigDecimal averageOrderValue, long lowStockItems,
                                    List<TopProduct> topProducts) {
    public record TopProduct(String name, long unitsSold) {}
}
