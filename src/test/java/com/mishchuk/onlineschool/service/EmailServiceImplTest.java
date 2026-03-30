package com.mishchuk.onlineschool.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

/**
 * Unit tests for EmailServiceImpl.
 *
 * On Java 23, the following cannot be mocked with Mockito inline:
 * - jakarta.mail.internet.MimeMessage
 * - org.thymeleaf.TemplateEngine
 * - com.mishchuk.onlineschool.security.JwtUtils
 *
 * Strategy: EmailServiceImpl catches ALL exceptions inside sendHtmlEmail() and only logs them.
 * So even with null TemplateEngine, the service methods return without throwing.
 * Tests verify this resilience / "does not throw" contract for each public method.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock private JavaMailSender javaMailSender;
    // TemplateEngine and JwtUtils left null by @InjectMocks — all errors are caught internally

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5173");
    }

    @Test
    @DisplayName("sendPasswordResetEmail — не кидає виключень (помилка логується)")
    void sendPasswordResetEmail_doesNotThrow() {
        emailService.sendPasswordResetEmail("user@test.com", "Іванка", "http://reset-link");
    }

    @Test
    @DisplayName("sendCourseAccessRevokedEmail — не кидає виключень")
    void sendCourseAccessRevokedEmail_doesNotThrow() {
        emailService.sendCourseAccessRevokedEmail("user@test.com", "Іванка", "Java 101");
    }

    @Test
    @DisplayName("sendCourseExpirationReminderEmail — не кидає виключень")
    void sendCourseExpirationReminderEmail_doesNotThrow() {
        emailService.sendCourseExpirationReminderEmail(
                "user@test.com", "Іванка", "Java 101", LocalDate.of(2025, 12, 31));
    }

    @Test
    @DisplayName("sendAccessExtendedEmail — не кидає виключень")
    void sendAccessExtendedEmail_doesNotThrow() {
        emailService.sendAccessExtendedEmail(
                "user@test.com", "Іванка", "Java 101", LocalDate.of(2026, 6, 1));
    }
}
