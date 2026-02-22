package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.controller.dto.ModuleCreateDto;
import com.mishchuk.onlineschool.controller.dto.ModuleDto;
import com.mishchuk.onlineschool.controller.dto.ModuleUpdateDto;
import com.mishchuk.onlineschool.mapper.ModuleMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.LessonRepository;
import com.mishchuk.onlineschool.repository.ModuleRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.LessonEntity;
import com.mishchuk.onlineschool.repository.entity.ModuleEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final ModuleMapper moduleMapper;
    private final LessonService lessonService;
    private final LessonRepository lessonRepository;
    private final PersonRepository personRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public void createModule(ModuleCreateDto dto) {
        // Load the course from database if provided
        CourseEntity course = null;
        if (dto.courseId() != null) {
            course = courseRepository.findById(dto.courseId())
                    .orElseThrow(() -> new RuntimeException("Course not found: " + dto.courseId()));
        }

        ModuleEntity entity = moduleMapper.toEntity(dto);
        entity.setCourse(course);

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        personRepository.findByEmail(userEmail).ifPresent(entity::setCreatedBy);

        ModuleEntity savedModule = moduleRepository.save(entity);

        // Assign lessons to this module if provided
        if (dto.lessonIds() != null && !dto.lessonIds().isEmpty()) {
            for (java.util.UUID lessonId : dto.lessonIds()) {
                LessonEntity lesson = lessonRepository.findById(lessonId)
                        .orElseThrow(() -> new RuntimeException("Lesson not found: " + lessonId));
                lesson.setModule(savedModule);
                lessonRepository.save(lesson);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModuleDto> getModule(java.util.UUID id) {
        return moduleRepository.findById(id)
                .map(moduleMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleDto> getAllModules() {
        return getAllModules(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleDto> getAllModules(java.util.UUID courseId) {
        List<ModuleEntity> entities;
        if (courseId != null) {
            entities = moduleRepository.findByCourseId(courseId);
        } else {
            entities = moduleRepository.findAll();
        }
        return entities.stream()
                .map(moduleMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonDto> getModuleLessons(java.util.UUID moduleId) {
        // Security Check: Ensure user has access to this module's course
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<PersonEntity> userOpt = personRepository.findByEmail(userEmail);

        boolean isAccessDenied = true;

        if (userOpt.isPresent()) {
            PersonEntity user = userOpt.get();
            // Admins have full access
            if (user.getRole() == PersonRole.ADMIN || user.getRole() == PersonRole.FAKE_ADMIN) {
                isAccessDenied = false;
            } else {
                // For regular users, check enrollment validity
                ModuleEntity module = moduleRepository.findById(moduleId)
                        .orElseThrow(() -> new RuntimeException("Module not found"));

                if (module.getCourse() != null) {
                    Optional<EnrollmentEntity> enrollmentOpt = enrollmentRepository
                            .findByStudentIdAndCourseId(user.getId(), module.getCourse().getId());

                    if (enrollmentOpt.isPresent()) {
                        EnrollmentEntity enrollment = enrollmentOpt.get();

                        // Check if blocked
                        boolean isBlocked = "BLOCKED".equals(enrollment.getStatus());
                        boolean isExpired = false;

                        if (enrollment.getExpiresAt() != null) {
                            if (OffsetDateTime.now().isAfter(enrollment.getExpiresAt())) {
                                isExpired = true;
                            }
                        } else if (module.getCourse().getAccessDuration() != null) {
                            OffsetDateTime expirationDate = enrollment.getCreatedAt()
                                    .plusDays(module.getCourse().getAccessDuration());
                            if (OffsetDateTime.now().isAfter(expirationDate)) {
                                isExpired = true;
                            }
                        }

                        if (!isBlocked && !isExpired) {
                            isAccessDenied = false;
                        }
                    }
                }
            }
        }

        List<LessonDto> lessons = lessonService.getLessonsByModule(moduleId);

        if (isAccessDenied) {
            // Scrub sensitive data (videoUrl, filesCount) but return structure
            return lessons.stream()
                    .map(lesson -> new LessonDto(
                            lesson.id(),
                            lesson.moduleId(),
                            lesson.name(),
                            lesson.description(),
                            null, // Scrubbed videoUrl
                            lesson.durationMinutes(),
                            lesson.moduleName(),
                            lesson.courseName(),
                            0, // Scrubbed filesCount
                            lesson.createdAt(),
                            lesson.updatedAt(),
                            null)) // Scrubbed createdBy - though we could pass it
                    .toList();
        }

        return lessons;
    }

    @Override
    @Transactional
    public void updateModule(java.util.UUID id, ModuleUpdateDto dto) {
        ModuleEntity entity = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found"));

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        PersonEntity currentUser = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getRole() == PersonRole.FAKE_ADMIN) {
            if (entity.getCreatedBy() == null || !entity.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "FAKE_ADMIN can only modify their own entities.");
            }
        }

        moduleMapper.updateEntity(entity, dto);
        moduleRepository.save(entity);

        // Update lesson assignments if provided
        if (dto.lessonIds() != null) {
            // First, unassign all current lessons from this module
            List<LessonEntity> currentLessons = lessonRepository.findByModuleId(id);
            for (LessonEntity lesson : currentLessons) {
                lesson.setModule(null);
                lessonRepository.save(lesson);
            }

            // Then assign the new lessons
            for (java.util.UUID lessonId : dto.lessonIds()) {
                LessonEntity lesson = lessonRepository.findById(lessonId)
                        .orElseThrow(() -> new RuntimeException("Lesson not found: " + lessonId));
                lesson.setModule(entity);
                lessonRepository.save(lesson);
            }
        }
    }

    @Override
    @Transactional
    public void deleteModule(java.util.UUID id) {
        ModuleEntity entity = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found"));

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        PersonEntity currentUser = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getRole() == PersonRole.FAKE_ADMIN) {
            if (entity.getCreatedBy() == null || !entity.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "FAKE_ADMIN can only delete their own entities.");
            }
        }

        moduleRepository.deleteById(id);
    }
}
