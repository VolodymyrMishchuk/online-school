package com.mishchuk.onlineschool.controller.dto;

import java.util.UUID;

public record AuthResponse(
                String accessToken,
                UUID userId,
                String role,
                String firstName,
                String lastName,
                String language) {
}
