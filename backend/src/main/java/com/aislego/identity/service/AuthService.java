package com.aislego.identity.service;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.domain.SupermarketStatus;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.BadRequestException;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.UnauthorizedException;
import com.aislego.common.security.JwtService;
import com.aislego.identity.domain.Role;
import com.aislego.identity.domain.User;
import com.aislego.identity.dto.AuthResponse;
import com.aislego.identity.dto.LoginRequest;
import com.aislego.identity.dto.RegisterRequest;
import com.aislego.identity.dto.RegisterSupermarketOwnerRequest;
import com.aislego.identity.dto.SupermarketOwnerAuthResponse;
import com.aislego.identity.dto.UpdateAccountRequest;
import com.aislego.identity.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SupermarketRepository supermarketRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, SupermarketRepository supermarketRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.supermarketRepository = supermarketRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("This account has been disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

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
        if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("EMAIL_TAKEN", "An account with this email already exists");
        }
        user.setEmail(newEmail);

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.newPassword().length() < 8) {
                throw new BadRequestException("New password must be at least 8 characters");
            }
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }
        userRepository.save(user);

        return issueTokens(user);
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
