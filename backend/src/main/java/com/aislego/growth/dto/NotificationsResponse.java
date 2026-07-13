package com.aislego.growth.dto;

import java.util.List;

public record NotificationsResponse(long unreadCount, List<UserNotificationResponse> notifications) {
}
