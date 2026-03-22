package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.dto.PromoCodeCheckResponseDto;
import com.mishchuk.onlineschool.dto.PromoCodeCreateDto;
import com.mishchuk.onlineschool.dto.PromoCodeResponseDto;
import com.mishchuk.onlineschool.service.PromoCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/promo-codes")
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'FAKE_ADMIN')")
    public ResponseEntity<Page<PromoCodeResponseDto>> getPaginatedPromoCodes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortKey,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String statusSort
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PromoCodeResponseDto> result = promoCodeService.getPaginatedPromoCodes(search, sortKey, sortDir, statusSort, pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCodeResponseDto> createPromoCode(
            @RequestBody PromoCodeCreateDto dto,
            Authentication authentication
    ) {
        PromoCodeResponseDto created = promoCodeService.createPromoCode(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/check")
    public ResponseEntity<PromoCodeCheckResponseDto> checkPromoCode(
            @RequestParam String code,
            Authentication authentication
    ) {
        PromoCodeCheckResponseDto response = promoCodeService.checkPromoCode(code, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/use")
    public ResponseEntity<Void> usePromoCode(
            @RequestParam String code,
            @RequestParam java.util.UUID courseId,
            Authentication authentication
    ) {
        promoCodeService.usePromoCode(code, courseId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCodeResponseDto> updatePromoCode(
            @PathVariable UUID id,
            @RequestBody PromoCodeCreateDto dto,
            Authentication authentication
    ) {
        PromoCodeResponseDto updated = promoCodeService.updatePromoCode(id, dto, authentication.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePromoCode(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        promoCodeService.deletePromoCode(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
