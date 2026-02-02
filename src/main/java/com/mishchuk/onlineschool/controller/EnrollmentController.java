package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.EnrollmentCreateDto;
import com.mishchuk.onlineschool.controller.dto.EnrollmentDto;
import com.mishchuk.onlineschool.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<Void> createEnrollment(@RequestBody EnrollmentCreateDto dto) {
        try {
            enrollmentService.createEnrollment(dto);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<EnrollmentDto>> getEnrollments(
            @RequestParam(required = false) UUID studentId) {
        if (studentId != null) {
            return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudent(studentId));
        }
        return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }
}
