package com.mishchuk.onlineschool.dto;

import com.mishchuk.onlineschool.repository.entity.DiscountType;
import com.mishchuk.onlineschool.repository.entity.PromoCodeScope;
import com.mishchuk.onlineschool.repository.entity.PromoCodeStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class PromoCodeCreateDto {
    private String code;
    private PromoCodeStatus status;
    private PromoCodeScope scope;
    private Set<UUID> targetPersonIds;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private List<PromoCodeDiscountDto> discounts;

    @Data
    public static class PromoCodeDiscountDto {
        private UUID courseId;
        private DiscountType discountType;
        private BigDecimal discountValue;
    }
}
