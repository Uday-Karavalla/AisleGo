package com.aislego.growth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record TrackEventRequest(
        @NotBlank @Size(max = 48) String eventName,
        @Size(max = 80) String sessionId,
        Map<String, Object> metadata
) {
}
