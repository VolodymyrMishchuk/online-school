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
    @Mapping(target = "enrollmentStatus", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "nextCourseId", source = "nextCourse.id")
    @Mapping(target = "nextCourseName", source = "nextCourse.name")
    CourseDto toDto(CourseEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modules", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "nextCourse", ignore = true)
    CourseEntity toEntity(CourseCreateDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modules", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "nextCourse", ignore = true)
    void updateEntityFromDto(CourseUpdateDto dto, @MappingTarget CourseEntity entity);
}
