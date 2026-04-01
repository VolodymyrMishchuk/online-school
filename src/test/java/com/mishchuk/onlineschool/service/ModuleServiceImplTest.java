package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.ModuleCreateDto;
import com.mishchuk.onlineschool.controller.dto.ModuleDto;
import com.mishchuk.onlineschool.controller.dto.ModuleUpdateDto;
import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.mapper.ModuleMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleServiceImplTest {

    @Mock private ModuleRepository moduleRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ModuleMapper moduleMapper;
    @Mock private LessonService lessonService;
    @Mock private LessonRepository lessonRepository;
    @Mock private PersonRepository personRepository;
    @Mock private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private ModuleServiceImpl moduleService;

    private PersonEntity adminUser;
    private PersonEntity regularUser;
    private PersonEntity fakeAdmin;

    @BeforeEach
    void setUp() {
        adminUser = new PersonEntity();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(PersonRole.ADMIN);

        regularUser = new PersonEntity();
        regularUser.setId(UUID.randomUUID());
        regularUser.setEmail("user@test.com");
        regularUser.setRole(PersonRole.USER);

        fakeAdmin = new PersonEntity();
        fakeAdmin.setId(UUID.randomUUID());
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

    // ─────────────────────── createModule ───────────────────────

    @Test
    @DisplayName("createModule — зберігає модуль без курсу")
    void createModule_noCourse_savesModule() {
        ModuleCreateDto dto = new ModuleCreateDto("Модуль 1", null, "Опис", null);
        ModuleEntity entity = new ModuleEntity();
        
        when(moduleMapper.toEntity(dto)).thenReturn(entity);
        when(moduleRepository.save(entity)).thenReturn(entity);

        moduleService.createModule(dto);

        verify(moduleRepository).save(entity);
        assertThat(entity.getCreatedBy()).isSameAs(adminUser);
        verify(courseRepository, never()).findById(any());
        verify(lessonRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createModule — кидає RuntimeException якщо курс не знайдено")
    void createModule_courseNotFound_throws() {
        UUID courseId = UUID.randomUUID();
        ModuleCreateDto dto = new ModuleCreateDto("Модуль 1", courseId, "Опис", null);
        
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.createModule(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Course not found");

        verify(moduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createModule — з уроками: призначає модуль на знайдені уроки")
    void createModule_withLessons_assignsModuleToLessons() {
        UUID lessonId = UUID.randomUUID();
        ModuleCreateDto dto = new ModuleCreateDto("Модуль 1", null, "Опис", List.of(lessonId));
        ModuleEntity entity = new ModuleEntity();
        LessonEntity lesson = new LessonEntity();

        when(moduleMapper.toEntity(dto)).thenReturn(entity);
        when(moduleRepository.save(entity)).thenReturn(entity);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        moduleService.createModule(dto);

        assertThat(lesson.getModule()).isSameAs(entity);
        verify(lessonRepository).save(lesson);
    }

    // ─────────────────────── getModuleLessons ───────────────────────

    @Test
    @DisplayName("getModuleLessons — ADMIN має повний доступ (не обнуляє дані)")
    void getModuleLessons_adminAccess_fullData() {
        UUID moduleId = UUID.randomUUID();
        LessonDto dto = new LessonDto(UUID.randomUUID(), moduleId, "T", "D", "http://vid", 10, "M", "C", 5, null, null, null);
        
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(lessonService.getLessonsByModule(moduleId)).thenReturn(List.of(dto));

        List<LessonDto> result = moduleService.getModuleLessons(moduleId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).videoUrl()).isEqualTo("http://vid");
        assertThat(result.get(0).filesCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("getModuleLessons — Звичайний USER без Enrollment бачить scrubbed дані")
    void getModuleLessons_userNoEnrollment_scrubbedData() {
        setSecurityContext("user@test.com", "ROLE_USER");
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        UUID moduleId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(courseId);
        ModuleEntity module = new ModuleEntity();
        module.setCourse(course);

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(enrollmentRepository.findByStudentIdAndCourseId(regularUser.getId(), courseId))
                .thenReturn(Optional.empty()); // Немає enrollment
                
        LessonDto dto = new LessonDto(UUID.randomUUID(), moduleId, "T", "D", "http://vid", 10, "M", "C", 5, null, null, null);
        when(lessonService.getLessonsByModule(moduleId)).thenReturn(List.of(dto));

        List<LessonDto> result = moduleService.getModuleLessons(moduleId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).videoUrl()).isNull();
        assertThat(result.get(0).filesCount()).isZero();
    }

    @Test
    @DisplayName("getModuleLessons — USER з ACTIVE Enrollment має повний доступ")
    void getModuleLessons_userActiveEnrollment_fullData() {
        setSecurityContext("user@test.com", "ROLE_USER");
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        UUID moduleId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(courseId);
        ModuleEntity module = new ModuleEntity();
        module.setCourse(course);

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStatus("ACTIVE");
        enrollment.setExpiresAt(OffsetDateTime.now().plusDays(10));

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(enrollmentRepository.findByStudentIdAndCourseId(regularUser.getId(), courseId))
                .thenReturn(Optional.of(enrollment));
                
        LessonDto dto = new LessonDto(UUID.randomUUID(), moduleId, "T", "D", "http://vid", 10, "M", "C", 5, null, null, null);
        when(lessonService.getLessonsByModule(moduleId)).thenReturn(List.of(dto));

        List<LessonDto> result = moduleService.getModuleLessons(moduleId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).videoUrl()).isEqualTo("http://vid");
        assertThat(result.get(0).filesCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("getModuleLessons — USER з протермінованим Enrollment або BLOCKED бачить scrubbed дані")
    void getModuleLessons_userBlockedEnrollment_scrubbedData() {
        setSecurityContext("user@test.com", "ROLE_USER");
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        UUID moduleId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(courseId);
        ModuleEntity module = new ModuleEntity();
        module.setCourse(course);

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStatus("BLOCKED");

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(enrollmentRepository.findByStudentIdAndCourseId(regularUser.getId(), courseId))
                .thenReturn(Optional.of(enrollment));
                
        LessonDto dto = new LessonDto(UUID.randomUUID(), moduleId, "T", "D", "http://vid", 10, "M", "C", 5, null, null, null);
        when(lessonService.getLessonsByModule(moduleId)).thenReturn(List.of(dto));

        List<LessonDto> result = moduleService.getModuleLessons(moduleId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).videoUrl()).isNull();
        assertThat(result.get(0).filesCount()).isZero();
    }

    // ─────────────────────── getAllModules / getModule ───────────────────────

    @Test
    @DisplayName("getModule — повертає Optional з DTO якщо знайдено")
    void getModule_found() {
        UUID id = UUID.randomUUID();
        ModuleEntity entity = new ModuleEntity();
        ModuleDto dto = new ModuleDto(id, "Модуль", null, null, 0, 0, null, null, null, null);

        when(moduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(moduleMapper.toDto(entity)).thenReturn(dto);

        assertThat(moduleService.getModule(id)).isPresent().containsSame(dto);
    }

    @Test
    @DisplayName("getAllModules — без courseId повертає всі модулі")
    void getAllModules_noCourseId() {
        ModuleEntity entity = new ModuleEntity();
        ModuleDto dto = new ModuleDto(UUID.randomUUID(), "Модуль", null, null, 0, 0, null, null, null, null);

        when(moduleRepository.findAll()).thenReturn(List.of(entity));
        when(moduleMapper.toDto(entity)).thenReturn(dto);

        List<ModuleDto> result = moduleService.getAllModules();
        assertThat(result).hasSize(1).containsExactly(dto);
    }

    // ─────────────────────── updateModule ───────────────────────

    @Test
    @DisplayName("updateModule — успішно оновлює і робить re-assign уроків")
    void updateModule_successAndReassignsLessons() {
        UUID id = UUID.randomUUID();
        UUID newLessonId = UUID.randomUUID();
        ModuleUpdateDto dto = new ModuleUpdateDto("Name", "Desc", "DRAFT", List.of(newLessonId));

        ModuleEntity entity = new ModuleEntity();
        LessonEntity oldLesson = new LessonEntity();
        oldLesson.setModule(entity);
        LessonEntity newLesson = new LessonEntity();

        when(moduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        
        when(lessonRepository.findByModuleId(id)).thenReturn(List.of(oldLesson)); // поточні уроки
        when(lessonRepository.findById(newLessonId)).thenReturn(Optional.of(newLesson));

        moduleService.updateModule(id, dto);

        verify(moduleMapper).updateEntity(entity, dto);
        verify(moduleRepository).save(entity);
        
        // Старий урок відв'язується
        assertThat(oldLesson.getModule()).isNull();
        verify(lessonRepository).save(oldLesson);
        
        // Новий урок прив'язується
        assertThat(newLesson.getModule()).isSameAs(entity);
        verify(lessonRepository).save(newLesson);
    }

    @Test
    @DisplayName("updateModule — FAKE_ADMIN не може оновити чужий модуль")
    void updateModule_fakeAdmin_othersModule_throws() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        UUID id = UUID.randomUUID();
        ModuleUpdateDto dto = new ModuleUpdateDto("Name", "Desc", "DRAFT", null);
        ModuleEntity entity = new ModuleEntity();
        entity.setCreatedBy(adminUser); // створено кимось іншим

        when(moduleRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> moduleService.updateModule(id, dto))
                .isInstanceOf(AccessDeniedException.class);

        verify(moduleRepository, never()).save(any());
    }

    // ─────────────────────── deleteModule ───────────────────────

    @Test
    @DisplayName("deleteModule — ADMIN успішно видаляє модуль")
    void deleteModule_admin_success() {
        UUID id = UUID.randomUUID();
        ModuleEntity entity = new ModuleEntity();
        entity.setCreatedBy(fakeAdmin);

        when(moduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        moduleService.deleteModule(id);

        verify(moduleRepository).deleteById(id);
    }

    @Test
    @DisplayName("deleteModule — кидає RuntimeException якщо модуль не знайдено")
    void deleteModule_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(moduleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.deleteModule(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Module not found");
                
        verify(moduleRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteModule — FAKE_ADMIN не може видалити чужий модуль")
    void deleteModule_fakeAdmin_othersModule_throws() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        UUID id = UUID.randomUUID();
        ModuleEntity entity = new ModuleEntity();
        entity.setCreatedBy(adminUser);

        when(moduleRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> moduleService.deleteModule(id))
                .isInstanceOf(AccessDeniedException.class);
                
        verify(moduleRepository, never()).deleteById(any());
    }
}
