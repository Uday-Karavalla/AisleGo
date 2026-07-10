package com.aislego.identity.service;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.domain.SupermarketStatus;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.BadRequestException;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.common.exception.UnauthorizedException;
import com.aislego.common.security.JwtService;
import com.aislego.common.security.LoginRateLimiter;
import com.aislego.email.EmailService;
import com.aislego.identity.domain.Role;
import com.aislego.identity.domain.User;
import com.aislego.identity.dto.AdminResetPasswordRequest;
import com.aislego.identity.dto.AdminVerifyEmailRequest;
import com.aislego.identity.dto.AuthResponse;
import com.aislego.identity.dto.LoginRequest;
import com.aislego.identity.dto.MeResponse;
import com.aislego.identity.dto.RegisterRequest;
import com.aislego.identity.dto.RegisterSupermarketOwnerRequest;
import com.aislego.identity.dto.SupermarketOwnerAuthResponse;
import com.aislego.identity.dto.UpdateAccountRequest;
import com.aislego.identity.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int VERIFICATION_CODE_TTL_HOURS = 24;

    private final UserRepository userRepository;
    private final SupermarketRepository supermarketRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(UserRepository userRepository, SupermarketRepository supermarketRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService,
                        LoginRateLimiter loginRateLimiter) {
        this.userRepository = userRepository;
        this.supermarketRepository = supermarketRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("EMAIL_TAKEN", "An account with this email already exists");
        }

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setRoles(Set.of(Role.CUSTOMER));
        issueAndSendVerificationCode(user);
        user = userRepository.save(user);

        return issueTokens(user);
    }

    /**
     * Self-registers a supermarket owner and their (initially {@code PENDING}) supermarket in
     * one step, reusing the same email-uniqueness rule as {@link #register}. The owner is
     * logged in immediately, consistent with customer registration UX, even though their
     * supermarket won't appear in discovery until an admin verifies it - see
     * {@code SupermarketVerificationService}.
     */
    @Transactional
    public SupermarketOwnerAuthResponse registerSupermarketOwner(RegisterSupermarketOwnerRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("EMAIL_TAKEN", "An account with this email already exists");
        }

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setRoles(Set.of(Role.SUPERMARKET_OWNER));
        issueAndSendVerificationCode(user);
        user = userRepository.save(user);

        Supermarket supermarket = new Supermarket();
        supermarket.setName(request.supermarketName());
        supermarket.setDescription(request.supermarketDescription());
        supermarket.setPhone(request.supermarketPhone());
        supermarket.setActive(true);
        supermarket.setStatus(SupermarketStatus.PENDING);
        supermarket.setOwner(user);
        supermarket = supermarketRepository.save(supermarket);

        return new SupermarketOwnerAuthResponse(issueTokens(user), supermarket.getId(), supermarket.getStatus().name());
    }

    /**
     * Backs {@code GET /api/auth/me} with a fresh DB read rather than trusting the JWT's
     * baked-in claims - {@code emailVerified} can change (via {@link #verifyEmail}) without a
     * new token being issued, so the JWT alone would go stale.
     */
    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        return new MeResponse(user.getId(), user.getEmail(), roles, user.isEmailVerified());
    }

    /**
     * Admin-only account recovery: resets any user's password by email with no current
     * password required, unlike {@link #updateAccount}'s self-service change. For recovering
     * access to an account whose password was lost, not for routine credential changes.
     */
    @Transactional
    public void adminResetPassword(AdminResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new NotFoundException("No account found with this email"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /**
     * Admin-only manual verification: marks a user's email verified without them ever entering
     * a code. A stopgap for real customers/owners who can't receive the actual verification
     * email yet (Resend's free tier only delivers to the account this Resend account was
     * signed up with, until a custom domain is verified) - lets the business keep operating for
     * everyone else in the meantime rather than leaving every non-owner account stuck unverified.
     */
    @Transactional
    public void adminVerifyEmail(AdminVerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new NotFoundException("No account found with this email"));
        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();
        loginRateLimiter.checkAllowed(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    loginRateLimiter.recordFailure(email);
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!user.isEnabled()) {
            throw new UnauthorizedException("This account has been disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginRateLimiter.recordFailure(email);
            throw new UnauthorizedException("Invalid email or password");
        }

        loginRateLimiter.recordSuccess(email);
        return issueTokens(user);
    }

    /**
     * Self-service email/password change for the currently authenticated user - re-verifies
     * {@code currentPassword} first (a valid JWT alone shouldn't be able to permanently take
     * over an account's login credentials), then re-issues tokens since the email is part of
     * the JWT claims and would otherwise go stale.
     */
    @Transactional
    public AuthResponse updateAccount(Long userId, UpdateAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        String newEmail = request.email().toLowerCase();
        boolean emailChanged = !newEmail.equals(user.getEmail());
        if (emailChanged && userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("EMAIL_TAKEN", "An account with this email already exists");
        }
        user.setEmail(newEmail);
        // A verification code only proves the *old* address was reachable - changing to a new
        // address must be re-verified before it counts as genuine again.
        if (emailChanged) {
            issueAndSendVerificationCode(user);
        }

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.newPassword().length() < 8) {
                throw new BadRequestException("New password must be at least 8 characters");
            }
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }
        userRepository.save(user);

        return issueTokens(user);
    }

    /**
     * Confirms the code sent to the user's own email and marks it verified. Codes expire
     * after {@value #VERIFICATION_CODE_TTL_HOURS} hours; an expired or already-verified state
     * both surface as the same generic "invalid or expired" message rather than distinguishing
     * them, so a stale code can't be used to probe whether verification already happened.
     */
    @Transactional
    public void verifyEmail(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));

        if (user.isEmailVerified()) {
            return;
        }
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)
                || user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("This verification code is invalid or has expired");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
    }

    /** Re-sends a fresh code to the caller's own (already on-file) email address. */
    @Transactional
    public void resendVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));

        if (user.isEmailVerified()) {
            throw new ConflictException("ALREADY_VERIFIED", "This email is already verified");
        }
        issueAndSendVerificationCode(user);
        userRepository.save(user);
    }

    /**
     * Generates a fresh 6-digit code, stores it (unverified) on the user, and emails it.
     *
     * <p>The email send is deliberately swallowed on failure rather than left to propagate:
     * this is called from inside {@code register}/{@code registerSupermarketOwner}/
     * {@code updateAccount}, all {@code @Transactional}, so an unhandled exception here (e.g. a
     * mail provider rejecting the recipient) would roll back the whole transaction - silently
     * discarding a brand-new account rather than just failing to deliver its verification code.
     * The account still ends up correctly marked unverified; the user can retry via
     * {@code resendVerification} once the delivery problem is resolved.
     */
    private void issueAndSendVerificationCode(User user) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setEmailVerified(false);
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(Instant.now().plus(VERIFICATION_CODE_TTL_HOURS, ChronoUnit.HOURS));
        try {
            emailService.sendVerificationCode(user.getEmail(), user.getFullName(), code);
        } catch (Exception ex) {
            log.warn("Could not send verification email to {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadRequestException("Provided token is not a refresh token");
        }

        Long userId = Long.valueOf(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));

        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), roles);
        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenTtlMillis());
    }
}
