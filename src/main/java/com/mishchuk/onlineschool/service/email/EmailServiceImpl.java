package com.mishchuk.onlineschool.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Override
    @Async
    public void sendWelcomeEmail(String to, String userName) {
        log.info("Sending welcome email to {}", to);
        Context context = new Context();
        context.setVariable("userName", userName);
        sendHtmlEmail(to, "Ласкаво просимо до Online School!", "email/welcome", context);
    }

    @Override
    @Async
    public void sendCoursePurchaseEmail(String to, String userName, String courseName) {
        log.info("Sending course purchase email to {} for course {}", to, courseName);
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("courseName", courseName);
        sendHtmlEmail(to, "Вітаємо з покупкою курсу!", "email/course-purchase", context);
    }

    @Override
    @Async
    public void sendCourseExpirationReminderEmail(String to, String userName, String courseName,
            LocalDate expirationDate) {
        log.info("Sending expiration reminder email to {} for course {}", to, courseName);
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("courseName", courseName);
        context.setVariable("expirationDate", expirationDate);
        sendHtmlEmail(to, "Нагадування: закінчується доступ до курсу", "email/course-expiration", context);
    }

    private void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            String htmlContent = templateEngine.process(templateName, context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@onlineschool.com");

            javaMailSender.send(mimeMessage);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
            // In a real app, might want to retry or store failed emails
        }
    }
}
