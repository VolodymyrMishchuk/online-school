package com.mishchuk.onlineschool.dto;

import com.mishchuk.onlineschool.repository.entity.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CourseWithPromoDto {
    private UUID courseId;
    private String name;
    private String description;
    private String coverImageUrl;
    private String averageColor;
    private BigDecimal price;
    private BigDecimal promoPrice;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private int modulesCount;
    private int lessonsCount;
}
