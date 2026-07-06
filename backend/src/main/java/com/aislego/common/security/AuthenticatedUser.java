package com.aislego.common.security;

import java.util.List;

/**
 * The authenticated principal placed into the {@code SecurityContext} by
 * {@link JwtAuthenticationFilter}. Controllers can inject this via
 * {@code @AuthenticationPrincipal} to get the current user's id without another DB lookup.
 */
public record AuthenticatedUser(Long userId, String email, List<String> roles) {
}
