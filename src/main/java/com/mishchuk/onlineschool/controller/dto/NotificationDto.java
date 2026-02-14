package com.mishchuk.onlineschool.controller.dto;

import com.mishchuk.onlineschool.repository.entity.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationDto(
                UUID id,
                String title,
                String message,
                NotificationType type,
                boolean read,
                @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) OffsetDateTime createdAt) {
}
