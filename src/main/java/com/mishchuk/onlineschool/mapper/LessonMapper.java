package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.LessonCreateDto;
import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.controller.dto.LessonUpdateDto;
import com.mishchuk.onlineschool.repository.entity.LessonEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "videoUrl", source = "videoUrl")
    @Mapping(target = "moduleId", source = "module.id")
    @Mapping(target = "moduleName", source = "module.name")
    @Mapping(target = "courseName", source = "module.course.name")
    @Mapping(target = "filesCount", expression = "java(entity.getFiles() != null ? entity.getFiles().size() : 0)")
    @Mapping(target = "createdBy", expression = "java(entity.getCreatedBy() != null ? new com.mishchuk.onlineschool.controller.dto.CreatedByDto(entity.getCreatedBy().getId(), entity.getCreatedBy().getFirstName(), entity.getCreatedBy().getLastName(), entity.getCreatedBy().getEmail()) : null)")
    LessonDto toDto(LessonEntity entity);

    @Mapping(target = "module", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "files", ignore = true)
    LessonEntity toEntity(LessonCreateDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "module", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "files", ignore = true)
    void updateEntity(@MappingTarget LessonEntity entity, LessonUpdateDto dto);
}
