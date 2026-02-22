package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.LessonCreateDto;
import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.controller.dto.LessonUpdateDto;
import com.mishchuk.onlineschool.mapper.LessonMapper;
import com.mishchuk.onlineschool.repository.LessonRepository;
import com.mishchuk.onlineschool.repository.ModuleRepository;
import com.mishchuk.onlineschool.repository.entity.LessonEntity;
import com.mishchuk.onlineschool.repository.entity.ModuleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final LessonMapper lessonMapper;
    private final PersonRepository personRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public LessonDto createLesson(LessonCreateDto dto) {
        LessonEntity entity = lessonMapper.toEntity(dto);

        // Module is optional - only set if provided
        if (dto.moduleId() != null) {
            ModuleEntity module = moduleRepository.findById(dto.moduleId())
                    .orElseThrow(() -> new RuntimeException("Module not found"));
            entity.setModule(module);
        }

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        personRepository.findByEmail(userEmail).ifPresent(entity::setCreatedBy);

        LessonEntity savedLesson = lessonRepository.save(entity);
        return lessonMapper.toDto(savedLesson);
    }

    @Override
    public Optional<LessonDto> getLesson(UUID id) {
        return lessonRepository.findById(id).map(lesson -> {
            boolean isAccessDenied = !hasAccessToLessonContent(lesson);
            LessonDto dto = lessonMapper.toDto(lesson);

            if (isAccessDenied) {
                return new LessonDto(
                        dto.id(),
                        dto.moduleId(),
                        dto.name(),
                        dto.description(),
                        null, // Scrubbed videoUrl
                        dto.durationMinutes(),
                        dto.moduleName(),
                        dto.courseName(),
                        dto.filesCount(),
                        dto.createdAt(),
                        dto.updatedAt(),
                        dto.createdBy());
            }

            return dto;
        });
    }

    private boolean hasAccessToLessonContent(LessonEntity lesson) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<PersonEntity> userOpt = personRepository.findByEmail(userEmail);

        if (userOpt.isPresent()) {
            PersonEntity user = userOpt.get();
            if (user.getRole() == PersonRole.ADMIN) {
                return true;
            }

            if (lesson != null && lesson.getModule() != null && lesson.getModule().getCourse() != null) {
                Optional<EnrollmentEntity> enrollmentOpt = enrollmentRepository
                        .findByStudentIdAndCourseId(user.getId(), lesson.getModule().getCourse().getId());

                if (enrollmentOpt.isPresent()) {
                    EnrollmentEntity enrollment = enrollmentOpt.get();

                    if ("BLOCKED".equals(enrollment.getStatus())) {
                        return false;
                    }

                    if (enrollment.getExpiresAt() != null) {
                        if (OffsetDateTime.now().isAfter(enrollment.getExpiresAt())) {
                            return false;
                        }
                    } else if (lesson.getModule().getCourse().getAccessDuration() != null) {
                        OffsetDateTime expirationDate = enrollment.getCreatedAt()
                                .plusDays(lesson.getModule().getCourse().getAccessDuration());
                        if (OffsetDateTime.now().isAfter(expirationDate)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<LessonDto> getAllLessons() {
        return lessonRepository.findAll()
                .stream()
                .map(lessonMapper::toDto)
                .toList();
    }

    @Override
    public List<LessonDto> getUnassignedLessons() {
        return lessonRepository.findByModuleIdIsNull()
                .stream()
                .map(lessonMapper::toDto)
                .toList();
    }

    @Override
    public List<LessonDto> getLessonsByModule(UUID moduleId) {
        return lessonRepository.findByModuleId(moduleId)
                .stream()
                .map(lessonMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void updateLesson(UUID id, LessonUpdateDto dto) {
        LessonEntity entity = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        PersonEntity currentUser = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getRole() == PersonRole.FAKE_ADMIN) {
            if (entity.getCreatedBy() == null || !entity.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "FAKE_ADMIN can only modify their own entities.");
            }
        }

        lessonMapper.updateEntity(entity, dto);
        lessonRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteLesson(UUID id) {
        LessonEntity entity = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        PersonEntity currentUser = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getRole() == PersonRole.FAKE_ADMIN) {
            if (entity.getCreatedBy() == null || !entity.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "FAKE_ADMIN can only delete their own entities.");
            }
        }

        lessonRepository.deleteById(id);
    }
}
