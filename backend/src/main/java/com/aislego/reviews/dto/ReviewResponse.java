package com.aislego.reviews.dto;

import com.aislego.reviews.domain.Review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        String reviewerName,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
