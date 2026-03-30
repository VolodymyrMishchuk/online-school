package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.CreatedByDto;
import com.mishchuk.onlineschool.controller.dto.ModuleCreateDto;
import com.mishchuk.onlineschool.controller.dto.ModuleDto;
import com.mishchuk.onlineschool.controller.dto.ModuleUpdateDto;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
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
public class ModuleMapperImpl implements ModuleMapper {

    @Override
    public ModuleDto toDto(ModuleEntity entity) {
        if ( entity == null ) {
            return null;
        }

        UUID courseId = null;
        UUID id = null;
        String name = null;
        String description = null;
        String status = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        courseId = entityCourseId( entity );
        id = entity.getId();
        name = entity.getName();
        description = entity.getDescription();
        status = entity.getStatus();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        Integer lessonsNumber = entity.getLessons() != null ? entity.getLessons().size() : 0;
        Integer durationMinutes = entity.getLessons() != null ? entity.getLessons().stream().mapToInt(l -> l.getDurationMinutes() != null ? l.getDurationMinutes() : 0).sum() : 0;
        CreatedByDto createdBy = entity.getCreatedBy() != null ? new com.mishchuk.onlineschool.controller.dto.CreatedByDto(entity.getCreatedBy().getId(), entity.getCreatedBy().getFirstName(), entity.getCreatedBy().getLastName(), entity.getCreatedBy().getEmail()) : null;

        ModuleDto moduleDto = new ModuleDto( id, name, courseId, description, lessonsNumber, durationMinutes, status, createdAt, updatedAt, createdBy );

        return moduleDto;
    }

    @Override
    public ModuleEntity toEntity(ModuleCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        ModuleEntity moduleEntity = new ModuleEntity();

        moduleEntity.setCourse( moduleCreateDtoToCourseEntity( dto ) );
        moduleEntity.setDescription( dto.description() );
        moduleEntity.setName( dto.name() );

        return moduleEntity;
    }

    @Override
    public void updateEntity(ModuleEntity entity, ModuleUpdateDto dto) {
        if ( dto == null ) {
            return;
        }

        if ( dto.description() != null ) {
            entity.setDescription( dto.description() );
        }
        if ( dto.name() != null ) {
            entity.setName( dto.name() );
        }
        if ( dto.status() != null ) {
            entity.setStatus( dto.status() );
        }
    }

    private UUID entityCourseId(ModuleEntity moduleEntity) {
        if ( moduleEntity == null ) {
            return null;
        }
        CourseEntity course = moduleEntity.getCourse();
        if ( course == null ) {
            return null;
        }
        UUID id = course.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected CourseEntity moduleCreateDtoToCourseEntity(ModuleCreateDto moduleCreateDto) {
        if ( moduleCreateDto == null ) {
            return null;
        }

        CourseEntity courseEntity = new CourseEntity();

        courseEntity.setId( moduleCreateDto.courseId() );

        return courseEntity;
    }
}
