package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;

import java.util.List;
import java.util.Optional;

public interface CourseService {
    void createCourse(CourseCreateDto dto, org.springframework.web.multipart.MultipartFile coverImage);

    Optional<CourseDto> getCourse(java.util.UUID id);

    // Add getCourseCoverImage method
    Optional<byte[]> getCourseCoverImage(java.util.UUID id);

    List<CourseDto> getAllCourses();

    List<CourseDto> getAllCoursesWithEnrollment(java.util.UUID userId);

    void updateCourse(java.util.UUID id, CourseUpdateDto dto,
            org.springframework.web.multipart.MultipartFile coverImage);

    void deleteCourse(java.util.UUID id);

    void extendAccessForReview(java.util.UUID userId, java.util.UUID courseId, String videoUrl,
            String originalFilename);
}
