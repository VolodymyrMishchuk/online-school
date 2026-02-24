package com.mishchuk.onlineschool.controller.dto;

public record AuthResultDto(
        AuthResponse authResponse,
        String refreshToken) {
}
