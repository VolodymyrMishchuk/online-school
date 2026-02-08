package com.mishchuk.onlineschool.controller.dto;

public record ModuleUpdateDto(
                String name,
                String description,
                String status,
                java.util.List<java.util.UUID> lessonIds) {
}
