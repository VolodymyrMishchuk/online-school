package com.mishchuk.onlineschool.controller.dto;

public record ModuleCreateDto(
                String name,
                java.util.UUID courseId,
                String description) {
}
