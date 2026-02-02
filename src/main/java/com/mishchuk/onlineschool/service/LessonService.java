package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.LessonCreateDto;
import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.controller.dto.LessonUpdateDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonService {
    void createLesson(LessonCreateDto dto);

    Optional<LessonDto> getLesson(UUID id);

    List<LessonDto> getLessonsByModule(UUID moduleId);

    void updateLesson(UUID id, LessonUpdateDto dto);

    void deleteLesson(UUID id);
}
