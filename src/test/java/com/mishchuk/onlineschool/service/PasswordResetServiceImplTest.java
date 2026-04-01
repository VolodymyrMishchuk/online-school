package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.repository.PasswordResetTokenRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.PasswordResetTokenEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock private PersonRepository personRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    @BeforeEach
    void setFrontendUrl() {
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:5173");
    }

    private PasswordResetTokenEntity unexpiredToken(PersonEntity user) {
        return new PasswordResetTokenEntity(user, UUID.randomUUID().toString(),
                OffsetDateTime.now().plusMinutes(15));
    }

    private PasswordResetTokenEntity expiredToken(PersonEntity user) {
        return new PasswordResetTokenEntity(user, UUID.randomUUID().toString(),
                OffsetDateTime.now().minusMinutes(30));
    }

    // ─────────────────────── initiatePasswordReset ───────────────────────

    @Test
    @DisplayName("initiatePasswordReset — створює токен і надсилає email якщо email знайдено")
    void initiatePasswordReset_found_sendsEmail() {
        PersonEntity user = new PersonEntity();
        user.setEmail("user@test.com");
        user.setFirstName("Іванка");

        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        passwordResetService.initiatePasswordReset("user@test.com");

        verify(tokenRepository).save(any(PasswordResetTokenEntity.class));
        verify(emailService).sendPasswordResetEmail(eq("user@test.com"), eq("Іванка"), anyString());
    }

    @Test
    @DisplayName("initiatePasswordReset — нічого не робить якщо email не знайдено (без розкриття)")
    void initiatePasswordReset_userNotFound_doesNothing() {
        when(personRepository.findByEmail("noone@test.com")).thenReturn(Optional.empty());

        passwordResetService.initiatePasswordReset("noone@test.com");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    // ─────────────────────── resetPassword ───────────────────────

    @Test
    @DisplayName("resetPassword — успішна зміна пароля")
    void resetPassword_success() {
        PersonEntity user = new PersonEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");

        PasswordResetTokenEntity tokenEntity = unexpiredToken(user);
        String tokenStr = tokenEntity.getToken();

        when(tokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(tokenEntity));
        when(personRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNew");

        passwordResetService.resetPassword(tokenStr, "newPass");

        assertThat(user.getPassword()).isEqualTo("encodedNew");
        verify(personRepository).saveAndFlush(user);
        assertThat(tokenEntity.isUsed()).isTrue();
    }

    @Test
    @DisplayName("resetPassword — кидає IllegalArgumentException якщо токен вже використано")
    void resetPassword_alreadyUsed_throws() {
        PersonEntity user = new PersonEntity();
        PasswordResetTokenEntity tokenEntity = unexpiredToken(user);
        tokenEntity.setUsed(true);
        String tokenStr = tokenEntity.getToken();

        when(tokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(tokenEntity));

        assertThatThrownBy(() -> passwordResetService.resetPassword(tokenStr, "newPass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    @DisplayName("resetPassword — кидає IllegalArgumentException якщо токен прострочено")
    void resetPassword_expired_throws() {
        PersonEntity user = new PersonEntity();
        PasswordResetTokenEntity tokenEntity = expiredToken(user);
        String tokenStr = tokenEntity.getToken();

        when(tokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(tokenEntity));

        assertThatThrownBy(() -> passwordResetService.resetPassword(tokenStr, "newPass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("resetPassword — кидає IllegalArgumentException якщо токен не знайдено")
    void resetPassword_tokenNotFound_throws() {
        when(tokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("invalid", "newPass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");
    }
}
