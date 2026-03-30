package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.ModuleCreateDto;
import com.mishchuk.onlineschool.controller.dto.ModuleDto;
import com.mishchuk.onlineschool.mapper.ModuleMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.LessonRepository;
import com.mishchuk.onlineschool.repository.ModuleRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.ModuleEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

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

    @BeforeEach
    void setUp() {
        adminUser = new PersonEntity();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(PersonRole.ADMIN);

        var auth = new UsernamePasswordAuthenticationToken("admin@test.com", null);
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    // --- createModule ---

    @Test
    @DisplayName("createModule — зберігає модуль без курсу")
    void createModule_noCourse_savesModule() {
        ModuleCreateDto dto = new ModuleCreateDto("Модуль 1", null, "Опис", null);
        ModuleEntity entity = new ModuleEntity();
        when(moduleMapper.toEntity(dto)).thenReturn(entity);
        when(moduleRepository.save(entity)).thenReturn(entity);

        moduleService.createModule(dto);

        verify(moduleRepository).save(entity);
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
    }

    // --- getModule ---

    @Test
    @DisplayName("getModule — повертає Optional з DTO якщо знайдено")
    void getModule_found() {
        UUID id = UUID.randomUUID();
        ModuleEntity entity = new ModuleEntity();
        ModuleDto dto = new ModuleDto(id, "Модуль", null, null, 0, 0, null, null, null, null);

        when(moduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(moduleMapper.toDto(entity)).thenReturn(dto);

        assertThat(moduleService.getModule(id)).isPresent().contains(dto);
    }

    @Test
    @DisplayName("getModule — повертає empty Optional якщо не знайдено")
    void getModule_notFound() {
        UUID id = UUID.randomUUID();
        when(moduleRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(moduleService.getModule(id)).isEmpty();
    }

    // --- getAllModules ---

    @Test
    @DisplayName("getAllModules(courseId) — повертає модулі для курсу")
    void getAllModules_byCourse() {
        UUID courseId = UUID.randomUUID();
        ModuleEntity entity = new ModuleEntity();
        ModuleDto dto = new ModuleDto(UUID.randomUUID(), "Модуль", courseId, null, 0, 0, null, null, null, null);

        when(moduleRepository.findByCourseId(courseId)).thenReturn(List.of(entity));
        when(moduleMapper.toDto(entity)).thenReturn(dto);

        List<ModuleDto> result = moduleService.getAllModules(courseId);
        assertThat(result).hasSize(1).containsExactly(dto);
    }

    // --- deleteModule ---

    @Test
    @DisplayName("deleteModule — ADMIN видаляє модуль")
    void deleteModule_admin_success() {
        UUID id = UUID.randomUUID();
        ModuleEntity entity = new ModuleEntity();
        entity.setCreatedBy(adminUser);

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
    }

    @Test
    @DisplayName("deleteModule — FAKE_ADMIN не може видалити чужий модуль")
    void deleteModule_fakeAdmin_othersModule_throws() {
        UUID id = UUID.randomUUID();
        PersonEntity fakeAdmin = new PersonEntity();
        fakeAdmin.setId(UUID.randomUUID());
        fakeAdmin.setEmail("fake@test.com");
        fakeAdmin.setRole(PersonRole.FAKE_ADMIN);

        PersonEntity owner = new PersonEntity();
        owner.setId(UUID.randomUUID());

        ModuleEntity entity = new ModuleEntity();
        entity.setCreatedBy(owner);

        var auth = new UsernamePasswordAuthenticationToken("fake@test.com", null);
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));

        when(moduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        assertThatThrownBy(() -> moduleService.deleteModule(id))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
