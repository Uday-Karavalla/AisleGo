package com.aislego.identity.service;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.domain.SupermarketStatus;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.security.JwtService;
import com.aislego.identity.domain.Role;
import com.aislego.identity.domain.User;
import com.aislego.identity.dto.RegisterSupermarketOwnerRequest;
import com.aislego.identity.dto.SupermarketOwnerAuthResponse;
import com.aislego.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SupermarketRepository supermarketRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private static final RegisterSupermarketOwnerRequest REQUEST = new RegisterSupermarketOwnerRequest(
            "owner@example.com", "password123", "Store Owner", "+91-90000-00000",
            "New Store", "A brand new store", "+91-80-2000-0000");

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, supermarketRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerSupermarketOwnerCreatesAPendingSupermarketOwnedByTheNewUser() {
        when(userRepository.existsByEmail(REQUEST.email())).thenReturn(false);
        when(passwordEncoder.encode(REQUEST.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(supermarketRepository.save(any(Supermarket.class))).thenAnswer(invocation -> {
            Supermarket supermarket = invocation.getArgument(0);
            supermarket.setId(7L);
            return supermarket;
        });
        when(jwtService.generateAccessToken(anyLong(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyLong(), any(), any())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenTtlMillis()).thenReturn(900_000L);

        SupermarketOwnerAuthResponse response = authService.registerSupermarketOwner(REQUEST);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("owner@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(savedUser.getRoles()).containsExactly(Role.SUPERMARKET_OWNER);

        ArgumentCaptor<Supermarket> supermarketCaptor = ArgumentCaptor.forClass(Supermarket.class);
        verify(supermarketRepository).save(supermarketCaptor.capture());
        Supermarket savedSupermarket = supermarketCaptor.getValue();
        assertThat(savedSupermarket.getName()).isEqualTo("New Store");
        assertThat(savedSupermarket.getStatus()).isEqualTo(SupermarketStatus.PENDING);
        assertThat(savedSupermarket.isActive()).isTrue();
        assertThat(savedSupermarket.getOwner()).isEqualTo(savedUser);

        assertThat(response.supermarketId()).isEqualTo(7L);
        assertThat(response.supermarketStatus()).isEqualTo("PENDING");
        assertThat(response.auth().accessToken()).isEqualTo("access-token");
        assertThat(response.auth().refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void registerSupermarketOwnerRejectsADuplicateEmailWithoutCreatingAnything() {
        when(userRepository.existsByEmail(REQUEST.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.registerSupermarketOwner(REQUEST))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
        verify(supermarketRepository, never()).save(any());
    }
}
