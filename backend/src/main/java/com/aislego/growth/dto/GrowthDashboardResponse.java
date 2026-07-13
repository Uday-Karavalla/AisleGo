package com.aislego.growth.dto;

import java.util.Map;

public record GrowthDashboardResponse(int periodDays, long visitors, long storeViews, long searches,
                                      long addToCarts, long checkouts, long purchases, long couponApplications,
                                      double checkoutConversionPercent, Map<String, Long> dailyPurchases) {
}
