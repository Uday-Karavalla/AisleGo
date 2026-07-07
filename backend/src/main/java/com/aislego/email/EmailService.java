package com.aislego.email;

/**
 * Sends the one transactional email this app needs today: an account's verification code.
 * Deliberately narrow (not a generic "send any email" abstraction) - if a second kind of
 * email is ever needed, widen this then, not speculatively now.
 *
 * <p>Two implementations, same mock-default / opt-in-real-provider split already used for
 * payments/routing/notifications: {@link LoggingEmailService} is the zero-setup default,
 * {@link SmtpEmailService} is opt-in via {@code aislego.email.provider=smtp}.
 */
public interface EmailService {
    void sendVerificationCode(String toEmail, String toName, String code);
}
