package com.mishchuk.onlineschool.controller.dto;

import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record PersonCreateDto(
        String firstName,
        String lastName,
        OffsetDateTime bornedAt,
        @Size(max = 20, message = "Телефон має бути не більше 20 символів") String phoneNumber,
        String email,
        String password,
        java.util.List<java.util.UUID> courseIds) {
}
