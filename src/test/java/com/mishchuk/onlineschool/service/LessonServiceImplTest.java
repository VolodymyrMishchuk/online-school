package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.LessonCreateDto;
import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.controller.dto.LessonUpdateDto;
import com.mishchuk.onlineschool.mapper.LessonMapper;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.LessonRepository;
import com.mishchuk.onlineschool.repository.ModuleRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.LessonEntity;
import com.mishchuk.onlineschool.repository.entity.ModuleEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

    @Mock private LessonRepository lessonRepository;
    @Mock private ModuleRepository moduleRepository;
    @Mock private LessonMapper lessonMapper;
    @Mock private PersonRepository personRepository;
    @Mock private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private LessonServiceImpl lessonService;

    private PersonEntity adminUser;
    private PersonEntity regularUser;
    private PersonEntity fakeAdmin;

    @BeforeEach
    void setUp() {
        adminUser = new PersonEntity();
        adminUser.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(PersonRole.ADMIN);

        regularUser = new PersonEntity();
        regularUser.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        regularUser.setEmail("user@test.com");
        regularUser.setRole(PersonRole.USER);

        fakeAdmin = new PersonEntity();
        fakeAdmin.setId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        fakeAdmin.setEmail("fake@test.com");
        fakeAdmin.setRole(PersonRole.FAKE_ADMIN);

        setSecurityContext("admin@test.com", "ROLE_ADMIN");
        lenient().when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    // ─────────────────────── createLesson ───────────────────────

    @Test
    @DisplayName("createLesson — зберігає урок без модуля якщо moduleId = null")
    void createLesson_noModule_success() {
        LessonCreateDto dto = new LessonCreateDto(null, "Урок 1", "Опис", "https://video.url", 30);
        LessonEntity entity = new LessonEntity();
        LessonDto expected = new LessonDto(null, null, "Урок 1", "Опис", "https://video.url", 30, null, null, 0, null, null, null);

        when(lessonMapper.toEntity(dto)).thenReturn(entity);
        when(lessonRepository.save(entity)).thenReturn(entity);
        when(lessonMapper.toDto(entity)).thenReturn(expected);

        LessonDto result = lessonService.createLesson(dto);

        assertThat(result).isSameAs(expected);
        assertThat(entity.getCreatedBy()).isSameAs(adminUser);
        verify(moduleRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createLesson — зберігає урок з модулем якщо moduleId задано")
    void createLesson_withModule_success() {
        UUID moduleId = UUID.randomUUID();
        LessonCreateDto dto = new LessonCreateDto(moduleId, "Урок", "Опис", "url", 10);
        LessonEntity entity = new LessonEntity();
        ModuleEntity module = new ModuleEntity();

        when(lessonMapper.toEntity(dto)).thenReturn(entity);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(lessonRepository.save(entity)).thenReturn(entity);
        when(lessonMapper.toDto(entity)).thenReturn(new LessonDto(null, null, null, null, null, 0, null, null, 0, null, null, null));

        lessonService.createLesson(dto);

        assertThat(entity.getModule()).isSameAs(module);
        assertThat(entity.getCreatedBy()).isSameAs(adminUser);
        verify(lessonRepository).save(entity);
    }

    @Test
    @DisplayName("createLesson — кидає RuntimeException якщо модуль не знайдено")
    void createLesson_moduleNotFound_throws() {
        UUID moduleId = UUID.randomUUID();
        LessonCreateDto dto = new LessonCreateDto(moduleId, "Урок", "Опис", "url", 10);
        LessonEntity entity = new LessonEntity();

        when(lessonMapper.toEntity(dto)).thenReturn(entity);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.createLesson(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Module not found");

        verify(lessonRepository, never()).save(any());
    }

    // ─────────────────────── getLesson ───────────────────────

    @Test
    @DisplayName("getLesson — ADMIN має повний доступ (videoUrl не обнуляється)")
    void getLesson_admin_fullAccess() {
        UUID lessonId = UUID.randomUUID();
        LessonEntity lesson = new LessonEntity();
        LessonDto dto = new LessonDto(lessonId, null, "Name", null, "https://secret.video", 10, null, null, 0, null, null, null);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonMapper.toDto(lesson)).thenReturn(dto);

        Optional<LessonDto> result = lessonService.getLesson(lessonId);

        assertThat(result).isPresent();
        assertThat(result.get().videoUrl()).isEqualTo("https://secret.video");
    }

    @Test
    @DisplayName("getLesson — USER з активним Enrollment має повний доступ")
    void getLesson_userActiveEnrollment_fullAccess() {
        setSecurityContext("user@test.com", "ROLE_USER");
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        UUID lessonId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(UUID.randomUUID());
        ModuleEntity module = new ModuleEntity();
        module.setCourse(course);
        LessonEntity lesson = new LessonEntity();
        lesson.setModule(module);

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStatus("ACTIVE");
        enrollment.setExpiresAt(OffsetDateTime.now().plusDays(10));

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findByStudentIdAndCourseId(regularUser.getId(), course.getId()))
                .thenReturn(Optional.of(enrollment));
        LessonDto dto = new LessonDto(lessonId, null, "Name", null, "https://secret.video", 10, null, null, 0, null, null, null);
        when(lessonMapper.toDto(lesson)).thenReturn(dto);

        Optional<LessonDto> result = lessonService.getLesson(lessonId);

        assertThat(result).isPresent();
        assertThat(result.get().videoUrl()).isEqualTo("https://secret.video");
    }

    @Test
    @DisplayName("getLesson — USER з протермінованим Enrollment бачить обнулений videoUrl")
    void getLesson_userExpiredEnrollment_scrubbedContent() {
        setSecurityContext("user@test.com", "ROLE_USER");
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        UUID lessonId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(UUID.randomUUID());
        ModuleEntity module = new ModuleEntity();
        module.setCourse(course);
        LessonEntity lesson = new LessonEntity();
        lesson.setModule(module);

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStatus("ACTIVE");
        enrollment.setExpiresAt(OffsetDateTime.now().minusDays(1)); // expired yesterday

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findByStudentIdAndCourseId(regularUser.getId(), course.getId()))
                .thenReturn(Optional.of(enrollment));
        LessonDto dto = new LessonDto(lessonId, null, "Name", null, "https://secret.video", 10, null, null, 0, null, null, null);
        when(lessonMapper.toDto(lesson)).thenReturn(dto);

        Optional<LessonDto> result = lessonService.getLesson(lessonId);

        assertThat(result).isPresent();
        assertThat(result.get().videoUrl()).isNull();
    }

    @Test
    @DisplayName("getLesson — USER з BLOCKED Enrollment бачить обнулений videoUrl")
    void getLesson_userBlockedEnrollment_scrubbedContent() {
        setSecurityContext("user@test.com", "ROLE_USER");
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        UUID lessonId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(UUID.randomUUID());
        ModuleEntity module = new ModuleEntity();
        module.setCourse(course);
        LessonEntity lesson = new LessonEntity();
        lesson.setModule(module);

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStatus("BLOCKED");

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findByStudentIdAndCourseId(regularUser.getId(), course.getId()))
                .thenReturn(Optional.of(enrollment));
        LessonDto dto = new LessonDto(lessonId, null, "Name", null, "https://secret.video", 10, null, null, 0, null, null, null);
        when(lessonMapper.toDto(lesson)).thenReturn(dto);

        Optional<LessonDto> result = lessonService.getLesson(lessonId);

        assertThat(result).isPresent();
        assertThat(result.get().videoUrl()).isNull();
    }

    // ─────────────────────── getAllLessons / getUnassignedLessons / getLessonsByModule ───────────────────────

    @Test
    @DisplayName("getAllLessons — повертає список DTO")
    void getAllLessons_returnsList() {
        LessonEntity e = new LessonEntity();
        LessonDto dto = new LessonDto(null, null, "Урок", null, null, 0, null, null, 0, null, null, null);

        when(lessonRepository.findAll()).thenReturn(List.of(e));
        when(lessonMapper.toDto(e)).thenReturn(dto);

        List<LessonDto> result = lessonService.getAllLessons();
        assertThat(result).hasSize(1).containsExactly(dto);
    }

    @Test
    @DisplayName("getUnassignedLessons — повертає уроки де moduleId IS NULL")
    void getUnassignedLessons_returnsList() {
        LessonEntity e = new LessonEntity();
        LessonDto dto = new LessonDto(null, null, "Orphan", null, null, 0, null, null, 0, null, null, null);

        when(lessonRepository.findByModuleIdIsNull()).thenReturn(List.of(e));
        when(lessonMapper.toDto(e)).thenReturn(dto);

        List<LessonDto> result = lessonService.getUnassignedLessons();
        assertThat(result).hasSize(1).containsExactly(dto);
    }

    @Test
    @DisplayName("getLessonsByModule — повертає уроки за конкретним модулем")
    void getLessonsByModule_returnsList() {
        UUID moduleId = UUID.randomUUID();
        LessonEntity e = new LessonEntity();
        LessonDto dto = new LessonDto(null, moduleId, "Урок", null, null, 0, null, null, 0, null, null, null);

        when(lessonRepository.findByModuleId(moduleId)).thenReturn(List.of(e));
        when(lessonMapper.toDto(e)).thenReturn(dto);

        List<LessonDto> result = lessonService.getLessonsByModule(moduleId);
        assertThat(result).hasSize(1).containsExactly(dto);
    }

    // ─────────────────────── updateLesson ───────────────────────

    @Test
    @DisplayName("updateLesson — ADMIN успішно оновлює будь-який урок")
    void updateLesson_admin_success() {
        UUID id = UUID.randomUUID();
        LessonEntity entity = new LessonEntity();
        LessonUpdateDto dto = new LessonUpdateDto("Updated", "Desc", "url", 5);

        when(lessonRepository.findById(id)).thenReturn(Optional.of(entity));

        lessonService.updateLesson(id, dto);

        verify(lessonMapper).updateEntity(entity, dto);
        verify(lessonRepository).save(entity);
    }

    @Test
    @DisplayName("updateLesson — FAKE_ADMIN успішно оновлює СВІЙ урок")
    void updateLesson_fakeAdminOwn_success() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        UUID id = UUID.randomUUID();
        LessonEntity entity = new LessonEntity();
        entity.setCreatedBy(fakeAdmin); // Це свій урок
        LessonUpdateDto dto = new LessonUpdateDto("Updated", "Desc", "url", 5);

        when(lessonRepository.findById(id)).thenReturn(Optional.of(entity));

        lessonService.updateLesson(id, dto);

        verify(lessonMapper).updateEntity(entity, dto);
        verify(lessonRepository).save(entity);
    }

    @Test
    @DisplayName("updateLesson — FAKE_ADMIN кидає AccessDeniedException для ЧУЖОГО уроку")
    void updateLesson_fakeAdminOther_throws() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        UUID id = UUID.randomUUID();
        LessonEntity entity = new LessonEntity();
        entity.setCreatedBy(adminUser); // Хтось інший створив
        LessonUpdateDto dto = new LessonUpdateDto("Updated", "Desc", "url", 5);

        when(lessonRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> lessonService.updateLesson(id, dto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("modify their own");

        verify(lessonRepository, never()).save(any());
        verify(lessonMapper, never()).updateEntity(any(), any());
    }

    @Test
    @DisplayName("updateLesson — кидає RuntimeException якщо урок не знайдено")
    void updateLesson_notFound_throws() {
        UUID id = UUID.randomUUID();
        LessonUpdateDto dto = new LessonUpdateDto("Updated", "Desc", "url", 5);

        when(lessonRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.updateLesson(id, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lesson not found");

        verify(lessonRepository, never()).save(any());
    }

    // ─────────────────────── deleteLesson ───────────────────────

    @Test
    @DisplayName("deleteLesson — ADMIN успішно видаляє будь-який урок")
    void deleteLesson_admin_success() {
        UUID id = UUID.randomUUID();
        LessonEntity lesson = new LessonEntity();
        when(lessonRepository.findById(id)).thenReturn(Optional.of(lesson));

        lessonService.deleteLesson(id);

        verify(lessonRepository).deleteById(id);
    }

    @Test
    @DisplayName("deleteLesson — FAKE_ADMIN успішно видаляє СВІЙ урок")
    void deleteLesson_fakeAdminOwn_success() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        UUID id = UUID.randomUUID();
        LessonEntity lesson = new LessonEntity();
        lesson.setCreatedBy(fakeAdmin);

        when(lessonRepository.findById(id)).thenReturn(Optional.of(lesson));

        lessonService.deleteLesson(id);

        verify(lessonRepository).deleteById(id);
    }

    @Test
    @DisplayName("deleteLesson — FAKE_ADMIN кидає AccessDeniedException для ЧУЖОГО уроку")
    void deleteLesson_fakeAdminOther_throws() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        UUID id = UUID.randomUUID();
        LessonEntity lesson = new LessonEntity();
        lesson.setCreatedBy(adminUser); // створено кимось іншим

        when(lessonRepository.findById(id)).thenReturn(Optional.of(lesson));

        assertThatThrownBy(() -> lessonService.deleteLesson(id))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("delete their own");

        verify(lessonRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteLesson — кидає RuntimeException якщо урок не знайдено")
    void deleteLesson_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(lessonRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.deleteLesson(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lesson not found");

        verify(lessonRepository, never()).deleteById(any());
    }
}
