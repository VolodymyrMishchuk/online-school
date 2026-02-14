package com.mishchuk.onlineschool.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record BroadcastRequest(
                @NotBlank String title,
                @NotBlank String message,
                String buttonUrl) {
}
