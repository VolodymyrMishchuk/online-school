package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.dto.PaymentRequestDto;
import com.mishchuk.onlineschool.dto.PaymentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentResponseDto processPayment(PaymentRequestDto request, String currentUsername);
    Page<PaymentResponseDto> getAllPayments(String search, String sortKey, String sortDir, java.time.LocalDate startDate, java.time.LocalDate endDate, Pageable pageable);
    Page<PaymentResponseDto> getMyPayments(Pageable pageable, String currentUsername);
    byte[] getPaymentReceipt(java.util.UUID paymentId, String currentUsername);
}
