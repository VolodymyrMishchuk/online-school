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
                UUID nextCourseId,
                List<UUID> moduleIds) {
}
