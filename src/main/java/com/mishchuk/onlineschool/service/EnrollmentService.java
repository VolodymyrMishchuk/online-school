package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.EnrollmentCreateDto;
import com.mishchuk.onlineschool.controller.dto.EnrollmentDto;
import java.util.List;
import java.util.UUID;

public interface EnrollmentService {
    void createEnrollment(EnrollmentCreateDto dto);

    List<EnrollmentDto> getEnrollmentsByStudent(UUID studentId);

    List<EnrollmentDto> getAllEnrollments();
}
