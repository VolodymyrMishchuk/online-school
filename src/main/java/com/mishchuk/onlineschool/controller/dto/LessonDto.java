package com.mishchuk.onlineschool.controller.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LessonDto(
        UUID id,
        UUID moduleId,
        String name,
        String description,
        String videoUrl,
        Integer durationMinutes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
