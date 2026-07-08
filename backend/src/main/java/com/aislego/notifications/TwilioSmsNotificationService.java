package com.aislego.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Real SMS delivery via Twilio's REST API. Opt-in via {@code aislego.notifications.provider=twilio};
 * {@link LoggingNotificationService} remains the default so the demo works with zero setup.
 *
 * <p>Uses Spring's {@link RestClient} directly against Twilio's Messages resource (HTTP Basic
 * Auth with the Account SID/Auth Token) rather than adding the Twilio SDK, mirroring
 * {@link com.aislego.catalogue.routing.OpenRouteServiceRoutingService}'s "no new HTTP client
 * dependency for one small integration" choice.
 *
 * <p>{@link #send} never throws: a recipient with no phone number is skipped, and any
 * network/API failure is caught and logged, per {@link NotificationService}'s fire-and-forget
 * contract - a notification failure must never fail the caller's checkout or admin action.
 */
@Service
@ConditionalOnProperty(name = "aislego.notifications.provider", havingValue = "twilio")
public class TwilioSmsNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsNotificationService.class);
    private static final String BASE_URL = "https://api.twilio.com/2010-04-01";

    private final RestClient restClient;
    private final String accountSid;
    private final String fromNumber;

    @Autowired
    public TwilioSmsNotificationService(@Value("${aislego.notifications.twilio.account-sid}") String accountSid,
                                         @Value("${aislego.notifications.twilio.auth-token}") String authToken,
                                         @Value("${aislego.notifications.twilio.from-number}") String fromNumber) {
        this(accountSid, fromNumber, RestClient.builder()
                .baseUrl(BASE_URL)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBasicAuth(accountSid, authToken);
                    return execution.execute(request, body);
                })
                .build());
    }

    /**
     * Package-private constructor so tests can inject a {@link RestClient} pointed at a fake
     * server instead of the real Twilio API.
     */
    TwilioSmsNotificationService(String accountSid, String fromNumber, RestClient restClient) {
        this.accountSid = accountSid;
        this.fromNumber = fromNumber;
        this.restClient = restClient;
    }

    @Override
    public void send(Notification notification) {
        String to = notification.recipientPhone();
        if (!StringUtils.hasText(to)) {
            log.debug("Skipping SMS to {}: no phone number on file", notification.recipientName());
            return;
        }
        try {
            callSendMessageApi(to, notification.subject() + ": " + notification.message());
        } catch (Exception ex) {
            log.warn("Failed to send SMS to {}: {}", to, ex.getMessage());
        }
    }

    /**
     * Package-private seam for tests: calls Twilio's Messages API. Overridable so tests can
     * simulate a Twilio failure without standing up a fake HTTP server.
     */
    void callSendMessageApi(String to, String body) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", to);
        form.add("From", fromNumber);
        form.add("Body", body);

        restClient.post()
                .uri("/Accounts/{accountSid}/Messages.json", accountSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
