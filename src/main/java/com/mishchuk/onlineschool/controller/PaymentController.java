package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.dto.PaymentRequestDto;
import com.mishchuk.onlineschool.dto.PaymentResponseDto;
import com.mishchuk.onlineschool.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @PreAuthorize("isAuthenticated()")
    public PaymentResponseDto processPayment(@Valid @RequestBody PaymentRequestDto request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        return paymentService.processPayment(request, userDetails.getUsername());
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<PaymentResponseDto> getAllPayments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortKey,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Explicity restricting to ADMIN. FAKE_ADMIN won't have access.
        return paymentService.getAllPayments(search, sortKey, sortDir, startDate, endDate, pageable);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Page<PaymentResponseDto> getMyPayments(Pageable pageable,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        return paymentService.getMyPayments(pageable, userDetails.getUsername());
    }

    @GetMapping("/{paymentId}/receipt")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public org.springframework.http.ResponseEntity<byte[]> getPaymentReceipt(
            @PathVariable java.util.UUID paymentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        byte[] pdf = paymentService.getPaymentReceipt(paymentId, userDetails.getUsername());
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receipt_" + paymentId + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return new org.springframework.http.ResponseEntity<>(pdf, headers, org.springframework.http.HttpStatus.OK);
    }
}
