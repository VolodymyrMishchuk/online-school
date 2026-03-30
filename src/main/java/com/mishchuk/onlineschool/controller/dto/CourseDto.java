package com.mishchuk.onlineschool.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CourseDto(
        UUID id,
        String name,
        String description,
        Integer modulesNumber,
        Integer lessonsCount,
        Integer durationMinutes,
        String status,
        String version,
        BigDecimal price,
        BigDecimal discountAmount,
        Integer discountPercentage,
        Integer accessDuration,
        Integer promotionalDiscountPercentage,
        BigDecimal promotionalDiscountAmount,
        UUID nextCourseId,
        String nextCourseName,
        @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime updatedAt,
        Boolean isEnrolled,
        @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime enrolledAt,
        String enrollmentStatus,
        String coverImageUrl,
        @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime expiresAt,
        String averageColor,
        CreatedByDto createdBy) {
}
