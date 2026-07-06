package com.aislego.notifications;

/**
 * Abstraction over "however we tell a person something happened" (order placed, payment
 * confirmed, a supermarket's verification decision, ...). Unlike {@link com.aislego.payments.PaymentService}
 * this is a fire-and-forget side channel, not part of the transaction it's called from: a
 * failed send must never fail the caller's checkout or admin action, so every
 * {@link #send(Notification)} implementation is expected to catch and log its own delivery
 * failures rather than throw.
 *
 * <p>{@link LoggingNotificationService} is the zero-setup default that just logs what would
 * have been sent, for local/demo use. {@link TwilioSmsNotificationService} is the opt-in real
 * SMS provider.
 */
public interface NotificationService {

    void send(Notification notification);
}
