package com.aislego.growth.web;

import com.aislego.common.security.AuthenticatedUser;
import com.aislego.growth.dto.NotificationsResponse;
import com.aislego.growth.service.UserNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class UserNotificationController {
    private final UserNotificationService service;

    public UserNotificationController(UserNotificationService service) { this.service = service; }

    @GetMapping
    public NotificationsResponse list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return service.list(principal.userId());
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal AuthenticatedUser principal,
                                         @PathVariable Long notificationId) {
        service.markRead(principal.userId(), notificationId);
        return ResponseEntity.noContent().build();
    }
}
