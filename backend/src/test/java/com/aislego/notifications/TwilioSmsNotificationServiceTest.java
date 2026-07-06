package com.aislego.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mirrors {@code OpenRouteServiceRoutingServiceTest}'s approach: exercises the package-private
 * {@code callSendMessageApi} seam with a throwaway subclass instead of mocking Spring's
 * {@link RestClient} fluent builder chain directly.
 */
class TwilioSmsNotificationServiceTest {

    private static final RestClient UNUSED_REST_CLIENT = RestClient.create();

    @Test
    void sendCallsTwilioWithTheRecipientPhoneAndComposedBody() {
        AtomicReference<String> capturedTo = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        TwilioSmsNotificationService service = new TwilioSmsNotificationService("AC123", "+15550001111",
                UNUSED_REST_CLIENT) {
            @Override
            void callSendMessageApi(String to, String body) {
                capturedTo.set(to);
                capturedBody.set(body);
            }
        };

        service.send(new Notification("Jane", "jane@example.com", "+15559998888",
                "Order placed", "Your order #1 has been placed."));

        assertThat(capturedTo.get()).isEqualTo("+15559998888");
        assertThat(capturedBody.get()).isEqualTo("Order placed: Your order #1 has been placed.");
    }

    @Test
    void sendSkipsWithoutCallingTwilioWhenThereIsNoPhoneNumber() {
        AtomicReference<String> called = new AtomicReference<>();
        TwilioSmsNotificationService service = new TwilioSmsNotificationService("AC123", "+15550001111",
                UNUSED_REST_CLIENT) {
            @Override
            void callSendMessageApi(String to, String body) {
                called.set(to);
            }
        };

        service.send(new Notification("Jane", "jane@example.com", null, "Order placed", "Your order was placed."));
        service.send(new Notification("Jane", "jane@example.com", "  ", "Order placed", "Your order was placed."));

        assertThat(called.get()).isNull();
    }

    @Test
    void sendSwallowsAnyFailureFromTwilio() {
        TwilioSmsNotificationService service = new TwilioSmsNotificationService("AC123", "+15550001111",
                UNUSED_REST_CLIENT) {
            @Override
            void callSendMessageApi(String to, String body) {
                throw new RuntimeException("simulated Twilio outage");
            }
        };

        service.send(new Notification("Jane", "jane@example.com", "+15559998888", "Order placed", "message"));
    }
}
