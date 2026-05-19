package com.mishchuk.onlineschool.dto;

import com.mishchuk.onlineschool.repository.entity.PaymentSystem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentRequestDto {
    @NotNull(message = "Course ID is required")
    private UUID courseId;

    @NotNull(message = "Payment system is required")
    private PaymentSystem paymentSystem;

    @NotBlank(message = "Country is required")
    private String country;

    private String promoCode;
}
