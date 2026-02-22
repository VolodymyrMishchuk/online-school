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
                String role,
                String status,
                List<EnrollmentDto> enrollments,
                OffsetDateTime createdAt,
                OffsetDateTime updatedAt,
                CreatedByDto createdBy) {
}
