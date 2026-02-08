package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;

import java.util.List;
import java.util.Optional;

public interface CourseService {
    void createCourse(CourseCreateDto dto);

    Optional<CourseDto> getCourse(java.util.UUID id);

    List<CourseDto> getAllCourses();

    List<CourseDto> getAllCoursesWithEnrollment(java.util.UUID userId);

    void updateCourse(java.util.UUID id, CourseUpdateDto dto);

    void deleteCourse(java.util.UUID id);
}
