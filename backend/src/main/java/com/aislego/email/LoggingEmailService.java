package com.aislego.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Zero-setup default: logs the verification code instead of emailing it, same spirit as
 * {@code MockPaymentGateway} and {@code LoggingNotificationService}. Fine for local/demo use
 * where nobody's inbox needs to actually receive anything.
 */
@Service
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendVerificationCode(String toEmail, String toName, String code) {
        log.info("Verification code for {} <{}>: {}", toName, toEmail, code);
    }
}
