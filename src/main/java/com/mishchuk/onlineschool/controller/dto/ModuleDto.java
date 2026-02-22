package com.mishchuk.onlineschool.controller.dto;

import java.time.OffsetDateTime;

public record ModuleDto(
        java.util.UUID id,
        String name,
        java.util.UUID courseId,
        String description,
        Integer lessonsNumber,
        Integer durationMinutes,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        CreatedByDto createdBy) {
}
