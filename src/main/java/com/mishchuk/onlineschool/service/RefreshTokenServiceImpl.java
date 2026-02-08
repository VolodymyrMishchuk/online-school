package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.repository.RefreshTokenRepository;
import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;
import com.mishchuk.onlineschool.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;

    @Value("${application.security.jwt.refresh-token-expiration:604800000}")
    private Long refreshTokenDurationMs;

    @Override
    @Transactional
    public RefreshTokenEntity createRefreshToken(UUID personId) {
        log.info("Creating refresh token for person: {}", personId);

        String tokenValue = jwtUtils.generateRefreshToken(personId);

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .token(tokenValue)
                .personId(personId)
                .expiryDate(OffsetDateTime.now().plusSeconds(refreshTokenDurationMs / 1000))
                .build();

        RefreshTokenEntity saved = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created with id: {}", saved.getId());

        return saved;
    }

    @Override
    public RefreshTokenEntity verifyExpiration(RefreshTokenEntity token) {
        if (token.getExpiryDate().isBefore(OffsetDateTime.now())) {
            log.warn("Refresh token expired: {}", token.getId());
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please sign in again.");
        }
        return token;
    }

    @Override
    public RefreshTokenEntity findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));
    }

    @Override
    @Transactional
    public void deleteByPersonId(UUID personId) {
        log.info("Deleting refresh tokens for person: {}", personId);
        refreshTokenRepository.deleteByPersonId(personId);
    }

    @Override
    @Transactional
    public RefreshTokenEntity rotateRefreshToken(String oldToken) {
        log.info("Rotating refresh token");

        RefreshTokenEntity oldRefreshToken = findByToken(oldToken);
        verifyExpiration(oldRefreshToken);

        // Delete old token
        refreshTokenRepository.delete(oldRefreshToken);

        // Create new token
        return createRefreshToken(oldRefreshToken.getPersonId());
    }
}
