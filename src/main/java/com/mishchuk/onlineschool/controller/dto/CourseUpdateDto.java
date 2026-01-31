package com.mishchuk.onlineschool.controller.dto;

public record CourseUpdateDto(
        String name,
        String description,
        String status) {
}
