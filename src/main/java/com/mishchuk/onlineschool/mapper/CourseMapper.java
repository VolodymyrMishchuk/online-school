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
    @Mapping(target = "modulesNumber", expression = "java(entity.getModules() != null ? entity.getModules().size() : 0)")
    @Mapping(target = "lessonsCount", expression = "java(calculateLessonsCount(entity))")
    @Mapping(target = "durationMinutes", expression = "java(calculateDuration(entity))")
    @Mapping(target = "coverImageUrl", expression = "java(entity.getCoverImage() != null ? \"/api/courses/\" + entity.getId() + \"/cover\" : null)")
    @Mapping(target = "averageColor", source = "coverImage.averageColor")
    @Mapping(target = "createdBy", expression = "java(entity.getCreatedBy() != null ? new com.mishchuk.onlineschool.controller.dto.CreatedByDto(entity.getCreatedBy().getId(), entity.getCreatedBy().getFirstName(), entity.getCreatedBy().getLastName(), entity.getCreatedBy().getEmail()) : null)")
    CourseDto toDto(CourseEntity entity);

    default Integer calculateLessonsCount(CourseEntity entity) {
        if (entity.getModules() == null)
            return 0;
        return entity.getModules().stream()
                .filter(m -> m.getLessons() != null)
                .mapToInt(m -> m.getLessons().size())
                .sum();
    }

    default Integer calculateDuration(CourseEntity entity) {
        if (entity.getModules() == null)
            return 0;
        return entity.getModules().stream()
                .filter(m -> m.getLessons() != null)
                .flatMap(m -> m.getLessons().stream())
                .mapToInt(l -> l.getDurationMinutes() != null ? l.getDurationMinutes() : 0)
                .sum();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modules", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "nextCourse", ignore = true)
    @Mapping(target = "modulesNumber", ignore = true)
    @Mapping(target = "status", constant = "DRAFT") // Default status for new course
    @Mapping(target = "coverImage", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    CourseEntity toEntity(CourseCreateDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modules", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "nextCourse", ignore = true)
    @Mapping(target = "modulesNumber", ignore = true)
    @Mapping(target = "coverImage", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromDto(CourseUpdateDto dto, @MappingTarget CourseEntity entity);
}
