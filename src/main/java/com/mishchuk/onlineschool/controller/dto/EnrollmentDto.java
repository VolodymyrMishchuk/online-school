package com.mishchuk.onlineschool.controller.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EnrollmentDto(
        UUID id,
        UUID studentId,
        UUID courseId,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
