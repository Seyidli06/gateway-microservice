package com.adil.feedbackservice.dto;

import java.time.Instant;

public record FeedbackResponse(
        Long id,
        String name,
        String email,
        String message,
        Long version,
        Instant createdAt
) {
}