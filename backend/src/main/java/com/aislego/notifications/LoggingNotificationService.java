package com.aislego.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Zero-setup stand-in for a real SMS/push/email provider: logs what would have been sent
 * instead of actually sending it, matching the "always-registered default" role
 * {@link com.aislego.payments.MockPaymentGateway} plays for payments.
 *
 * <p>Explicitly mutually exclusive with {@link TwilioSmsNotificationService} via the same
 * property (matching "mock" or unset) - without this, setting
 * {@code aislego.notifications.provider=twilio} would leave *both* beans active and crash the
 * app at startup with a {@code NoUniqueBeanDefinitionException}.
 */
@Service
@ConditionalOnProperty(name = "aislego.notifications.provider", havingValue = "mock", matchIfMissing = true)
public class LoggingNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void send(Notification notification) {
        log.info("Notification to {} <{}, {}>: [{}] {}", notification.recipientName(),
                notification.recipientEmail(), notification.recipientPhone(), notification.subject(),
                notification.message());
    }
}
