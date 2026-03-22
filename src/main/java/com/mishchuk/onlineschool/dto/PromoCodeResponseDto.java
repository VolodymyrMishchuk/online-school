package com.mishchuk.onlineschool.dto;

import com.mishchuk.onlineschool.repository.entity.PromoCodeScope;
import com.mishchuk.onlineschool.repository.entity.PromoCodeStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PromoCodeResponseDto {
    private UUID id;
    private String code;
    private PromoCodeStatus status;
    private PromoCodeScope scope;
    private List<PromoCodeTargetUserDto> targetPersons;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String validFromDisplay;
    private String validUntilDisplay;
    private boolean isPendingActivation;
    private List<PromoCodeDiscountResponseDto> discounts;

    @Data
    public static class PromoCodeTargetUserDto {
        private UUID id;
        private String name;
        private String email;
        private String phone;
        private List<UUID> usedCourseIds;
    }
}
