package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.repository.entity.PaymentEntity;

public interface PdfService {
    byte[] generateReceiptPdf(PaymentEntity payment);
}
