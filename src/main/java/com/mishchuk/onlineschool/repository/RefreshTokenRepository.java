package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByToken(String token);

    void deleteByPersonId(UUID personId);

    void deleteByExpiryDateBefore(OffsetDateTime now);
}
