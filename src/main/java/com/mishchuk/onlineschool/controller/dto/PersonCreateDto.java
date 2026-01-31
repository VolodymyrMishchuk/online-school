package com.mishchuk.onlineschool.controller.dto;

import java.time.OffsetDateTime;

public record PersonCreateDto(
        String firstName,
        String lastName,
        OffsetDateTime bornedAt,
        String phoneNumber,
        String email,
        String password) {
}
