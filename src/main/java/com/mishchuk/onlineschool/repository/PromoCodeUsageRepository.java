package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.PromoCodeUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsageEntity, UUID> {
    Optional<PromoCodeUsageEntity> findByPromoCodeIdAndPersonId(UUID promoCodeId, UUID personId);
    boolean existsByPromoCodeIdAndPersonId(UUID promoCodeId, UUID personId);
}
