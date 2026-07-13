package com.aislego.growth.service;

import com.aislego.common.exception.NotFoundException;
import com.aislego.growth.domain.UserNotification;
import com.aislego.growth.dto.NotificationsResponse;
import com.aislego.growth.dto.UserNotificationResponse;
import com.aislego.growth.repository.UserNotificationRepository;
import com.aislego.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;

@Service
public class UserNotificationService {
    private final UserNotificationRepository repository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbc;

    public UserNotificationService(UserNotificationRepository repository, UserRepository userRepository,
                                   JdbcTemplate jdbc) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.jdbc = jdbc;
    }

    @Transactional
    public void create(Long userId, String title, String message, String actionUrl) {
        UserNotification notification = new UserNotification();
        notification.setUser(userRepository.getReferenceById(userId));
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setActionUrl(actionUrl);
        notification.setCreatedAt(Instant.now());
        repository.save(notification);
    }

    @Transactional(readOnly = true)
    public NotificationsResponse list(Long userId) {
        return new NotificationsResponse(repository.countByUserIdAndReadAtIsNull(userId),
                repository.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(UserNotificationResponse::from).toList());
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        UserNotification notification = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification was not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            repository.save(notification);
        }
    }

    /** Fan-out for high-value, user-requested alerts only (a saved item dropping in price or
     * returning to stock), not general promotional spam. */
    @Transactional
    public void notifyProductFollowers(Long productId, String title, String message, String actionUrl) {
        jdbc.update("insert into user_notifications(user_id, title, message, action_url, created_at) " +
                        "select user_id, ?, ?, ?, ? from favorite_products where product_id = ?",
                title, message, actionUrl, Timestamp.from(Instant.now()), productId);
    }
}
