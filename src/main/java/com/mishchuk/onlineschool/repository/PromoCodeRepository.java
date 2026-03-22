package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.PromoCodeEntity;
import com.mishchuk.onlineschool.repository.entity.PromoCodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCodeEntity, UUID>, PromoCodeRepositoryCustom {
    Optional<PromoCodeEntity> findByCodeIgnoreCase(String code);

    List<PromoCodeEntity> findByStatusAndValidFromLessThanEqual(PromoCodeStatus status, LocalDateTime date);
}
