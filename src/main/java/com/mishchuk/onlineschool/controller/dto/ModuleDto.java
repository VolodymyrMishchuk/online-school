package com.mishchuk.onlineschool.controller.dto;

import java.time.OffsetDateTime;

public record ModuleDto(
                java.util.UUID id,
                String name,
                String course,
                String description,
                Integer lessonsNumber,
                String status,
                OffsetDateTime createdAt,
                OffsetDateTime updatedAt) {
}
