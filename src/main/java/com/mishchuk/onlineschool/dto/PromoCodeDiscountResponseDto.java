package com.mishchuk.onlineschool.dto;

import com.mishchuk.onlineschool.repository.entity.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PromoCodeDiscountResponseDto {
    private UUID courseId;
    private String courseName;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal originalCoursePrice;
}
