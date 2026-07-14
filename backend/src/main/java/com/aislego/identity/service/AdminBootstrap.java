package com.aislego.identity.service;

import com.aislego.identity.domain.Role;
import com.aislego.identity.domain.User;
import com.aislego.identity.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * One-time production bootstrap for an account registered through the normal flow.
 * Credentials never pass through configuration: the operator supplies only the email,
 * enables this runner for one deployment, then removes both variables.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aislego.admin-bootstrap", name = "enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;

    @Value("${aislego.admin-bootstrap.email:}")
    private String configuredEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = configuredEmail == null ? "" : configuredEmail.trim().toLowerCase(Locale.ROOT);
        if (email.isBlank()) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_EMAIL is required when admin bootstrap is enabled");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Admin bootstrap account does not exist; register it before enabling bootstrap"));

        boolean changed = user.getRoles().add(Role.ADMIN);
        if (!user.isEnabled()) {
            user.setEnabled(true);
            changed = true;
        }
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
            log.info("Admin bootstrap completed for the configured account");
        } else {
            log.info("Configured admin account is already active");
        }
    }
}
