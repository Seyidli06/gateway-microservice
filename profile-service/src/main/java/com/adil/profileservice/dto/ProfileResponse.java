package com.adil.profileservice.dto;

import java.time.Instant;

public record ProfileResponse(
        Long id,
        String name,
        String email,
        String bio,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}