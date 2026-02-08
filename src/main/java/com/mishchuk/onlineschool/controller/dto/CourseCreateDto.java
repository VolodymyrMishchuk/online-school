package com.mishchuk.onlineschool.controller.dto;

public record CourseCreateDto(
                String name,
                String description,
                java.util.List<java.util.UUID> moduleIds) {
}
