package com.mishchuk.onlineschool.controller.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CourseCreateDto(
                String name,
                String description,
                BigDecimal price,
                BigDecimal discountAmount,
                Integer discountPercentage,
                Integer accessDuration,
                Integer promotionalDiscountPercentage,
                BigDecimal promotionalDiscountAmount,
                Integer renewalDiscountPercentage,
                BigDecimal renewalDiscountAmount,
                Boolean extendForReviewEnabled,
                Boolean renewalEnabled,
                Boolean nextCourseDiscountEnabled,
                UUID nextCourseId,
                List<UUID> moduleIds) {
}
