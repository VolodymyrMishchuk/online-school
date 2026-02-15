package com.mishchuk.onlineschool.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mishchuk.onlineschool.repository.entity.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationDto(
                UUID id,
                String title,
                String message,
                NotificationType type,
                boolean read,
                @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime createdAt,
                String buttonUrl) {
}
