package com.aislego.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Issues and validates the platform's access and refresh JWTs. Deliberately minimal:
 * subject = user id, plus email and roles as custom claims. No token blacklist / rotation
 * store yet - that belongs to a later hardening pass, not the first working flow.
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenTtlMillis;
    private final long refreshTokenTtlMillis;

    public JwtService(
            @Value("${aislego.security.jwt.secret}") String secret,
            @Value("${aislego.security.jwt.access-token-ttl-ms}") long accessTokenTtlMillis,
            @Value("${aislego.security.jwt.refresh-token-ttl-ms}") long refreshTokenTtlMillis) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMillis = accessTokenTtlMillis;
        this.refreshTokenTtlMillis = refreshTokenTtlMillis;
    }

    public String generateAccessToken(Long userId, String email, List<String> roles) {
        return generateToken(userId, email, roles, "access", accessTokenTtlMillis);
    }

    public String generateRefreshToken(Long userId, String email, List<String> roles) {
        return generateToken(userId, email, roles, "refresh", refreshTokenTtlMillis);
    }

    public long getAccessTokenTtlMillis() {
        return accessTokenTtlMillis;
    }

    private String generateToken(Long userId, String email, List<String> roles, String type, long ttlMillis) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", roles)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMillis)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and validates a token's signature/expiry. Throws {@link JwtException} on any
     * problem - callers translate that into a 401.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }
}
