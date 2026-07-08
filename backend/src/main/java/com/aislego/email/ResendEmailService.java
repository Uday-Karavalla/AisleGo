package com.aislego.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Real email delivery via Resend's HTTP API (a plain HTTPS POST, like
 * {@code TwilioSmsNotificationService} uses for Twilio - no SDK dependency) - opt-in via
 * {@code aislego.email.provider=resend}; {@link LoggingEmailService} remains the zero-setup
 * default.
 *
 * <p>Deliberately not SMTP: testing {@link SmtpEmailService} against Render's free tier found
 * outbound SMTP is blocked at the network level (a common PaaS restriction to prevent spam
 * abuse) - the connection either hangs or fails outright regardless of credentials. An HTTPS
 * API call goes out over port 443, the one port every PaaS host reliably allows, sidestepping
 * that restriction entirely.
 */
@Service
@ConditionalOnProperty(name = "aislego.email.provider", havingValue = "resend")
public class ResendEmailService implements EmailService {

    private final RestClient restClient;
    private final String fromAddress;
    private final String fromName;

    @Autowired
    public ResendEmailService(@Value("${aislego.email.resend.api-key}") String apiKey,
                               @Value("${aislego.email.from-address}") String fromAddress,
                               @Value("${aislego.email.from-name}") String fromName) {
        this(fromAddress, fromName, RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build());
    }

    /** Package-private constructor so tests can inject a {@link RestClient} pointed at a fake
     *  server instead of the real Resend API. */
    ResendEmailService(String fromAddress, String fromName, RestClient restClient) {
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.restClient = restClient;
    }

    @Override
    public void sendVerificationCode(String toEmail, String toName, String code) {
        String text = "Hi " + toName + ",\n\n"
                + "Your AisleGo verification code is: " + code + "\n\n"
                + "This code expires in 24 hours. If you didn't request this, you can ignore this email.";
        callSendEmailApi(toEmail, text);
    }

    /**
     * Package-private seam for tests: calls Resend's Emails API. Overridable so tests can
     * assert on the exact request without mocking {@link RestClient}'s fluent chain, same
     * pattern as {@code TwilioSmsNotificationService#callSendMessageApi}.
     */
    void callSendEmailApi(String toEmail, String text) {
        Map<String, Object> body = Map.of(
                "from", fromName + " <" + fromAddress + ">",
                "to", List.of(toEmail),
                "subject", "Verify your AisleGo account",
                "text", text
        );

        restClient.post()
                .uri("/emails")
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
