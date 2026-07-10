package com.aislego.common.security;

import com.aislego.common.exception.TooManyAttemptsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Covers the actual lockout counting/window-reset logic, using a fixed-then-advanced clock
 *  rather than mocking it away (unlike AuthServiceTest, which mocks this class entirely). */
class LoginRateLimiterTest {

    private static final String EMAIL = "shopper@example.com";

    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void allowsUpToFourFailuresThenLocksOutOnTheFifthAttempt() {
        LoginRateLimiter limiter = new LoginRateLimiter(new MutableClock(Instant.parse("2026-01-01T00:00:00Z")));

        for (int i = 0; i < 4; i++) {
            limiter.checkAllowed(EMAIL);
            limiter.recordFailure(EMAIL);
        }

        // The 5th check should still pass (4 recorded failures < 5 max) ...
        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
        limiter.recordFailure(EMAIL);

        // ... but the 6th is now blocked (5 recorded failures = max).
        assertThatThrownBy(() -> limiter.checkAllowed(EMAIL)).isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void aSuccessfulLoginClearsThePriorFailureCount() {
        LoginRateLimiter limiter = new LoginRateLimiter(new MutableClock(Instant.parse("2026-01-01T00:00:00Z")));

        limiter.recordFailure(EMAIL);
        limiter.recordFailure(EMAIL);
        limiter.recordFailure(EMAIL);
        limiter.recordFailure(EMAIL);
        limiter.recordSuccess(EMAIL);

        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void theLockoutExpiresAfterTheWindowPasses() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        LoginRateLimiter limiter = new LoginRateLimiter(clock);

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(EMAIL);
        }
        assertThatThrownBy(() -> limiter.checkAllowed(EMAIL)).isInstanceOf(TooManyAttemptsException.class);

        clock.advance(Duration.ofMinutes(16));

        assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void differentEmailsAreTrackedIndependently() {
        LoginRateLimiter limiter = new LoginRateLimiter(new MutableClock(Instant.parse("2026-01-01T00:00:00Z")));

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("victim@example.com");
        }
        assertThatThrownBy(() -> limiter.checkAllowed("victim@example.com"))
                .isInstanceOf(TooManyAttemptsException.class);

        assertThatCode(() -> limiter.checkAllowed("someone-else@example.com")).doesNotThrowAnyException();
    }
}
