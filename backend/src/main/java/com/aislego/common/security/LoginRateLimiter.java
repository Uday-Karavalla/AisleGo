package com.aislego.common.security;

import com.aislego.common.exception.TooManyAttemptsException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-email failed-login lockout. AisleGo runs as a single instance (Render free
 * tier), so this doesn't need to be distributed/Redis-backed to be effective - if that ever
 * changes (multiple instances behind a load balancer), this would need to move to shared
 * storage, since each instance would otherwise count attempts independently.
 *
 * <p>Deliberately keyed by email, not IP: the goal is protecting one account from being
 * password-guessed, not rate-limiting a network address (which would also be trivial for an
 * attacker to spread across many IPs, but much harder to spread across "attempts against this
 * one email" without actually knowing more passwords to try).
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginRateLimiter() {
        this(Clock.systemUTC());
    }

    LoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Call before attempting a password check. Throws if this email has already hit the
     *  failed-attempt ceiling within the current window. */
    public void checkAllowed(String email) {
        Window window = windows.computeIfAbsent(normalize(email), key -> new Window(clock.instant()));
        synchronized (window) {
            resetIfWindowExpired(window);
            if (window.count >= MAX_ATTEMPTS) {
                throw new TooManyAttemptsException(
                        "Too many failed login attempts. Please try again in a few minutes.");
            }
        }
    }

    /** Call after a failed password check (wrong password or unknown email). */
    public void recordFailure(String email) {
        Window window = windows.computeIfAbsent(normalize(email), key -> new Window(clock.instant()));
        synchronized (window) {
            resetIfWindowExpired(window);
            window.count++;
        }
    }

    /** Call after a successful login - clears any accumulated failures for this email. */
    public void recordSuccess(String email) {
        windows.remove(normalize(email));
    }

    private void resetIfWindowExpired(Window window) {
        if (clock.instant().isAfter(window.startedAt.plus(LOCKOUT_WINDOW))) {
            window.startedAt = clock.instant();
            window.count = 0;
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private static final class Window {
        private Instant startedAt;
        private int count;

        private Window(Instant startedAt) {
            this.startedAt = startedAt;
        }
    }
}
