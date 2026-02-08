package com.mishchuk.onlineschool.controller.dto;

public record CourseCreateDto(
                String name,
                String description,
                java.math.BigDecimal price,
                java.math.BigDecimal discountAmount,
                Integer discountPercentage,
                Integer accessDuration,
                java.util.List<java.util.UUID> moduleIds) {
}
