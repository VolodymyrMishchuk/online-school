package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    @Mapping(target = "isEnrolled", constant = "false")
    @Mapping(target = "enrolledAt", ignore = true)
    CourseDto toDto(CourseEntity entity);

    CourseEntity toEntity(CourseCreateDto dto);

    void updateEntityFromDto(CourseUpdateDto dto, @MappingTarget CourseEntity entity);
}
