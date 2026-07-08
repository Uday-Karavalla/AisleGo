package com.aislego.email;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mirrors {@code TwilioSmsNotificationServiceTest}'s approach: exercises the package-private
 * {@code callSendEmailApi} seam with a throwaway subclass instead of mocking Spring's
 * {@link RestClient} fluent builder chain directly.
 */
class ResendEmailServiceTest {

    private static final RestClient UNUSED_REST_CLIENT = RestClient.create();

    @Test
    void sendVerificationCodeComposesTheExpectedRecipientAndBody() {
        AtomicReference<String> capturedTo = new AtomicReference<>();
        AtomicReference<String> capturedText = new AtomicReference<>();
        ResendEmailService service = new ResendEmailService("aislego@example.com", "AisleGo", UNUSED_REST_CLIENT) {
            @Override
            void callSendEmailApi(String toEmail, String text) {
                capturedTo.set(toEmail);
                capturedText.set(text);
            }
        };

        service.sendVerificationCode("jane@example.com", "Jane", "483920");

        assertThat(capturedTo.get()).isEqualTo("jane@example.com");
        assertThat(capturedText.get()).contains("Jane").contains("483920");
    }

    @Test
    void sendVerificationCodePropagatesAFailureInsteadOfSwallowingIt() {
        ResendEmailService service = new ResendEmailService("aislego@example.com", "AisleGo", UNUSED_REST_CLIENT) {
            @Override
            void callSendEmailApi(String toEmail, String text) {
                throw new RuntimeException("simulated Resend outage");
            }
        };

        assertThatThrownBy(() -> service.sendVerificationCode("jane@example.com", "Jane", "483920"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("simulated Resend outage");
    }
}
