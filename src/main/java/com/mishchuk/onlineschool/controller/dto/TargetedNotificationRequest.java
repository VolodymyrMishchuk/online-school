package com.mishchuk.onlineschool.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record TargetedNotificationRequest(
                @NotBlank String title,
                @NotBlank String message,
                @NotEmpty List<UUID> userIds,
                String buttonUrl) {
}
