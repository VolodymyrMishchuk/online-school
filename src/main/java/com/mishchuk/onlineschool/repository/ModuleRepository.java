package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleRepository extends JpaRepository<ModuleEntity, java.util.UUID> {
    java.util.List<ModuleEntity> findByCourseId(java.util.UUID courseId);
}
