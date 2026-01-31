package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;
import com.mishchuk.onlineschool.mapper.CourseMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    @Override
    @Transactional
    public void createCourse(CourseCreateDto dto) {
        CourseEntity entity = courseMapper.toEntity(dto);
        courseRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseDto> getCourse(java.util.UUID id) {
        return courseRepository.findById(id)
                .map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDto> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(courseMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void updateCourse(java.util.UUID id, CourseUpdateDto dto) {
        CourseEntity entity = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        courseMapper.updateEntityFromDto(dto, entity);
        courseRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteCourse(java.util.UUID id) {
        courseRepository.deleteById(id);
    }
}
