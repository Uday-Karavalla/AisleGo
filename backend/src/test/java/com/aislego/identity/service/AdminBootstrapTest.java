package com.aislego.identity.service;

import com.aislego.identity.domain.Role;
import com.aislego.identity.domain.User;
import com.aislego.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminBootstrap bootstrap = new AdminBootstrap(userRepository);

    @Test
    void promotesAndVerifiesAnExistingAccount() throws Exception {
        User user = new User();
        user.setEmail("owner@example.com");
        user.setEnabled(false);
        user.setEmailVerified(false);
        user.setVerificationCode("123456");
        user.setVerificationCodeExpiresAt(Instant.now());
        user.setRoles(new HashSet<>());
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        ReflectionTestUtils.setField(bootstrap, "configuredEmail", " OWNER@example.com ");

        bootstrap.run(new DefaultApplicationArguments());

        assertThat(user.getRoles()).contains(Role.ADMIN);
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationCode()).isNull();
        assertThat(user.getVerificationCodeExpiresAt()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void doesNotWriteWhenAccountIsAlreadyAnActiveAdmin() throws Exception {
        User user = new User();
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setRoles(new HashSet<>(Set.of(Role.ADMIN)));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        ReflectionTestUtils.setField(bootstrap, "configuredEmail", "admin@example.com");

        bootstrap.run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(user);
    }

    @Test
    void refusesToStartWithoutAnEmail() {
        ReflectionTestUtils.setField(bootstrap, "configuredEmail", " ");

        assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_BOOTSTRAP_EMAIL");
    }

    @Test
    void refusesToPromoteAnAccountThatDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(bootstrap, "configuredEmail", "missing@example.com");

        assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not exist");
    }
}
