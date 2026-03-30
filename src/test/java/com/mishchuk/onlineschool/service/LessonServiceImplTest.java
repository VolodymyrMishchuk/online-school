package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.LessonCreateDto;
import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.mapper.LessonMapper;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.LessonRepository;
import com.mishchuk.onlineschool.repository.ModuleRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.LessonEntity;
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
class LessonServiceImplTest {

    @Mock private LessonRepository lessonRepository;
    @Mock private ModuleRepository moduleRepository;
    @Mock private LessonMapper lessonMapper;
    @Mock private PersonRepository personRepository;
    @Mock private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private LessonServiceImpl lessonService;

    @BeforeEach
    void mockAdminContext() {
        PersonEntity admin = new PersonEntity();
        admin.setRole(PersonRole.ADMIN);
        admin.setEmail("admin@test.com");

        var auth = new UsernamePasswordAuthenticationToken("admin@test.com", null);
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));

        lenient().when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
    }

    // --- createLesson ---

    @Test
    @DisplayName("createLesson — зберігає урок без модуля якщо moduleId = null")
    void createLesson_noModule_success() {
        LessonCreateDto dto = new LessonCreateDto(null, "Урок 1", "Опис", "https://video.url", 30);
        LessonEntity entity = new LessonEntity();
        LessonDto expected = new LessonDto(
                null, null, "Урок 1", "Опис", "https://video.url", 30, null, null, null, null, null, null);

        when(lessonMapper.toEntity(dto)).thenReturn(entity);
        when(lessonRepository.save(entity)).thenReturn(entity);
        when(lessonMapper.toDto(entity)).thenReturn(expected);

        LessonDto result = lessonService.createLesson(dto);
        assertThat(result).isEqualTo(expected);
        verify(moduleRepository, never()).findById(any());
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
    }

    // --- getAllLessons ---

    @Test
    @DisplayName("getAllLessons — повертає список DTO")
    void getAllLessons_returnsList() {
        LessonEntity e = new LessonEntity();
        LessonDto dto = new LessonDto(null, null, "Урок", null, null, 0, null, null, null, null, null, null);

        when(lessonRepository.findAll()).thenReturn(List.of(e));
        when(lessonMapper.toDto(e)).thenReturn(dto);

        List<LessonDto> result = lessonService.getAllLessons();
        assertThat(result).hasSize(1).containsExactly(dto);
    }

    // --- getLessonsByModule ---

    @Test
    @DisplayName("getLessonsByModule — повертає уроки за модулем")
    void getLessonsByModule_returnsList() {
        UUID moduleId = UUID.randomUUID();
        LessonEntity e = new LessonEntity();
        LessonDto dto = new LessonDto(null, moduleId, "Урок", null, null, 0, null, null, null, null, null, null);

        when(lessonRepository.findByModuleId(moduleId)).thenReturn(List.of(e));
        when(lessonMapper.toDto(e)).thenReturn(dto);

        List<LessonDto> result = lessonService.getLessonsByModule(moduleId);
        assertThat(result).hasSize(1).containsExactly(dto);
    }

    // --- deleteLesson ---

    @Test
    @DisplayName("deleteLesson — кидає RuntimeException якщо урок не знайдено")
    void deleteLesson_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(lessonRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.deleteLesson(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lesson not found");
    }

    @Test
    @DisplayName("deleteLesson — видаляє урок для ADMIN")
    void deleteLesson_admin_success() {
        UUID id = UUID.randomUUID();
        PersonEntity admin = new PersonEntity();
        admin.setRole(PersonRole.ADMIN);

        LessonEntity lesson = new LessonEntity();
        lesson.setId(id);

        when(lessonRepository.findById(id)).thenReturn(Optional.of(lesson));

        lessonService.deleteLesson(id);

        verify(lessonRepository).deleteById(id);
    }
}
