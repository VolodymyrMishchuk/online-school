package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.EnrollmentCreateDto;
import com.mishchuk.onlineschool.controller.dto.EnrollmentDto;
import com.mishchuk.onlineschool.mapper.EnrollmentMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private PersonRepository personRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentMapper enrollmentMapper;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private UUID studentId;
    private UUID courseId;
    private PersonEntity student;
    private CourseEntity course;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        student = new PersonEntity();
        student.setId(studentId);
        student.setFirstName("Іванка");
        student.setLastName("Петренко");
        student.setEmail("student@test.com");

        course = new CourseEntity();
        course.setId(courseId);
        course.setName("Пологи Nature");
    }

    private EnrollmentCreateDto dto() {
        return new EnrollmentCreateDto(studentId, courseId);
    }

    // --- createEnrollment success ---

    @Test
    @DisplayName("createEnrollment — успішне збереження + відправка email")
    void createEnrollment_success_sendsEmail() {
        when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)).thenReturn(Optional.empty());
        when(personRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentMapper.toEntity(any())).thenReturn(new EnrollmentEntity());

        enrollmentService.createEnrollment(dto());

        verify(enrollmentRepository).save(any(EnrollmentEntity.class));
        verify(emailService).sendCourseAccessGrantedEmail(
                eq(student.getEmail()), contains(student.getFirstName()), eq(course.getName())
        );
    }

    @Test
    @DisplayName("createEnrollment — встановлює expiresAt якщо курс має accessDuration")
    void createEnrollment_setsExpiresAt_whenAccessDurationSet() {
        course.setAccessDuration(30);
        EnrollmentEntity entity = new EnrollmentEntity();

        when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)).thenReturn(Optional.empty());
        when(personRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentMapper.toEntity(any())).thenReturn(entity);

        enrollmentService.createEnrollment(dto());

        ArgumentCaptor<EnrollmentEntity> captor = ArgumentCaptor.forClass(EnrollmentEntity.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("createEnrollment — не встановлює expiresAt якщо курс без accessDuration")
    void createEnrollment_nullExpiresAt_whenNoAccessDuration() {
        course.setAccessDuration(null);
        EnrollmentEntity entity = new EnrollmentEntity();

        when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)).thenReturn(Optional.empty());
        when(personRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentMapper.toEntity(any())).thenReturn(entity);

        enrollmentService.createEnrollment(dto());

        ArgumentCaptor<EnrollmentEntity> captor = ArgumentCaptor.forClass(EnrollmentEntity.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isNull();
    }

    // --- createEnrollment failures ---

    @Test
    @DisplayName("createEnrollment — кидає RuntimeException якщо зарахування вже існує")
    void createEnrollment_alreadyExists_throws() {
        when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(Optional.of(new EnrollmentEntity()));

        assertThatThrownBy(() -> enrollmentService.createEnrollment(dto()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Enrollment already exists");
    }

    @Test
    @DisplayName("createEnrollment — кидає RuntimeException якщо студент не знайдений")
    void createEnrollment_studentNotFound_throws() {
        when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)).thenReturn(Optional.empty());
        when(personRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.createEnrollment(dto()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    @DisplayName("createEnrollment — кидає RuntimeException якщо курс не знайдений")
    void createEnrollment_courseNotFound_throws() {
        when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)).thenReturn(Optional.empty());
        when(personRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.createEnrollment(dto()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Course not found");
    }

    // --- getEnrollmentsByStudent ---

    @Test
    @DisplayName("getEnrollmentsByStudent — повертає список DTO")
    void getEnrollmentsByStudent_returnsList() {
        EnrollmentEntity e = new EnrollmentEntity();
        EnrollmentDto dto = new EnrollmentDto(UUID.randomUUID(), studentId, courseId, null, null, null, null);

        when(enrollmentRepository.findByStudentId(studentId)).thenReturn(List.of(e));
        when(enrollmentMapper.toDto(e)).thenReturn(dto);

        List<EnrollmentDto> result = enrollmentService.getEnrollmentsByStudent(studentId);

        assertThat(result).hasSize(1).containsExactly(dto);
    }
}
