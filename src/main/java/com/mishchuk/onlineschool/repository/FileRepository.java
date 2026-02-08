package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.FileEntity;
import com.mishchuk.onlineschool.repository.entity.LessonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    List<FileEntity> findByRelatedEntityTypeAndRelatedEntityId(
            String entityType,
            UUID entityId);

    List<FileEntity> findByUploadedBy(PersonEntity person);

    // Пошук файлів конкретного уроку
    List<FileEntity> findByLesson(LessonEntity lesson);

    List<FileEntity> findByLessonId(UUID lessonId);

    // Пошук файлів уроку з сортуванням по даті завантаження
    @Query("SELECT f FROM FileEntity f WHERE f.lesson.id = :lessonId ORDER BY f.uploadedAt ASC")
    List<FileEntity> findLessonFilesOrdered(@Param("lessonId") UUID lessonId);
}
