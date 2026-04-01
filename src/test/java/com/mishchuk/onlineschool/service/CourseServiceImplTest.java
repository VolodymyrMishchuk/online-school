package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;
import com.mishchuk.onlineschool.exception.BadRequestException;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.CourseMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.CourseReviewRequestRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.CourseStatus;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.NotificationType;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseMapper courseMapper;
    @Mock private NotificationService notificationService;
    @Mock private CourseReviewRequestRepository courseReviewRequestRepository;
    @Mock private EmailService emailService;
    @Mock private PersonRepository personRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private PersonEntity adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new PersonEntity();
        adminUser.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(PersonRole.ADMIN);

        setSecurityContext("admin@test.com");
        lenient().when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────── createCourse ───────────────────────

    @Test
    @DisplayName("createCourse — кидає BadRequestException коли задано і % і суму знижки одночасно")
    void createCourse_dualDiscount_throws() {
        CourseCreateDto dto = buildCreateDto(null, 10, BigDecimal.valueOf(5));

        assertThatThrownBy(() -> courseService.createCourse(dto, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("promotional discount");

        verifyNoInteractions(courseRepository);
    }

    @Test
    @DisplayName("createCourse — зберігає новий курс без обкладинки та встановлює createdBy")
    void createCourse_success_savesEntityWithCreatedBy() {
        CourseCreateDto dto = buildCreateDto(BigDecimal.valueOf(100), null, null);
        CourseEntity entity = new CourseEntity();
        when(courseMapper.toEntity(dto)).thenReturn(entity);

        courseService.createCourse(dto, null);

        ArgumentCaptor<CourseEntity> captor = ArgumentCaptor.forClass(CourseEntity.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(adminUser);
    }

    @Test
    @DisplayName("createCourse — коли задано тільки % знижки, amount = null в entity")
    void createCourse_onlyPercentageDiscount_clearsAmount() {
        CourseCreateDto dto = buildCreateDto(BigDecimal.valueOf(100), 15, null);
        CourseEntity entity = new CourseEntity();
        entity.setPromotionalDiscountAmount(BigDecimal.TEN); // щоб перевірити що обнуляється
        when(courseMapper.toEntity(dto)).thenReturn(entity);

        courseService.createCourse(dto, null);

        ArgumentCaptor<CourseEntity> captor = ArgumentCaptor.forClass(CourseEntity.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getPromotionalDiscountAmount()).isNull();
    }

    // ─────────────────────── getCourse ───────────────────────

    @Test
    @DisplayName("getCourse — знайдено: повертає Optional<CourseDto>, mapper викликаний явно")
    void getCourse_found_returnsMappedDtoWithIdentity() {
        UUID id = UUID.randomUUID();
        CourseEntity entity = new CourseEntity();
        entity.setId(id);
        CourseDto expected = buildCourseDto(id);

        when(courseRepository.findById(id)).thenReturn(Optional.of(entity));
        when(courseMapper.toDto(entity)).thenReturn(expected);

        Optional<CourseDto> result = courseService.getCourse(id);

        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(expected); // identity: повертається саме той об'єкт
        verify(courseMapper).toDto(entity);
    }

    @Test
    @DisplayName("getCourse — не знайдено: повертає порожній Optional, mapper не викликається")
    void getCourse_notFound_returnsEmptyOptional() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        Optional<CourseDto> result = courseService.getCourse(id);

        assertThat(result).isEmpty();
        verifyNoInteractions(courseMapper);
    }

    // ─────────────────────── getAllCourses ───────────────────────

    @Test
    @DisplayName("getAllCourses — ADMIN бачить всі курси, включно з DRAFT і ARCHIVED")
    void getAllCourses_adminUser_returnsAllCourses() {
        CourseEntity draft = courseOfStatus(CourseStatus.DRAFT);
        CourseEntity published = courseOfStatus(CourseStatus.PUBLISHED);
        CourseEntity archived = courseOfStatus(CourseStatus.ARCHIVED);

        when(courseRepository.findAll()).thenReturn(List.of(draft, published, archived));
        when(courseMapper.toDto(any())).thenReturn(buildCourseDto(UUID.randomUUID()));

        List<CourseDto> result = courseService.getAllCourses();

        assertThat(result).hasSize(3);
        verify(courseMapper, times(3)).toDto(any());
    }

    @Test
    @DisplayName("getAllCourses — USER бачить лише PUBLISHED курси")
    void getAllCourses_regularUser_returnsOnlyPublished() {
        PersonEntity regularUser = personEntity("user@test.com", PersonRole.USER);
        setSecurityContext("user@test.com");
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        CourseEntity draft = courseOfStatus(CourseStatus.DRAFT);
        CourseEntity published = courseOfStatus(CourseStatus.PUBLISHED);
        CourseEntity archived = courseOfStatus(CourseStatus.ARCHIVED);

        when(courseRepository.findAll()).thenReturn(List.of(draft, published, archived));
        when(courseMapper.toDto(published)).thenReturn(buildCourseDto(UUID.randomUUID()));

        List<CourseDto> result = courseService.getAllCourses();

        assertThat(result).hasSize(1);
        verify(courseMapper, times(1)).toDto(published);
        verify(courseMapper, never()).toDto(draft);
        verify(courseMapper, never()).toDto(archived);
    }

    @Test
    @DisplayName("getAllCourses — порожній репозиторій повертає порожній список")
    void getAllCourses_empty_returnsEmptyList() {
        when(courseRepository.findAll()).thenReturn(Collections.emptyList());

        List<CourseDto> result = courseService.getAllCourses();

        assertThat(result).isEmpty();
        verifyNoInteractions(courseMapper);
    }

    // ─────────────────────── deleteCourse ───────────────────────

    @Test
    @DisplayName("deleteCourse — кидає ResourceNotFoundException якщо курс не знайдено")
    void deleteCourse_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.deleteCourse(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(courseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteCourse — ADMIN успішно видаляє будь-який курс")
    void deleteCourse_admin_deletesEntity() {
        UUID id = UUID.randomUUID();
        CourseEntity entity = new CourseEntity();
        entity.setId(id);

        when(courseRepository.findById(id)).thenReturn(Optional.of(entity));

        courseService.deleteCourse(id);

        verify(courseRepository).delete(entity);
    }

    @Test
    @DisplayName("deleteCourse — FAKE_ADMIN може видаляти лише свій курс")
    void deleteCourse_fakeAdmin_ownCourse_succeeds() {
        PersonEntity fakeAdmin = personEntity("fake@test.com", PersonRole.FAKE_ADMIN);
        setSecurityContext("fake@test.com");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        UUID id = UUID.randomUUID();
        CourseEntity entity = new CourseEntity();
        entity.setId(id);
        entity.setCreatedBy(fakeAdmin); // власний курс

        when(courseRepository.findById(id)).thenReturn(Optional.of(entity));

        courseService.deleteCourse(id);

        verify(courseRepository).delete(entity);
    }

    @Test
    @DisplayName("deleteCourse — FAKE_ADMIN кидає AccessDeniedException для чужого курсу")
    void deleteCourse_fakeAdmin_otherCourse_throwsAccessDenied() {
        PersonEntity fakeAdmin = personEntity("fake@test.com", PersonRole.FAKE_ADMIN);
        setSecurityContext("fake@test.com");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        PersonEntity otherAdmin = personEntity("other@test.com", PersonRole.ADMIN);
        UUID id = UUID.randomUUID();
        CourseEntity entity = new CourseEntity();
        entity.setId(id);
        entity.setCreatedBy(otherAdmin); // чужий курс

        when(courseRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> courseService.deleteCourse(id))
                .isInstanceOf(AccessDeniedException.class);

        verify(courseRepository, never()).delete(any());
    }

    // ─────────────────────── updateCourse ───────────────────────

    @Test
    @DisplayName("updateCourse — кидає BadRequestException для конфлікту знижок")
    void updateCourse_dualDiscount_throws() {
        UUID id = UUID.randomUUID();
        CourseUpdateDto dto = buildUpdateDto(10, BigDecimal.valueOf(5));

        assertThatThrownBy(() -> courseService.updateCourse(id, dto, null))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(courseRepository);
    }

    @Test
    @DisplayName("updateCourse — кидає ResourceNotFoundException якщо курс не знайдено")
    void updateCourse_notFound_throws() {
        UUID id = UUID.randomUUID();
        CourseUpdateDto dto = buildUpdateDto(null, null);

        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourse(id, dto, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCourse — ADMIN успішно оновлює та зберігає курс")
    void updateCourse_admin_savesEntity() {
        UUID id = UUID.randomUUID();
        CourseEntity entity = new CourseEntity();
        entity.setId(id);
        CourseUpdateDto dto = buildUpdateDto(null, null);

        when(courseRepository.findById(id)).thenReturn(Optional.of(entity));

        courseService.updateCourse(id, dto, null);

        verify(courseMapper).updateEntityFromDto(dto, entity);
        verify(courseRepository).save(entity);
    }

    @Test
    @DisplayName("updateCourse — FAKE_ADMIN кидає AccessDeniedException для чужого курсу")
    void updateCourse_fakeAdmin_otherCourse_throwsAccessDenied() {
        PersonEntity fakeAdmin = personEntity("fake@test.com", PersonRole.FAKE_ADMIN);
        setSecurityContext("fake@test.com");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        PersonEntity other = personEntity("other@test.com", PersonRole.ADMIN);
        UUID id = UUID.randomUUID();
        CourseEntity entity = new CourseEntity();
        entity.setId(id);
        entity.setCreatedBy(other);

        when(courseRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> courseService.updateCourse(id, buildUpdateDto(null, null), null))
                .isInstanceOf(AccessDeniedException.class);

        verify(courseRepository, never()).save(any());
    }

    // ─────────────────────── updateCourseStatus ───────────────────────

    @ParameterizedTest
    @EnumSource(CourseStatus.class)
    @DisplayName("updateCourseStatus — оновлює статус для всіх значень CourseStatus")
    void updateCourseStatus_updatesForAllStatuses(CourseStatus status) {
        UUID id = UUID.randomUUID();
        CourseEntity entity = new CourseEntity();
        entity.setId(id);
        entity.setStatus(CourseStatus.DRAFT);

        when(courseRepository.findById(id)).thenReturn(Optional.of(entity));

        courseService.updateCourseStatus(id, status);

        assertThat(entity.getStatus()).isEqualTo(status);
        verify(courseRepository).save(entity);
    }

    @Test
    @DisplayName("updateCourseStatus — кидає ResourceNotFoundException якщо не знайдено")
    void updateCourseStatus_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourseStatus(id, CourseStatus.PUBLISHED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(courseRepository, never()).save(any());
    }

    // ─────────────────────── cloneCourse ───────────────────────

    @Test
    @DisplayName("cloneCourse — зберігає копію з суфіксом (Copy) та інкрементованою версією")
    void cloneCourse_savesWithCopySuffixAndIncrementedVersion() {
        UUID id = UUID.randomUUID();
        CourseEntity original = new CourseEntity();
        original.setId(id);
        original.setName("Курс Медитації");
        original.setVersion("2.0");
        original.setStatus(CourseStatus.PUBLISHED);
        original.setPrice(BigDecimal.valueOf(500));

        when(courseRepository.findById(id)).thenReturn(Optional.of(original));

        courseService.cloneCourse(id);

        ArgumentCaptor<CourseEntity> captor = ArgumentCaptor.forClass(CourseEntity.class);
        verify(courseRepository).save(captor.capture());
        CourseEntity cloned = captor.getValue();

        assertThat(cloned.getName()).isEqualTo("Курс Медитації (Copy)");
        assertThat(cloned.getVersion()).isEqualTo("3.0");   // 2 + 1
        assertThat(cloned.getStatus()).isEqualTo(CourseStatus.DRAFT); // завжди DRAFT
        assertThat(cloned.getPrice()).isEqualTo(BigDecimal.valueOf(500));
        assertThat(cloned.getCreatedBy()).isEqualTo(adminUser);
    }

    @Test
    @DisplayName("cloneCourse — версія 1.0 → клон отримує 2.0")
    void cloneCourse_version1_becomes2() {
        UUID id = UUID.randomUUID();
        CourseEntity original = new CourseEntity();
        original.setId(id);
        original.setName("Тест");
        original.setVersion("1.0");

        when(courseRepository.findById(id)).thenReturn(Optional.of(original));

        courseService.cloneCourse(id);

        ArgumentCaptor<CourseEntity> captor = ArgumentCaptor.forClass(CourseEntity.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo("2.0");
    }

    @Test
    @DisplayName("cloneCourse — кидає ResourceNotFoundException якщо курс не знайдено")
    void cloneCourse_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.cloneCourse(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(courseRepository, never()).save(any());
    }

    // ─────────────────────── extendAccessForReview ───────────────────────

    @Test
    @DisplayName("extendAccessForReview — продовжує доступ, зберігає review request, надсилає сповіщення")
    void extendAccessForReview_success_createsAllSideEffects() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID courseId = UUID.fromString("00000000-0000-0000-0000-000000000020");

        PersonEntity student = new PersonEntity();
        student.setEmail("student@test.com");
        student.setFirstName("Юля");
        student.setLastName("Іваненко");

        CourseEntity course = new CourseEntity();
        course.setName("Йога для початківців");

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStudent(student);
        enrollment.setCourse(course);

        when(enrollmentRepository.findByStudentIdAndCourseId(userId, courseId))
                .thenReturn(Optional.of(enrollment));

        courseService.extendAccessForReview(userId, courseId, "https://cdn.example.com/video.mp4", "review.mp4");

        // Доступ продовжено і статус оновлено
        verify(enrollmentRepository).save(enrollment);
        assertThat(enrollment.getStatus()).isEqualTo("ACTIVE");
        assertThat(enrollment.getExpiresAt()).isNotNull();

        // ReviewRequest збережено
        verify(courseReviewRequestRepository).save(any());

        // Користувач отримав сповіщення
        verify(notificationService).createNotification(
                eq(userId),
                eq("Доступ продовжено"),
                contains("31 день"),
                eq(NotificationType.COURSE_ACCESS_EXTENDED)
        );

        // Адміни отримали сповіщення з URL відео
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).broadcastToAdmins(
                anyString(), anyString(), eq(NotificationType.SYSTEM), urlCaptor.capture());
        assertThat(urlCaptor.getValue()).isEqualTo("https://cdn.example.com/video.mp4");

        // Email надіслано студенту
        verify(emailService).sendAccessExtendedEmail(
                eq("student@test.com"), eq("Юля"), eq("Йога для початківців"), any());
    }

    @Test
    @DisplayName("extendAccessForReview — кидає ResourceNotFoundException якщо enrollment не знайдено")
    void extendAccessForReview_notFound_throws() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(enrollmentRepository.findByStudentIdAndCourseId(userId, courseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                courseService.extendAccessForReview(userId, courseId, "url", "file.mp4"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(enrollmentRepository, never()).save(any());
        verifyNoInteractions(notificationService, emailService, courseReviewRequestRepository);
    }

    // ─────────────────────── helpers ───────────────────────

    private void setSecurityContext(String email) {
        // 3-arg конструктор створює authenticated=true токен (2-arg — unauthenticated)
        var auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    private PersonEntity personEntity(String email, PersonRole role) {
        PersonEntity p = new PersonEntity();
        p.setId(UUID.randomUUID());
        p.setEmail(email);
        p.setRole(role);
        return p;
    }

    private CourseEntity courseOfStatus(CourseStatus status) {
        CourseEntity e = new CourseEntity();
        e.setId(UUID.randomUUID());
        e.setStatus(status);
        return e;
    }

    private CourseDto buildCourseDto(UUID id) {
        return new CourseDto(id, "Test", null, 0, 0, 0,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private CourseCreateDto buildCreateDto(BigDecimal price, Integer discountPct, BigDecimal discountAmt) {
        return new CourseCreateDto("Тест курс", "Опис", price, null, null, null,
                discountPct, discountAmt, null, null);
    }

    private CourseUpdateDto buildUpdateDto(Integer discountPct, BigDecimal discountAmt) {
        return new CourseUpdateDto("Назва", "Опис", BigDecimal.valueOf(100), null,
                null, null, null, discountPct, discountAmt, null, null, null);
    }
}
