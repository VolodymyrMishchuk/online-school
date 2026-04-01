package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.repository.RefreshTokenRepository;
import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;
import com.mishchuk.onlineschool.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    @Mock private JwtUtils jwtUtils;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        // 604800000 ms = 7 days (default refresh token expiration)
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 604800000L);
    }

    // ─────────────────────── verifyExpiration ───────────────────────

    @Test
    @DisplayName("verifyExpiration — повертає токен якщо він дійсний (не прострочений)")
    void verifyExpiration_valid_returnsToken() {
        RefreshTokenEntity token = buildToken(UUID.randomUUID(), "valid.token",
                OffsetDateTime.now().plusDays(7));

        RefreshTokenEntity result = refreshTokenService.verifyExpiration(token);

        assertThat(result).isSameAs(token);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("verifyExpiration — кидає RuntimeException і видаляє прострочений токен")
    void verifyExpiration_expired_throwsAndDeletes() {
        RefreshTokenEntity token = buildToken(UUID.randomUUID(), "expired.token",
                OffsetDateTime.now().minusDays(1));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");

        // Токен має бути видалений з репозиторію при проставлені expired
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    @DisplayName("verifyExpiration — токен прострочений рівно зараз (граничне значення)")
    void verifyExpiration_expiredRightNow_throws() {
        RefreshTokenEntity token = buildToken(UUID.randomUUID(), "borderline.token",
                OffsetDateTime.now().minusSeconds(1));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(RuntimeException.class);

        verify(refreshTokenRepository).delete(token);
    }

    // ─────────────────────── findByToken ───────────────────────

    @Test
    @DisplayName("findByToken — повертає токен якщо знайдений в репозиторії")
    void findByToken_found_returnsEntity() {
        String tokenStr = "valid.refresh.token";
        RefreshTokenEntity entity = buildToken(UUID.randomUUID(), tokenStr,
                OffsetDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(entity));

        RefreshTokenEntity result = refreshTokenService.findByToken(tokenStr);

        assertThat(result).isSameAs(entity);
        assertThat(result.getToken()).isEqualTo(tokenStr);
    }

    @Test
    @DisplayName("findByToken — кидає ResourceNotFoundException якщо токен не знайдено")
    void findByToken_notFound_throwsResourceNotFound() {
        when(refreshTokenRepository.findByToken("invalid.token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.findByToken("invalid.token"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Refresh token not found");
    }

    // ─────────────────────── deleteByPersonId ───────────────────────

    @Test
    @DisplayName("deleteByPersonId — делегує видалення репозиторію з правильним UUID")
    void deleteByPersonId_delegatesWithCorrectId() {
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        refreshTokenService.deleteByPersonId(personId);

        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(refreshTokenRepository).deleteByPersonId(captor.capture());
        assertThat(captor.getValue()).isEqualTo(personId);
    }

    @Test
    @DisplayName("deleteByPersonId — викликається рівно один раз")
    void deleteByPersonId_calledExactlyOnce() {
        UUID personId = UUID.randomUUID();

        refreshTokenService.deleteByPersonId(personId);
        refreshTokenService.deleteByPersonId(personId);

        verify(refreshTokenRepository, times(2)).deleteByPersonId(personId);
    }

    // ─────────────────────── createRefreshToken ───────────────────────

    @Test
    @DisplayName("createRefreshToken — зберігає токен з правильним personId та expiresAt")
    void createRefreshToken_savesTokenWithCorrectFields() {
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        RefreshTokenEntity saved = buildToken(personId, "new.refresh.token",
                OffsetDateTime.now().plusDays(7));

        when(jwtUtils.generateRefreshToken(personId)).thenReturn("new.refresh.token");
        when(refreshTokenRepository.save(any())).thenReturn(saved);

        RefreshTokenEntity result = refreshTokenService.createRefreshToken(personId);

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshTokenEntity captured = captor.getValue();
        assertThat(captured.getPersonId()).isEqualTo(personId);
        assertThat(captured.getToken()).isEqualTo("new.refresh.token");
        assertThat(captured.getExpiryDate()).isAfter(OffsetDateTime.now());
        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("createRefreshToken — викликає generateRefreshToken з правильним personId")
    void createRefreshToken_callsJwtUtilsWithCorrectPersonId() {
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        when(jwtUtils.generateRefreshToken(personId)).thenReturn("generated.token");
        when(refreshTokenRepository.save(any())).thenReturn(buildToken(personId, "generated.token",
                OffsetDateTime.now().plusDays(7)));

        refreshTokenService.createRefreshToken(personId);

        verify(jwtUtils).generateRefreshToken(personId);
    }

    // ─────────────────────── rotateRefreshToken ───────────────────────

    @Test
    @DisplayName("rotateRefreshToken — happy path: видаляє старий і створює новий токен")
    void rotateRefreshToken_validToken_deletesOldAndCreatesNew() {
        UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String oldTokenStr = "old.valid.token";
        RefreshTokenEntity oldToken = buildToken(personId, oldTokenStr,
                OffsetDateTime.now().plusDays(7));
        RefreshTokenEntity newToken = buildToken(personId, "new.rotated.token",
                OffsetDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByToken(oldTokenStr)).thenReturn(Optional.of(oldToken));
        when(jwtUtils.generateRefreshToken(personId)).thenReturn("new.rotated.token");
        when(refreshTokenRepository.save(any())).thenReturn(newToken);

        RefreshTokenEntity result = refreshTokenService.rotateRefreshToken(oldTokenStr);

        // Старий токен має бути видалений
        verify(refreshTokenRepository).delete(oldToken);
        // Новий токен має бути збережений
        verify(refreshTokenRepository).save(any());
        assertThat(result).isSameAs(newToken);
        assertThat(result.getToken()).isEqualTo("new.rotated.token");
    }

    @Test
    @DisplayName("rotateRefreshToken — кидає RuntimeException якщо старий токен прострочений")
    void rotateRefreshToken_expiredToken_throws() {
        String oldTokenStr = "old.expired.token";
        RefreshTokenEntity oldToken = buildToken(UUID.randomUUID(), oldTokenStr,
                OffsetDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken(oldTokenStr)).thenReturn(Optional.of(oldToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(oldTokenStr))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(oldToken);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("rotateRefreshToken — кидає ResourceNotFoundException якщо старий токен не знайдено")
    void rotateRefreshToken_tokenNotFound_throws() {
        when(refreshTokenRepository.findByToken("ghost.token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("ghost.token"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    // ─────────────────────── helpers ───────────────────────

    private RefreshTokenEntity buildToken(UUID personId, String token, OffsetDateTime expiry) {
        return RefreshTokenEntity.builder()
                .token(token)
                .personId(personId)
                .expiryDate(expiry)
                .build();
    }
}
