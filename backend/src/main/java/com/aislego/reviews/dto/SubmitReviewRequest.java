package com.aislego.reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SubmitReviewRequest(
        @Min(1) @Max(5) int rating,
        String comment
) {
}
