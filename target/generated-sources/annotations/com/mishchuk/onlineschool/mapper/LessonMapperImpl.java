package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.CreatedByDto;
import com.mishchuk.onlineschool.controller.dto.LessonCreateDto;
import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.controller.dto.LessonUpdateDto;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.LessonEntity;
import com.mishchuk.onlineschool.repository.entity.ModuleEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-29T19:02:08+0200",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class LessonMapperImpl implements LessonMapper {

    @Override
    public LessonDto toDto(LessonEntity entity) {
        if ( entity == null ) {
            return null;
        }

        String videoUrl = null;
        UUID moduleId = null;
        String moduleName = null;
        String courseName = null;
        UUID id = null;
        String name = null;
        String description = null;
        Integer durationMinutes = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        videoUrl = entity.getVideoUrl();
        moduleId = entityModuleId( entity );
        moduleName = entityModuleName( entity );
        courseName = entityModuleCourseName( entity );
        id = entity.getId();
        name = entity.getName();
        description = entity.getDescription();
        durationMinutes = entity.getDurationMinutes();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        Integer filesCount = entity.getFiles() != null ? entity.getFiles().size() : 0;
        CreatedByDto createdBy = entity.getCreatedBy() != null ? new com.mishchuk.onlineschool.controller.dto.CreatedByDto(entity.getCreatedBy().getId(), entity.getCreatedBy().getFirstName(), entity.getCreatedBy().getLastName(), entity.getCreatedBy().getEmail()) : null;

        LessonDto lessonDto = new LessonDto( id, moduleId, name, description, videoUrl, durationMinutes, moduleName, courseName, filesCount, createdAt, updatedAt, createdBy );

        return lessonDto;
    }

    @Override
    public LessonEntity toEntity(LessonCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        LessonEntity lessonEntity = new LessonEntity();

        lessonEntity.setDescription( dto.description() );
        lessonEntity.setDurationMinutes( dto.durationMinutes() );
        lessonEntity.setName( dto.name() );
        lessonEntity.setVideoUrl( dto.videoUrl() );

        return lessonEntity;
    }

    @Override
    public void updateEntity(LessonEntity entity, LessonUpdateDto dto) {
        if ( dto == null ) {
            return;
        }

        if ( dto.description() != null ) {
            entity.setDescription( dto.description() );
        }
        if ( dto.durationMinutes() != null ) {
            entity.setDurationMinutes( dto.durationMinutes() );
        }
        if ( dto.name() != null ) {
            entity.setName( dto.name() );
        }
        if ( dto.videoUrl() != null ) {
            entity.setVideoUrl( dto.videoUrl() );
        }
    }

    private UUID entityModuleId(LessonEntity lessonEntity) {
        if ( lessonEntity == null ) {
            return null;
        }
        ModuleEntity module = lessonEntity.getModule();
        if ( module == null ) {
            return null;
        }
        UUID id = module.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String entityModuleName(LessonEntity lessonEntity) {
        if ( lessonEntity == null ) {
            return null;
        }
        ModuleEntity module = lessonEntity.getModule();
        if ( module == null ) {
            return null;
        }
        String name = module.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String entityModuleCourseName(LessonEntity lessonEntity) {
        if ( lessonEntity == null ) {
            return null;
        }
        ModuleEntity module = lessonEntity.getModule();
        if ( module == null ) {
            return null;
        }
        CourseEntity course = module.getCourse();
        if ( course == null ) {
            return null;
        }
        String name = course.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
