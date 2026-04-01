package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.EnrollmentCreateDto;
import com.mishchuk.onlineschool.controller.dto.EnrollmentDto;
import com.mishchuk.onlineschool.mapper.EnrollmentMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.NotificationType;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.temporal.ChronoUnit;

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
        studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        courseId  = UUID.fromString("00000000-0000-0000-0000-000000000002");

        student = new PersonEntity();
        student.setId(studentId);
        student.setFirstName("Іванка");
        student.setLastName("Петренко");
        student.setEmail("student@test.com");

        course = new CourseEntity();
        course.setId(courseId);
        course.setName("Пологи Nature");

        // Lenient happy-path стаби: більшість тестів починають з "enrollment не існує"
        lenient().when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(Optional.empty());
        lenient().when(personRepository.findById(studentId)).thenReturn(Optional.of(student));
        lenient().when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        lenient().when(enrollmentMapper.toEntity(any())).thenReturn(new EnrollmentEntity());
    }

    // ─────────────────────── createEnrollment ───────────────────────

    @Test
    @DisplayName("createEnrollment — зберігає entity зі student і course")
    void createEnrollment_savesEntityWithStudentAndCourse() {
        enrollmentService.createEnrollment(dto());

        ArgumentCaptor<EnrollmentEntity> captor = ArgumentCaptor.forClass(EnrollmentEntity.class);
        verify(enrollmentRepository).save(captor.capture());

        EnrollmentEntity saved = captor.getValue();
        assertThat(saved.getStudent()).isEqualTo(student);
        assertThat(saved.getCourse()).isEqualTo(course);
    }

    @Test
    @DisplayName("createEnrollment — надсилає email з точним іменем студента та назвою курсу")
    void createEnrollment_sendsEmailWithExactArgs() {
        enrollmentService.createEnrollment(dto());

        verify(emailService).sendCourseAccessGrantedEmail(
                "student@test.com",
                "Іванка Петренко",
                "Пологи Nature"
        );
    }

    @Test
    @DisplayName("createEnrollment — сповіщає адмінів з точним повідомленням і COURSE_PURCHASED")
    void createEnrollment_notifiesAdminsWithExactMessage() {
        enrollmentService.createEnrollment(dto());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).broadcastToAdmins(
                eq("Нова покупка курсу"),
                messageCaptor.capture(),
                eq(NotificationType.COURSE_PURCHASED)
        );

        String message = messageCaptor.getValue();
        assertThat(message).contains("Іванка Петренко");
        assertThat(message).contains("student@test.com");
        assertThat(message).contains("Пологи Nature");
    }

    @Test
    @DisplayName("createEnrollment — надсилає студенту сповіщення COURSE_PURCHASED з його id")
    void createEnrollment_notifiesStudentWithCourseNameAndCorrectId() {
        enrollmentService.createEnrollment(dto());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                eq(studentId),
                eq("Успішна покупка!"),
                messageCaptor.capture(),
                eq(NotificationType.COURSE_PURCHASED)
        );

        assertThat(messageCaptor.getValue()).contains("Пологи Nature");
    }

    // ─────────────────────── createEnrollment — expiresAt ───────────────────────

    @Test
    @DisplayName("createEnrollment — встановлює expiresAt приблизно через N днів якщо курс має accessDuration")
    void createEnrollment_setsExpiresAtApproximately_whenAccessDurationSet() {
        course.setAccessDuration(30);
        EnrollmentEntity entity = new EnrollmentEntity();
        when(enrollmentMapper.toEntity(any())).thenReturn(entity);

        OffsetDateTime before = OffsetDateTime.now().plusDays(30);

        enrollmentService.createEnrollment(dto());

        ArgumentCaptor<EnrollmentEntity> captor = ArgumentCaptor.forClass(EnrollmentEntity.class);
        verify(enrollmentRepository).save(captor.capture());

        OffsetDateTime expiresAt = captor.getValue().getExpiresAt();
        assertThat(expiresAt).isNotNull();
        // Перевіряємо точне значення з допуском 5 секунд
        assertThat(expiresAt).isCloseTo(before, byLessThan(5L, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("createEnrollment — не встановлює expiresAt якщо accessDuration = null")
    void createEnrollment_nullExpiresAt_whenNoAccessDuration() {
        course.setAccessDuration(null);
        EnrollmentEntity entity = new EnrollmentEntity();
        when(enrollmentMapper.toEntity(any())).thenReturn(entity);

        enrollmentService.createEnrollment(dto());

        ArgumentCaptor<EnrollmentEntity> captor = ArgumentCaptor.forClass(EnrollmentEntity.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("createEnrollment — не встановлює expiresAt якщо accessDuration = 0")
    void createEnrollment_nullExpiresAt_whenAccessDurationIsZero() {
        course.setAccessDuration(0);
        EnrollmentEntity entity = new EnrollmentEntity();
        when(enrollmentMapper.toEntity(any())).thenReturn(entity);

        enrollmentService.createEnrollment(dto());

        ArgumentCaptor<EnrollmentEntity> captor = ArgumentCaptor.forClass(EnrollmentEntity.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isNull();
    }

    // ─────────────────────── createEnrollment — errors ───────────────────────

    @Test
    @DisplayName("createEnrollment — кидає RuntimeException якщо зарахування вже існує")
    void createEnrollment_alreadyExists_throws() {
        when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(Optional.of(new EnrollmentEntity()));

        assertThatThrownBy(() -> enrollmentService.createEnrollment(dto()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Enrollment already exists");

        // жодні інші залежності не виклкались
        verifyNoInteractions(personRepository, courseRepository, emailService, notificationService);
    }

    @Test
    @DisplayName("createEnrollment — кидає RuntimeException якщо студент не знайдений")
    void createEnrollment_studentNotFound_throws() {
        when(personRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.createEnrollment(dto()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Student not found");

        verifyNoInteractions(emailService, notificationService);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createEnrollment — кидає RuntimeException якщо курс не знайдений")
    void createEnrollment_courseNotFound_throws() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.createEnrollment(dto()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Course not found");

        verifyNoInteractions(emailService, notificationService);
        verify(enrollmentRepository, never()).save(any());
    }

    // ─────────────────────── getEnrollmentsByStudent ───────────────────────

    @Test
    @DisplayName("getEnrollmentsByStudent — повертає список DTO, mapper викликається явно")
    void getEnrollmentsByStudent_returnsMappedList() {
        EnrollmentEntity entity = new EnrollmentEntity();
        EnrollmentDto expected = buildDto();

        when(enrollmentRepository.findByStudentId(studentId)).thenReturn(List.of(entity));
        when(enrollmentMapper.toDto(entity)).thenReturn(expected);

        List<EnrollmentDto> result = enrollmentService.getEnrollmentsByStudent(studentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(expected); // identity check
        verify(enrollmentMapper).toDto(entity);
    }

    @Test
    @DisplayName("getEnrollmentsByStudent — повертає порожній список якщо зарахувань немає")
    void getEnrollmentsByStudent_noEnrollments_returnsEmptyList() {
        when(enrollmentRepository.findByStudentId(studentId)).thenReturn(Collections.emptyList());

        List<EnrollmentDto> result = enrollmentService.getEnrollmentsByStudent(studentId);

        assertThat(result).isEmpty();
        verify(enrollmentMapper, never()).toDto(any());
    }

    // ─────────────────────── getAllEnrollments ───────────────────────

    @Test
    @DisplayName("getAllEnrollments — повертає всі зарахування як DTO, mapper викликається для кожного")
    void getAllEnrollments_returnsMappedList() {
        EnrollmentEntity e1 = new EnrollmentEntity();
        EnrollmentEntity e2 = new EnrollmentEntity();
        EnrollmentDto dto1 = buildDto();
        EnrollmentDto dto2 = buildDto();

        when(enrollmentRepository.findAll()).thenReturn(List.of(e1, e2));
        // Використовуємо послідовні відповіді: e1 == e2 за equals() (всі поля null)
        // → окремі стаби перезаписують один одного
        when(enrollmentMapper.toDto(any()))
                .thenReturn(dto1)   // 1-ий виклик
                .thenReturn(dto2);  // 2-ий виклик

        List<EnrollmentDto> result = enrollmentService.getAllEnrollments();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(dto1, dto2);
        verify(enrollmentMapper, times(2)).toDto(any());
    }

    @Test
    @DisplayName("getAllEnrollments — порожній репозиторій повертає порожній список")
    void getAllEnrollments_empty_returnsEmptyList() {
        when(enrollmentRepository.findAll()).thenReturn(Collections.emptyList());

        List<EnrollmentDto> result = enrollmentService.getAllEnrollments();

        assertThat(result).isEmpty();
        verify(enrollmentMapper, never()).toDto(any());
    }

    // ─────────────────────── helpers ───────────────────────

    private EnrollmentCreateDto dto() {
        return new EnrollmentCreateDto(studentId, courseId);
    }

    private EnrollmentDto buildDto() {
        return new EnrollmentDto(UUID.randomUUID(), studentId, courseId, null, null, null, null);
    }
}
