package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.repository.entity.PaymentEntity;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import static com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle.NORMAL;

@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final TemplateEngine templateEngine;

    @Override
    public byte[] generateReceiptPdf(PaymentEntity payment) {
        Context context = new Context();
        context.setVariable("payment", payment);
        context.setVariable("personName", payment.getPerson().getFirstName() + " " + payment.getPerson().getLastName());
        context.setVariable("courseName", payment.getCourse() != null ? payment.getCourse().getName() : "Unknown Course");
        context.setVariable("date", payment.getCreatedAt() != null ? payment.getCreatedAt().toLocalDate() : LocalDate.now());

        String html = templateEngine.process("email/receipt", context);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(() -> getClass().getResourceAsStream("/fonts/Roboto-Regular.ttf"), "Roboto", 400, NORMAL, true);
            builder.useFont(() -> getClass().getResourceAsStream("/fonts/Roboto-Bold.ttf"), "Roboto", 700, NORMAL, true);
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF receipt", e);
        }
    }
}
