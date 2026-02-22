package com.mishchuk.onlineschool.controller.dto;

import java.util.UUID;

public record CreatedByDto(
        UUID id,
        String firstName,
        String lastName,
        String email) {
}
