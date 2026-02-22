package com.mishchuk.onlineschool.controller.dto;

import java.time.OffsetDateTime;

public record PersonDto(
        java.util.UUID id,
        String firstName,
        String lastName,
        OffsetDateTime bornedAt,
        String phoneNumber,
        String email,
        String role,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        CreatedByDto createdBy) {
}
