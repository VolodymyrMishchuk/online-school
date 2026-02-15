package com.mishchuk.onlineschool.service.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final com.mishchuk.onlineschool.security.JwtUtils jwtUtils;

    @Value("${spring.application.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    @Async
    public void sendWelcomeEmail(String to, String userName) {
        log.info("Sending welcome email to {}", to);
        Context context = new Context();
        context.setVariable("userName", userName);

        String token = jwtUtils.generateMagicToken(to);
        String magicLink = frontendUrl + "/magic-login?token=" + token + "&redirect=/dashboard/all-courses";
        context.setVariable("magicLink", magicLink);

        sendHtmlEmail(to, "Ласкаво просимо до Online School!", "email/welcome", context);
    }

    @Override
    @Async
    public void sendCourseAccessGrantedEmail(String to, String userName, String courseName) {
        log.info("Sending course access granted email to {} for course {}", to, courseName);
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("courseName", courseName);

        String token = jwtUtils.generateMagicToken(to);
        String magicLink = frontendUrl + "/magic-login?token=" + token + "&redirect=/dashboard/my-courses";
        context.setVariable("magicLink", magicLink);

        sendHtmlEmail(to, "Новий курс доступний!", "email/access-granted", context);
    }

    @Override
    @Async
    public void sendCourseAccessRevokedEmail(String to, String userName, String courseName) {
        log.info("Sending course access revoked email to {} for course {}", to, courseName);
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("courseName", courseName);

        // We can include a link to catalog or keep it simple
        String catalogLink = frontendUrl + "/catalog";
        context.setVariable("catalogLink", catalogLink);

        sendHtmlEmail(to, "Доступ до курсу скасовано", "email/access-revoked", context);
    }

    @Override
    public void sendCourseExpirationReminderEmail(String to, String userName, String courseName,
            LocalDate expirationDate) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("courseName", courseName);
        context.setVariable("expirationDate", expirationDate);
        sendHtmlEmail(to, "Нагадування: закінчується термін доступу до курсу", "email/course-expiration", context);
    }

    @Override
    public void sendPasswordResetEmail(String to, String userName, String resetLink) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("resetLink", resetLink);
        sendHtmlEmail(to, "Відновлення паролю | Svitlo School", "email/password-reset", context);
    }

    @Override
    public void sendAccessExtendedEmail(String to, String userName, String courseName, LocalDate expirationDate) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("courseName", courseName);
        context.setVariable("expirationDate", expirationDate);
        // We can link directly to the course or just the dashboard
        context.setVariable("courseUrl", frontendUrl + "/dashboard/my-courses");

        sendHtmlEmail(to, "Доступ до курсу продовжено!", "email/access-extended", context);
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
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            // In a real app, might want to retry or store failed emails
        }
    }
}
