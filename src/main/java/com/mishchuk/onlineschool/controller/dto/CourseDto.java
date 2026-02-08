package com.mishchuk.onlineschool.controller.dto;

import java.time.OffsetDateTime;

public record CourseDto(
        java.util.UUID id,
        String name,
        String description,
        Integer modulesNumber,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Boolean isEnrolled,
        OffsetDateTime enrolledAt) {
}
