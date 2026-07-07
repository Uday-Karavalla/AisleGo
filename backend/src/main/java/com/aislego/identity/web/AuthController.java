package com.aislego.identity.web;

import com.aislego.common.exception.UnauthorizedException;
import com.aislego.common.security.AuthenticatedUser;
import com.aislego.identity.dto.AuthResponse;
import com.aislego.identity.dto.LoginRequest;
import com.aislego.identity.dto.MeResponse;
import com.aislego.identity.dto.RefreshRequest;
import com.aislego.identity.dto.RegisterRequest;
import com.aislego.identity.dto.RegisterSupermarketOwnerRequest;
import com.aislego.identity.dto.SupermarketOwnerAuthResponse;
import com.aislego.identity.dto.UpdateAccountRequest;
import com.aislego.identity.dto.VerifyEmailRequest;
import com.aislego.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/register-supermarket-owner")
    public ResponseEntity<SupermarketOwnerAuthResponse> registerSupermarketOwner(
            @Valid @RequestBody RegisterSupermarketOwnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerSupermarketOwner(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * Lets the frontend rehydrate "who's logged in" after a page reload from a stored token
     * alone. {@code /api/auth/**} is {@code permitAll()} at the filter-chain level, so the
     * request reaches here regardless of whether a token was sent - the JWT filter always runs
     * and populates {@code AuthenticatedUser} when a valid Bearer token is present, but with no
     * (or an anonymous) principal {@code @AuthenticationPrincipal} resolves to {@code null}
     * rather than throwing. An explicit null check is what actually enforces "must be logged in
     * for this to mean anything", via the same {@link UnauthorizedException} -> 401 path already
     * used by {@code AuthService.login}/{@code refresh}, deliberately not a {@code @PreAuthorize}
     * check - that would surface as 403 (ACCESS_DENIED) through {@code GlobalExceptionHandler},
     * not the 401 a missing/invalid token should produce.
     */
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        return ResponseEntity.ok(authService.getMe(principal.userId()));
    }

    /** Self-service email/password change - see {@link com.aislego.identity.service.AuthService#updateAccount}. */
    @PatchMapping("/me")
    public ResponseEntity<AuthResponse> updateMe(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @Valid @RequestBody UpdateAccountRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        return ResponseEntity.ok(authService.updateAccount(principal.userId(), request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @Valid @RequestBody VerifyEmailRequest request) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        authService.verifyEmail(principal.userId(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        authService.resendVerification(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
