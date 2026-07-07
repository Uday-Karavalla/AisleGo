package com.aislego.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Real email delivery via SMTP (e.g. Gmail with an app password) using Spring Boot's
 * auto-configured {@link JavaMailSender} - opt-in via {@code aislego.email.provider=smtp};
 * {@link LoggingEmailService} remains the zero-setup default. SMTP connection details
 * (host/port/username/password) are the standard {@code spring.mail.*} properties, not
 * reinvented here.
 *
 * <p>Unlike {@code TwilioSmsNotificationService}, a failure here is NOT swallowed - the
 * caller (registration) needs to know the code genuinely didn't reach the user.
 */
@Service
@ConditionalOnProperty(name = "aislego.email.provider", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public SmtpEmailService(JavaMailSender mailSender,
                             @Value("${aislego.email.from-address}") String fromAddress,
                             @Value("${aislego.email.from-name}") String fromName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void sendVerificationCode(String toEmail, String toName, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(String.format("%s <%s>", fromName, fromAddress));
        message.setTo(toEmail);
        message.setSubject("Verify your AisleGo account");
        message.setText("Hi " + toName + ",\n\n"
                + "Your AisleGo verification code is: " + code + "\n\n"
                + "This code expires in 24 hours. If you didn't request this, you can ignore this email.");
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send verification email to {}", toEmail, ex);
            throw ex;
        }
    }
}
