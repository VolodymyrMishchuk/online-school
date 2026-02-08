package com.mishchuk.onlineschool.controller.dto;

import java.time.OffsetDateTime;

public record CourseDto(
                java.util.UUID id,
                String name,
                String description,
                Integer modulesNumber,
                String status,
                java.math.BigDecimal price,
                java.math.BigDecimal discountAmount,
                Integer discountPercentage,
                Integer accessDuration,
                OffsetDateTime createdAt,
                OffsetDateTime updatedAt,
                Boolean isEnrolled,
                OffsetDateTime enrolledAt,
                String enrollmentStatus) {
}
