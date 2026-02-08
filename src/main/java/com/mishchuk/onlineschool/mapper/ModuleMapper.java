package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.ModuleCreateDto;
import com.mishchuk.onlineschool.controller.dto.ModuleDto;
import com.mishchuk.onlineschool.controller.dto.ModuleUpdateDto;
import com.mishchuk.onlineschool.repository.entity.ModuleEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ModuleMapper {
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "lessonsNumber", expression = "java(entity.getLessons() != null ? entity.getLessons().size() : 0)")
    @Mapping(target = "durationMinutes", expression = "java(entity.getLessons() != null ? entity.getLessons().stream().mapToInt(l -> l.getDurationMinutes() != null ? l.getDurationMinutes() : 0).sum() : 0)")
    ModuleDto toDto(ModuleEntity entity);

    @Mapping(target = "course.id", source = "courseId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lessonsNumber", ignore = true)
    ModuleEntity toEntity(ModuleCreateDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget ModuleEntity entity, ModuleUpdateDto dto);
}
