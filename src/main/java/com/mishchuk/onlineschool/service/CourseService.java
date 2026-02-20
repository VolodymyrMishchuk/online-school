package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseService {
    void createCourse(CourseCreateDto dto, MultipartFile coverImage);

    Optional<CourseDto> getCourse(java.util.UUID id);

    Optional<byte[]> getCourseCoverImage(java.util.UUID id);

    List<CourseDto> getAllCourses();

    List<CourseDto> getAllCoursesWithEnrollment(java.util.UUID userId);

    void updateCourse(UUID id, CourseUpdateDto dto, MultipartFile coverImage);

    void deleteCourse(UUID id);

    void extendAccessForReview(UUID userId, UUID courseId, String videoUrl, String originalFilename);
}
