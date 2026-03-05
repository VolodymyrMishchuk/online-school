package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.AppealEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AppealRepository extends JpaRepository<AppealEntity, UUID> {
    Page<AppealEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
