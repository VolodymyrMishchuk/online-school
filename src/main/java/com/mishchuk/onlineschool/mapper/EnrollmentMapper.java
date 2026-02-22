package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.EnrollmentCreateDto;
import com.mishchuk.onlineschool.controller.dto.EnrollmentDto;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseName", source = "course.name")
    EnrollmentDto toDto(EnrollmentEntity entity);

    @Mapping(target = "student.id", source = "studentId")
    @Mapping(target = "course.id", source = "courseId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    EnrollmentEntity toEntity(EnrollmentCreateDto dto);
}
