package com.aislego.identity.dto;

import java.util.List;

public record MeResponse(Long id, String email, List<String> roles) {
}
