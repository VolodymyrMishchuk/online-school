package com.mishchuk.onlineschool.controller.dto;

public record CourseUpdateDto(
        String name,
        String description,
        java.math.BigDecimal price,
        java.math.BigDecimal discountAmount,
        Integer discountPercentage,
        Integer accessDuration,
        String status,
        java.math.BigDecimal promotionalDiscount,
        java.util.UUID nextCourseId,
        java.util.List<java.util.UUID> moduleIds) {
}
