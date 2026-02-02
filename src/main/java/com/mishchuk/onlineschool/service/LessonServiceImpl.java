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

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final LessonMapper lessonMapper;

    @Override
    @Transactional
    public void createLesson(LessonCreateDto dto) {
        ModuleEntity module = moduleRepository.findById(dto.moduleId())
                .orElseThrow(() -> new RuntimeException("Module not found"));

        LessonEntity entity = lessonMapper.toEntity(dto);
        entity.setModule(module);
        lessonRepository.save(entity);
    }

    @Override
    public Optional<LessonDto> getLesson(UUID id) {
        return lessonRepository.findById(id)
                .map(lessonMapper::toDto);
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
        lessonMapper.updateEntity(entity, dto);
        lessonRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteLesson(UUID id) {
        lessonRepository.deleteById(id);
    }
}
