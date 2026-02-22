package com.mishchuk.onlineschool.controller.dto;

import java.time.OffsetDateTime;

public record CourseDto(
        java.util.UUID id,
        String name,
        String description,
        Integer modulesNumber,
        Integer lessonsCount,
        Integer durationMinutes,
        String status,
        String version,
        java.math.BigDecimal price,
        java.math.BigDecimal discountAmount,
        Integer discountPercentage,
        Integer accessDuration,
        Integer promotionalDiscountPercentage,
        java.math.BigDecimal promotionalDiscountAmount,
        java.util.UUID nextCourseId,
        String nextCourseName,
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) OffsetDateTime createdAt,
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) OffsetDateTime updatedAt,
        Boolean isEnrolled,
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) OffsetDateTime enrolledAt,
        String enrollmentStatus,
        String coverImageUrl,
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) OffsetDateTime expiresAt,
        String averageColor,
        CreatedByDto createdBy) {
}
