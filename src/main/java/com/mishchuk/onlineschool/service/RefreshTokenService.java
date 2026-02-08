package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;

import java.util.UUID;

public interface RefreshTokenService {

    RefreshTokenEntity createRefreshToken(UUID personId);

    RefreshTokenEntity verifyExpiration(RefreshTokenEntity token);

    RefreshTokenEntity findByToken(String token);

    void deleteByPersonId(UUID personId);

    RefreshTokenEntity rotateRefreshToken(String oldToken);
}
