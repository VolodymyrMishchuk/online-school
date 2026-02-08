package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.LessonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<LessonEntity, UUID> {
    java.util.List<LessonEntity> findByModuleId(UUID moduleId);

    java.util.List<LessonEntity> findByModuleIdIsNull();
}
