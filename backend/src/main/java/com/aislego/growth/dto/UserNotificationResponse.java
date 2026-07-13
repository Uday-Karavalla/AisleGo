package com.aislego.growth.dto;

import com.aislego.growth.domain.UserNotification;
import java.time.Instant;

public record UserNotificationResponse(Long id, String title, String message, String actionUrl,
                                       boolean read, Instant createdAt) {
    public static UserNotificationResponse from(UserNotification value) {
        return new UserNotificationResponse(value.getId(), value.getTitle(), value.getMessage(),
                value.getActionUrl(), value.getReadAt() != null, value.getCreatedAt());
    }
}
