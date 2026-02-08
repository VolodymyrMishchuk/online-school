package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;
import com.mishchuk.onlineschool.mapper.CourseMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final com.mishchuk.onlineschool.repository.ModuleRepository moduleRepository;
    private final com.mishchuk.onlineschool.repository.EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public void createCourse(CourseCreateDto dto) {
        CourseEntity entity = courseMapper.toEntity(dto);
        entity.setStatus("PUBLISHED");

        // Mutual exclusion for discounts
        if (entity.getDiscountAmount() != null && entity.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            entity.setDiscountPercentage(null);
        } else if (entity.getDiscountPercentage() != null && entity.getDiscountPercentage() > 0) {
            entity.setDiscountAmount(null);
        }

        CourseEntity savedCourse = courseRepository.save(entity);

        if (dto.moduleIds() != null && !dto.moduleIds().isEmpty()) {
            List<com.mishchuk.onlineschool.repository.entity.ModuleEntity> modules = moduleRepository
                    .findAllById(dto.moduleIds());
            for (com.mishchuk.onlineschool.repository.entity.ModuleEntity module : modules) {
                module.setCourse(savedCourse);
                moduleRepository.save(module);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseDto> getCourse(java.util.UUID id) {
        return courseRepository.findById(id)
                .map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDto> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(entity -> courseMapper.toDto(entity))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDto> getAllCoursesWithEnrollment(java.util.UUID userId) {
        List<CourseEntity> courses = courseRepository.findAll();

        return courses.stream()
                .map(course -> {
                    CourseDto baseDto = courseMapper.toDto(course);

                    // Check enrollment
                    java.util.Optional<com.mishchuk.onlineschool.repository.entity.EnrollmentEntity> enrollment = enrollmentRepository
                            .findByStudentIdAndCourseId(userId, course.getId());

                    boolean isEnrolled = enrollment.isPresent();
                    java.time.OffsetDateTime enrolledAt = enrollment.map(
                            com.mishchuk.onlineschool.repository.entity.EnrollmentEntity::getCreatedAt).orElse(null);

                    return new CourseDto(
                            baseDto.id(),
                            baseDto.name(),
                            baseDto.description(),
                            baseDto.modulesNumber(),
                            baseDto.status(),
                            baseDto.price(),
                            baseDto.discountAmount(),
                            baseDto.discountPercentage(),
                            baseDto.accessDuration(),
                            baseDto.createdAt(),
                            baseDto.updatedAt(),
                            isEnrolled,
                            enrolledAt,
                            isEnrolled ? enrollment.get().getStatus() : null);
                })
                .toList();
    }

    @Override
    @Transactional
    public void updateCourse(java.util.UUID id, CourseUpdateDto dto) {
        CourseEntity entity = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        courseMapper.updateEntityFromDto(dto, entity);

        // Mutual exclusion for discounts
        if (dto.discountAmount() != null && dto.discountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            entity.setDiscountAmount(dto.discountAmount());
            entity.setDiscountPercentage(null);
        } else if (dto.discountPercentage() != null && dto.discountPercentage() > 0) {
            entity.setDiscountPercentage(dto.discountPercentage());
            entity.setDiscountAmount(null);
        }
        // If both are null/empty in DTO but user wanted to clear them,
        // we might need more explicit logic, but assuming frontend sends 0 or null.
        // If we want to allow clearing both:
        if (dto.discountAmount() == null && dto.discountPercentage() == null) {
            // Do nothing, keep existing or MapStruct handled it?
            // If I want to clear, I should probably send logic for that.
            // For now, let's strictly follow "if one is entered, remove other".
        }

        CourseEntity savedCourse = courseRepository.save(entity);

        if (dto.moduleIds() != null) {
            // Unassign all modules currently assigned to this course
            List<com.mishchuk.onlineschool.repository.entity.ModuleEntity> currentModules = moduleRepository
                    .findByCourseId(id);
            for (com.mishchuk.onlineschool.repository.entity.ModuleEntity module : currentModules) {
                module.setCourse(null);
                moduleRepository.save(module);
            }

            // Assign new modules
            if (!dto.moduleIds().isEmpty()) {
                List<com.mishchuk.onlineschool.repository.entity.ModuleEntity> newModules = moduleRepository
                        .findAllById(dto.moduleIds());
                for (com.mishchuk.onlineschool.repository.entity.ModuleEntity module : newModules) {
                    module.setCourse(savedCourse);
                    moduleRepository.save(module);
                }
            }
        }
    }

    @Override
    @Transactional
    public void deleteCourse(java.util.UUID id) {
        // Unassign modules before deleting (optional, but good practice if cascade
        // isn't set)
        List<com.mishchuk.onlineschool.repository.entity.ModuleEntity> currentModules = moduleRepository
                .findByCourseId(id);
        for (com.mishchuk.onlineschool.repository.entity.ModuleEntity module : currentModules) {
            module.setCourse(null);
            moduleRepository.save(module);
        }
        courseRepository.deleteById(id);
    }
}
