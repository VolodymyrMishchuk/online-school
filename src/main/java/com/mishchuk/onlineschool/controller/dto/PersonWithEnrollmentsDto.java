package com.mishchuk.onlineschool.controller.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PersonWithEnrollmentsDto(
        UUID id,
        String firstName,
        String lastName,
        OffsetDateTime bornedAt,
        String phoneNumber,
        String email,
        String language,
        String avatarUrl,
        String role,
        String status,
        List<EnrollmentDto> enrollments,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean hasPassword,
        CreatedByDto createdBy) {
}
