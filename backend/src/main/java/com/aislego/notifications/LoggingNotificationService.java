package com.aislego.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Zero-setup stand-in for a real SMS/push/email provider: logs what would have been sent
 * instead of actually sending it, matching the "always-registered default" role
 * {@link com.aislego.payments.MockPaymentGateway} plays for payments.
 */
@Service
public class LoggingNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void send(Notification notification) {
        log.info("Notification to {} <{}, {}>: [{}] {}", notification.recipientName(),
                notification.recipientEmail(), notification.recipientPhone(), notification.subject(),
                notification.message());
    }
}
