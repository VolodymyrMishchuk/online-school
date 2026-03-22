package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.dto.PromoCodeCheckResponseDto;
import com.mishchuk.onlineschool.dto.PromoCodeCreateDto;
import com.mishchuk.onlineschool.dto.PromoCodeResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PromoCodeService {
    Page<PromoCodeResponseDto> getPaginatedPromoCodes(String search, String sortKey, String sortDir, String statusSort, Pageable pageable);
    PromoCodeResponseDto createPromoCode(PromoCodeCreateDto createDto, String currentUsername);
    PromoCodeCheckResponseDto checkPromoCode(String code, String currentUsername);
    PromoCodeResponseDto updatePromoCode(UUID id, PromoCodeCreateDto updateDto, String currentUsername);
    void deletePromoCode(UUID id, String currentUsername);
    void usePromoCode(String code, UUID courseId, String currentUsername);
}
