package com.mishchuk.onlineschool.dto;

import com.mishchuk.onlineschool.repository.entity.PaymentStatus;
import com.mishchuk.onlineschool.repository.entity.PaymentSystem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class PaymentResponseDto {
    private UUID id;
    private UUID personId;
    private String personName;
    private String personEmail;
    private UUID courseId;
    private String courseName;
    private BigDecimal amount;
    private String currency;
    private PaymentSystem paymentSystem;
    private PaymentStatus status;
    private String country;
    private OffsetDateTime createdAt;
    private Integer accessDurationDays;
    private OffsetDateTime accessExpiresAt;
}
