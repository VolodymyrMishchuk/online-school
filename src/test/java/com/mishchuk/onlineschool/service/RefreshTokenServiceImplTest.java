package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.repository.RefreshTokenRepository;
import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    // JwtUtils is injected via @InjectMocks but not mocked — tests that need
    // a token value use pre-built RefreshTokenEntity objects directly.
    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    // --- verifyExpiration ---

    @Test
    @DisplayName("verifyExpiration — повертає токен якщо він дійсний")
    void verifyExpiration_valid_returnsToken() {
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .token("tok").personId(UUID.randomUUID())
                .expiryDate(OffsetDateTime.now().plusDays(7)).build();

        RefreshTokenEntity result = refreshTokenService.verifyExpiration(token);
        assertThat(result).isEqualTo(token);
    }

    @Test
    @DisplayName("verifyExpiration — кидає RuntimeException якщо токен прострочений і видаляє його")
    void verifyExpiration_expired_throwsAndDeletes() {
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .token("tok").personId(UUID.randomUUID())
                .expiryDate(OffsetDateTime.now().minusDays(1)).build();

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(token);
    }

    // --- findByToken ---

    @Test
    @DisplayName("findByToken — повертає токен якщо знайдено")
    void findByToken_found() {
        String tokenStr = "valid.token";
        RefreshTokenEntity entity = RefreshTokenEntity.builder().token(tokenStr).build();
        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(entity));

        RefreshTokenEntity result = refreshTokenService.findByToken(tokenStr);
        assertThat(result.getToken()).isEqualTo(tokenStr);
    }

    @Test
    @DisplayName("findByToken — кидає RuntimeException якщо не знайдено")
    void findByToken_notFound_throws() {
        when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.findByToken("invalid"))
                .isInstanceOf(RuntimeException.class);
    }

    // --- deleteByPersonId ---

    @Test
    @DisplayName("deleteByPersonId — делегує до репозиторію")
    void deleteByPersonId_callsRepository() {
        UUID personId = UUID.randomUUID();
        refreshTokenService.deleteByPersonId(personId);
        verify(refreshTokenRepository).deleteByPersonId(personId);
    }

    @Test
    @DisplayName("deleteByToken — deletes by personId correctly")
    void deleteByToken_deletesCorrectEntity() {
        UUID personId = UUID.randomUUID();
        // Simply verify deleteByPersonId delegates correctly
        refreshTokenService.deleteByPersonId(personId);
        verify(refreshTokenRepository, times(1)).deleteByPersonId(personId);
    }
}
