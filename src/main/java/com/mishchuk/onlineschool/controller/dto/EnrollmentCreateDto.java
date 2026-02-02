package com.mishchuk.onlineschool.controller.dto;

import java.util.UUID;

public record EnrollmentCreateDto(
        UUID studentId,
        UUID courseId) {
}
