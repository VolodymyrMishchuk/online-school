package com.mishchuk.onlineschool.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PromoCodeCheckResponseDto {
    private String code;
    private List<PromoCodeDiscountResponseDto> discounts;
    private List<CourseWithPromoDto> courses;
}
