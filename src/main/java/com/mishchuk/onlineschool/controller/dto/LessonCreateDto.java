package com.mishchuk.onlineschool.controller.dto;

import java.util.UUID;

public record LessonCreateDto(
        UUID moduleId,
        String name,
        String description,
        String videoUrl,
        Integer durationMinutes) {
}
