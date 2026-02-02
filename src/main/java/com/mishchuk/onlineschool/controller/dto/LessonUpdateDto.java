package com.mishchuk.onlineschool.controller.dto;

public record LessonUpdateDto(
        String name,
        String description,
        String videoUrl,
        Integer durationMinutes) {
}
