package com.mishchuk.onlineschool.controller.dto;

public record CourseUpdateDto(
        String name,
        String description,
        java.math.BigDecimal price,
        java.math.BigDecimal discountAmount,
        Integer discountPercentage,
        Integer accessDuration,
        String status,
        Integer promotionalDiscountPercentage,
        java.math.BigDecimal promotionalDiscountAmount,
        Integer renewalDiscountPercentage,
        java.math.BigDecimal renewalDiscountAmount,
        Boolean extendForReviewEnabled,
        Boolean renewalEnabled,
        Boolean nextCourseDiscountEnabled,
        java.util.UUID nextCourseId,
        java.util.List<java.util.UUID> moduleIds,
        Boolean deleteCoverImage) {
    public CourseUpdateDto {
        deleteCoverImage = deleteCoverImage != null ? deleteCoverImage : false;
        extendForReviewEnabled = extendForReviewEnabled != null ? extendForReviewEnabled : true;
        renewalEnabled = renewalEnabled != null ? renewalEnabled : true;
        nextCourseDiscountEnabled = nextCourseDiscountEnabled != null ? nextCourseDiscountEnabled : true;
    }
}
