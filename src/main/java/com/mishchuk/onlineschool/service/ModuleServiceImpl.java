package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.controller.dto.ModuleCreateDto;
import com.mishchuk.onlineschool.controller.dto.ModuleDto;
import com.mishchuk.onlineschool.controller.dto.ModuleUpdateDto;
import com.mishchuk.onlineschool.mapper.ModuleMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.LessonRepository;
import com.mishchuk.onlineschool.repository.ModuleRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.LessonEntity;
import com.mishchuk.onlineschool.repository.entity.ModuleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return moduleRepository.findAll().stream()
                .map(moduleMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonDto> getModuleLessons(java.util.UUID moduleId) {
        return lessonService.getLessonsByModule(moduleId);
    }

    @Override
    @Transactional
    public void updateModule(java.util.UUID id, ModuleUpdateDto dto) {
        ModuleEntity entity = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found"));
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
        moduleRepository.deleteById(id);
    }
}
