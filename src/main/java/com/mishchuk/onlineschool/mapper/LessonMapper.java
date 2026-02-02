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

    @Mapping(target = "moduleId", source = "module.id")
    LessonDto toDto(LessonEntity entity);

    @Mapping(target = "module.id", source = "moduleId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LessonEntity toEntity(LessonCreateDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "module", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget LessonEntity entity, LessonUpdateDto dto);
}
