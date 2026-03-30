package com.mishchuk.onlineschool.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PersonCreateDto(
                String firstName,
                String lastName,
                OffsetDateTime bornedAt,
                @Size(max = 20, message = "Телефон має бути не більше 20 символів")
                String phoneNumber,
                @NotBlank(message = "Email is required")
                @Email(message = "Must be a valid email")
                String email,
                String password,
                String language,
                List<UUID> courseIds) {
}
