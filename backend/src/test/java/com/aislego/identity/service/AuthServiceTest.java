package com.aislego.identity.service;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.domain.SupermarketStatus;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.BadRequestException;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.common.exception.UnauthorizedException;
import com.aislego.common.exception.TooManyAttemptsException;
import com.aislego.common.security.JwtService;
import com.aislego.common.security.LoginRateLimiter;
import com.aislego.email.EmailService;
import com.aislego.delivery.repository.DeliveryPartnerProfileRepository;
import com.aislego.identity.domain.Role;
import com.aislego.identity.domain.User;
import com.aislego.identity.dto.AdminResetPasswordRequest;
import com.aislego.identity.dto.AdminVerifyEmailRequest;
import com.aislego.identity.dto.LoginRequest;
import com.aislego.identity.dto.RegisterSupermarketOwnerRequest;
import com.aislego.identity.dto.RegisterDeliveryPartnerRequest;
import com.aislego.identity.dto.SupermarketOwnerAuthResponse;
import com.aislego.identity.dto.UpdateAccountRequest;
import com.aislego.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
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
    @Mock
    private EmailService emailService;
    @Mock
    private LoginRateLimiter loginRateLimiter;
    @Mock
    private DeliveryPartnerProfileRepository deliveryPartnerProfileRepository;

    private AuthService authService;

    private static final RegisterSupermarketOwnerRequest REQUEST = new RegisterSupermarketOwnerRequest(
            "owner@example.com", "password123", "Store Owner", "+91-90000-00000",
            "New Store", "A brand new store", "+91-80-2000-0000");

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, supermarketRepository, passwordEncoder, jwtService, emailService,
                loginRateLimiter, deliveryPartnerProfileRepository);
    }

    @Test
    void loginRejectsRightAwayWhenTheRateLimiterHasAlreadyLockedThisEmail() {
        doThrow(new TooManyAttemptsException("Too many failed login attempts. Please try again in a few minutes."))
                .when(loginRateLimiter).checkAllowed("owner@example.com");

        assertThatThrownBy(() -> authService.login(new LoginRequest("owner@example.com", "whatever")))
                .isInstanceOf(TooManyAttemptsException.class);

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void loginRecordsAFailureWhenTheEmailDoesNotExist() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever")))
                .isInstanceOf(UnauthorizedException.class);

        verify(loginRateLimiter).recordFailure("nobody@example.com");
    }

    @Test
    void loginRecordsAFailureWhenThePasswordIsWrong() {
        User user = new User();
        user.setId(1L);
        user.setEmail("owner@example.com");
        user.setPasswordHash("hashed");
        user.setEnabled(true);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("owner@example.com", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class);

        verify(loginRateLimiter).recordFailure("owner@example.com");
    }

    @Test
    void loginClearsTheRateLimiterOnSuccess() {
        User user = new User();
        user.setId(1L);
        user.setEmail("owner@example.com");
        user.setPasswordHash("hashed");
        user.setEnabled(true);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(anyLong(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyLong(), any(), any())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenTtlMillis()).thenReturn(900_000L);

        authService.login(new LoginRequest("owner@example.com", "correct-password"));

        verify(loginRateLimiter).recordSuccess("owner@example.com");
        verify(loginRateLimiter, never()).recordFailure(any());
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

    @Test
    void registerDeliveryPartnerCreatesAnOfflinePartnerProfile() {
        RegisterDeliveryPartnerRequest request = new RegisterDeliveryPartnerRequest(
                "rider@example.com", "password123", "Ravi Rider", "+919000000000");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(50L);
            return user;
        });
        when(jwtService.generateAccessToken(anyLong(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyLong(), any(), any())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenTtlMillis()).thenReturn(900_000L);

        authService.registerDeliveryPartner(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoles()).containsExactly(Role.DELIVERY_PARTNER);
        ArgumentCaptor<com.aislego.delivery.domain.DeliveryPartnerProfile> profileCaptor =
                ArgumentCaptor.forClass(com.aislego.delivery.domain.DeliveryPartnerProfile.class);
        verify(deliveryPartnerProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().isAvailable()).isFalse();
        assertThat(profileCaptor.getValue().getUser()).isEqualTo(userCaptor.getValue());
    }

    @Test
    void registerSupermarketOwnerStillCreatesTheAccountWhenSendingTheVerificationEmailFails() {
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
        org.mockito.Mockito.doThrow(new RuntimeException("Resend rejected the recipient"))
                .when(emailService).sendVerificationCode(any(), any(), any());

        SupermarketOwnerAuthResponse response = authService.registerSupermarketOwner(REQUEST);

        verify(userRepository).save(any(User.class));
        verify(supermarketRepository).save(any(Supermarket.class));
        assertThat(response.auth().accessToken()).isEqualTo("access-token");
    }

    private static final Long USER_ID = 1L;

    private User buildUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("admin@aislego.com");
        user.setPasswordHash("old-hash");
        user.setFullName("AisleGo Admin");
        user.setRoles(Set.of(Role.ADMIN));
        return user;
    }

    @Test
    void updateAccountChangesEmailAndPasswordWhenCurrentPasswordMatches() {
        User user = buildUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(userRepository.existsByEmail("aislego@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("new-password123")).thenReturn("new-hash");
        when(jwtService.generateAccessToken(anyLong(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyLong(), any(), any())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenTtlMillis()).thenReturn(900_000L);

        UpdateAccountRequest request = new UpdateAccountRequest(
                "AisleGo@gmail.com", "old-password", "new-password123");

        authService.updateAccount(USER_ID, request);

        assertThat(user.getEmail()).isEqualTo("aislego@gmail.com");
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
    }

    @Test
    void updateAccountRejectsAnIncorrectCurrentPassword() {
        User user = buildUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        UpdateAccountRequest request = new UpdateAccountRequest("new@example.com", "wrong-password", null);

        assertThatThrownBy(() -> authService.updateAccount(USER_ID, request))
                .isInstanceOf(UnauthorizedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAccountRejectsAnEmailAlreadyTakenBySomeoneElse() {
        User user = buildUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        UpdateAccountRequest request = new UpdateAccountRequest("taken@example.com", "old-password", null);

        assertThatThrownBy(() -> authService.updateAccount(USER_ID, request))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAccountRejectsATooShortNewPassword() {
        User user = buildUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);

        UpdateAccountRequest request = new UpdateAccountRequest("admin@aislego.com", "old-password", "short");

        assertThatThrownBy(() -> authService.updateAccount(USER_ID, request))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateAccountLeavesPasswordUnchangedWhenNewPasswordIsOmitted() {
        User user = buildUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(jwtService.generateAccessToken(anyLong(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyLong(), any(), any())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenTtlMillis()).thenReturn(900_000L);

        UpdateAccountRequest request = new UpdateAccountRequest("admin@aislego.com", "old-password", null);

        authService.updateAccount(USER_ID, request);

        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void adminResetPasswordChangesTheTargetUsersPasswordHashWithoutNeedingTheOldOne() {
        User user = buildUser();
        when(userRepository.findByEmail("admin@aislego.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("brand-new-password")).thenReturn("brand-new-hash");

        authService.adminResetPassword(new AdminResetPasswordRequest("admin@aislego.com", "brand-new-password"));

        assertThat(user.getPasswordHash()).isEqualTo("brand-new-hash");
        verify(userRepository).save(user);
    }

    @Test
    void adminResetPasswordThrowsNotFoundForAnUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.adminResetPassword(
                new AdminResetPasswordRequest("nobody@example.com", "brand-new-password")))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void adminVerifyEmailMarksTheTargetUserVerifiedAndClearsTheirPendingCode() {
        User user = buildUser();
        user.setEmailVerified(false);
        user.setVerificationCode("123456");
        when(userRepository.findByEmail("admin@aislego.com")).thenReturn(Optional.of(user));

        authService.adminVerifyEmail(new AdminVerifyEmailRequest("admin@aislego.com"));

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationCode()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void adminVerifyEmailThrowsNotFoundForAnUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.adminVerifyEmail(new AdminVerifyEmailRequest("nobody@example.com")))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository, never()).save(any());
    }
}
