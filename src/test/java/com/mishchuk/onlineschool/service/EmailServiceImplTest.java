package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.security.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock private JavaMailSender javaMailSender;
    @Mock private TemplateEngine templateEngine;
    @Mock private JwtUtils jwtUtils;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5173");
        when(javaMailSender.createMimeMessage())
                .thenReturn(new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null));
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>content</html>");
        
        ((Logger) LoggerFactory.getLogger(EmailServiceImpl.class)).setLevel(Level.OFF);
    }

    @AfterEach
    void clearSecurityContext() {
        ((Logger) LoggerFactory.getLogger(EmailServiceImpl.class)).setLevel(null);
    }

    // ─────────────────────── sendWelcomeEmail ───────────────────────

    @Test
    @DisplayName("sendWelcomeEmail — не кидає виключень")
    void sendWelcomeEmail_doesNotThrow() {
        when(jwtUtils.generateMagicToken("user@test.com")).thenReturn("magic-token-123");

        assertThatCode(() -> emailService.sendWelcomeEmail("user@test.com", "Іванка"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendWelcomeEmail — генерує magic token для коректного email")
    void sendWelcomeEmail_generatesMagicTokenForCorrectEmail() {
        when(jwtUtils.generateMagicToken("user@test.com")).thenReturn("magic-token-123");

        emailService.sendWelcomeEmail("user@test.com", "Іванка");

        verify(jwtUtils).generateMagicToken("user@test.com");
    }

    @Test
    @DisplayName("sendWelcomeEmail — передає userName і magicLink у шаблон")
    void sendWelcomeEmail_passesCorrectVariablesToTemplate() {
        when(jwtUtils.generateMagicToken("user@test.com")).thenReturn("magic-token-123");

        emailService.sendWelcomeEmail("user@test.com", "Іванка");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/welcome"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertThat(context.getVariable("userName")).isEqualTo("Іванка");
        assertThat(context.getVariable("magicLink").toString())
                .contains("magic-token-123")
                .contains("/magic-login")
                .contains("/dashboard/all-courses")
                .startsWith("http://localhost:5173");
    }

    @Test
    @DisplayName("sendWelcomeEmail — намагається відправити листа")
    void sendWelcomeEmail_attemptsToSend() {
        when(jwtUtils.generateMagicToken("user@test.com")).thenReturn("magic-token-123");

        emailService.sendWelcomeEmail("user@test.com", "Іванка");

        verify(javaMailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    // ─────────────────────── sendCourseAccessGrantedEmail ───────────────────────

    @Test
    @DisplayName("sendCourseAccessGrantedEmail — не кидає виключень")
    void sendCourseAccessGrantedEmail_doesNotThrow() {
        when(jwtUtils.generateMagicToken("user@test.com")).thenReturn("magic-token-456");

        assertThatCode(() ->
                emailService.sendCourseAccessGrantedEmail("user@test.com", "Іванка", "Java 101"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendCourseAccessGrantedEmail — генерує magic token для коректного email")
    void sendCourseAccessGrantedEmail_generatesMagicTokenForCorrectEmail() {
        when(jwtUtils.generateMagicToken("user@test.com")).thenReturn("magic-token-456");

        emailService.sendCourseAccessGrantedEmail("user@test.com", "Іванка", "Java 101");

        verify(jwtUtils).generateMagicToken("user@test.com");
    }

    @Test
    @DisplayName("sendCourseAccessGrantedEmail — передає userName, courseName і magicLink у шаблон")
    void sendCourseAccessGrantedEmail_passesCorrectVariablesToTemplate() {
        when(jwtUtils.generateMagicToken("user@test.com")).thenReturn("magic-token-456");

        emailService.sendCourseAccessGrantedEmail("user@test.com", "Іванка", "Java 101");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/access-granted"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertThat(context.getVariable("userName")).isEqualTo("Іванка");
        assertThat(context.getVariable("courseName")).isEqualTo("Java 101");
        assertThat(context.getVariable("magicLink").toString())
                .contains("magic-token-456")
                .contains("/magic-login")
                .contains("/dashboard/my-courses")
                .startsWith("http://localhost:5173");
    }

    @Test
    @DisplayName("sendCourseAccessGrantedEmail — намагається відправити листа")
    void sendCourseAccessGrantedEmail_attemptsToSend() {
        when(jwtUtils.generateMagicToken("user@test.com")).thenReturn("magic-token-456");

        emailService.sendCourseAccessGrantedEmail("user@test.com", "Іванка", "Java 101");

        verify(javaMailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    // ─────────────────────── sendCourseAccessRevokedEmail ───────────────────────

    @Test
    @DisplayName("sendCourseAccessRevokedEmail — не кидає виключень")
    void sendCourseAccessRevokedEmail_doesNotThrow() {
        assertThatCode(() ->
                emailService.sendCourseAccessRevokedEmail("user@test.com", "Іванка", "Java 101"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendCourseAccessRevokedEmail — передає userName, courseName і catalogLink у шаблон")
    void sendCourseAccessRevokedEmail_passesCorrectVariablesToTemplate() {
        emailService.sendCourseAccessRevokedEmail("user@test.com", "Іванка", "Java 101");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/access-revoked"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertThat(context.getVariable("userName")).isEqualTo("Іванка");
        assertThat(context.getVariable("courseName")).isEqualTo("Java 101");
        assertThat(context.getVariable("catalogLink").toString())
                .isEqualTo("http://localhost:5173/catalog");
    }

    @Test
    @DisplayName("sendCourseAccessRevokedEmail — намагається відправити листа")
    void sendCourseAccessRevokedEmail_attemptsToSend() {
        emailService.sendCourseAccessRevokedEmail("user@test.com", "Іванка", "Java 101");

        verify(javaMailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    @DisplayName("sendCourseAccessRevokedEmail — не викликає jwtUtils")
    void sendCourseAccessRevokedEmail_doesNotUseJwtUtils() {
        emailService.sendCourseAccessRevokedEmail("user@test.com", "Іванка", "Java 101");

        verify(jwtUtils, never()).generateMagicToken(anyString());
    }

    // ─────────────────────── sendCourseExpirationReminderEmail ───────────────────────

    @Test
    @DisplayName("sendCourseExpirationReminderEmail — не кидає виключень")
    void sendCourseExpirationReminderEmail_doesNotThrow() {
        assertThatCode(() ->
                emailService.sendCourseExpirationReminderEmail(
                        "user@test.com", "Іванка", "Java 101", LocalDate.of(2025, 12, 31)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendCourseExpirationReminderEmail — передає userName, courseName і expirationDate у шаблон")
    void sendCourseExpirationReminderEmail_passesCorrectVariablesToTemplate() {
        LocalDate expirationDate = LocalDate.of(2025, 12, 31);

        emailService.sendCourseExpirationReminderEmail("user@test.com", "Іванка", "Java 101", expirationDate);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/course-expiration"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertThat(context.getVariable("userName")).isEqualTo("Іванка");
        assertThat(context.getVariable("courseName")).isEqualTo("Java 101");
        assertThat(context.getVariable("expirationDate")).isEqualTo(expirationDate);
    }

    @Test
    @DisplayName("sendCourseExpirationReminderEmail — намагається відправити листа")
    void sendCourseExpirationReminderEmail_attemptsToSend() {
        emailService.sendCourseExpirationReminderEmail(
                "user@test.com", "Іванка", "Java 101", LocalDate.of(2025, 12, 31));

        verify(javaMailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    @DisplayName("sendCourseExpirationReminderEmail — не кидає виключень для минулої дати")
    void sendCourseExpirationReminderEmail_pastDate_doesNotThrow() {
        assertThatCode(() ->
                emailService.sendCourseExpirationReminderEmail(
                        "user@test.com", "Іванка", "Java 101", LocalDate.of(2020, 1, 1)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendCourseExpirationReminderEmail — не викликає jwtUtils")
    void sendCourseExpirationReminderEmail_doesNotUseJwtUtils() {
        emailService.sendCourseExpirationReminderEmail(
                "user@test.com", "Іванка", "Java 101", LocalDate.of(2025, 12, 31));

        verify(jwtUtils, never()).generateMagicToken(anyString());
    }

    // ─────────────────────── sendPasswordResetEmail ───────────────────────

    @Test
    @DisplayName("sendPasswordResetEmail — не кидає виключень")
    void sendPasswordResetEmail_doesNotThrow() {
        assertThatCode(() ->
                emailService.sendPasswordResetEmail("user@test.com", "Іванка", "http://reset-link"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendPasswordResetEmail — передає userName і resetLink у шаблон")
    void sendPasswordResetEmail_passesCorrectVariablesToTemplate() {
        emailService.sendPasswordResetEmail("user@test.com", "Іванка", "http://reset-link");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/password-reset"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertThat(context.getVariable("userName")).isEqualTo("Іванка");
        assertThat(context.getVariable("resetLink")).isEqualTo("http://reset-link");
    }

    @Test
    @DisplayName("sendPasswordResetEmail — намагається відправити листа")
    void sendPasswordResetEmail_attemptsToSend() {
        emailService.sendPasswordResetEmail("user@test.com", "Іванка", "http://reset-link");

        verify(javaMailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    @DisplayName("sendPasswordResetEmail — не викликає jwtUtils")
    void sendPasswordResetEmail_doesNotUseJwtUtils() {
        emailService.sendPasswordResetEmail("user@test.com", "Іванка", "http://reset-link");

        verify(jwtUtils, never()).generateMagicToken(anyString());
    }

    // ─────────────────────── sendAccessExtendedEmail ───────────────────────

    @Test
    @DisplayName("sendAccessExtendedEmail — не кидає виключень")
    void sendAccessExtendedEmail_doesNotThrow() {
        assertThatCode(() ->
                emailService.sendAccessExtendedEmail(
                        "user@test.com", "Іванка", "Java 101", LocalDate.of(2026, 6, 1)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendAccessExtendedEmail — передає userName, courseName, expirationDate і courseUrl у шаблон")
    void sendAccessExtendedEmail_passesCorrectVariablesToTemplate() {
        LocalDate expirationDate = LocalDate.of(2026, 6, 1);

        emailService.sendAccessExtendedEmail("user@test.com", "Іванка", "Java 101", expirationDate);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/access-extended"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        assertThat(context.getVariable("userName")).isEqualTo("Іванка");
        assertThat(context.getVariable("courseName")).isEqualTo("Java 101");
        assertThat(context.getVariable("expirationDate")).isEqualTo(expirationDate);
        assertThat(context.getVariable("courseUrl").toString())
                .isEqualTo("http://localhost:5173/dashboard/my-courses");
    }

    @Test
    @DisplayName("sendAccessExtendedEmail — намагається відправити листа")
    void sendAccessExtendedEmail_attemptsToSend() {
        emailService.sendAccessExtendedEmail(
                "user@test.com", "Іванка", "Java 101", LocalDate.of(2026, 6, 1));

        verify(javaMailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    @DisplayName("sendAccessExtendedEmail — не викликає jwtUtils")
    void sendAccessExtendedEmail_doesNotUseJwtUtils() {
        emailService.sendAccessExtendedEmail(
                "user@test.com", "Іванка", "Java 101", LocalDate.of(2026, 6, 1));

        verify(jwtUtils, never()).generateMagicToken(anyString());
    }

    // ─────────────────────── resilience ───────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not-an-email", "very.long.email@subdomain.domain.example.com"})
    @DisplayName("sendPasswordResetEmail — не кидає виключень для різних форматів email")
    void sendPasswordResetEmail_variousEmailFormats_neverThrows(String email) {
        assertThatCode(() ->
                emailService.sendPasswordResetEmail(email, "Тест", "http://link"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not-an-email", "very.long.email@subdomain.domain.example.com"})
    @DisplayName("sendWelcomeEmail — не кидає виключень для різних форматів email")
    void sendWelcomeEmail_variousEmailFormats_neverThrows(String email) {
        when(jwtUtils.generateMagicToken(email)).thenReturn("token-for-" + email);

        assertThatCode(() -> emailService.sendWelcomeEmail(email, "Тест"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendHtmlEmail — обробляє виключення під час відправки")
    void sendHtmlEmail_handlesExceptionGracefully() {
        doThrow(new org.springframework.mail.MailSendException("Connection failed"))
                .when(javaMailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        assertThatCode(() -> emailService.sendPasswordResetEmail("user@test.com", "Іванка", "http://reset"))
                .doesNotThrowAnyException();

        verify(javaMailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }
}