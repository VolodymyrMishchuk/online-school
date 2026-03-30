package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.exception.BadRequestException;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.CourseMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.CourseReviewRequestRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.math.BigDecimal;
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

    @BeforeEach
    void setupSecurityContext() {
        var auth = new UsernamePasswordAuthenticationToken("admin@test.com", null);
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));

        PersonEntity admin = new PersonEntity();
        admin.setEmail("admin@test.com");
        lenient().when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
    }

    // --- createCourse ---

    @Test
    @DisplayName("createCourse — кидає BadRequestException якщо задано і % і суму знижки")
    void createCourse_dualDiscount_throws() {
        // promotionalDiscountPercentage and promotionalDiscountAmount both set
        CourseCreateDto dto = new CourseCreateDto(
                "Тест курс", "Опис",
                null, null,     // price, discountAmount
                null,           // discountPercentage
                null,           // accessDuration
                10,             // promotionalDiscountPercentage (Integer)
                BigDecimal.valueOf(5), // promotionalDiscountAmount
                null,           // nextCourseId
                null);          // moduleIds

        assertThatThrownBy(() -> courseService.createCourse(dto, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("createCourse — зберігає курс без обкладинки")
    void createCourse_success_noImage() {
        CourseCreateDto dto = new CourseCreateDto(
                "Тест курс", "Опис",
                BigDecimal.valueOf(100), null,
                null,
                null,
                null, null,
                null, null);

        when(courseMapper.toEntity(dto)).thenReturn(new CourseEntity());

        courseService.createCourse(dto, null);

        verify(courseRepository).save(any(CourseEntity.class));
    }

    // --- getCourse ---

    @Test
    @DisplayName("getCourse — повертає Optional з DTO якщо курс знайдено")
    void getCourse_found() {
        UUID id = UUID.randomUUID();
        CourseEntity entity = new CourseEntity();
        entity.setId(id);
        CourseDto dto = new CourseDto(
                id, "Test", null, 0, 0, 0, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        when(courseRepository.findById(id)).thenReturn(Optional.of(entity));
        when(courseMapper.toDto(entity)).thenReturn(dto);

        Optional<CourseDto> result = courseService.getCourse(id);

        assertThat(result).isPresent().contains(dto);
    }

    @Test
    @DisplayName("getCourse — повертає empty Optional якщо не знайдено")
    void getCourse_notFound() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        Optional<CourseDto> result = courseService.getCourse(id);

        assertThat(result).isEmpty();
    }

    // --- deleteCourse ---

    @Test
    @DisplayName("deleteCourse — кидає ResourceNotFoundException якщо не знайдено")
    void deleteCourse_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.deleteCourse(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- updateCourse ---

    @Test
    @DisplayName("updateCourse — кидає BadRequestException для конфлікту знижок")
    void updateCourse_dualDiscount_throws() {
        UUID id = UUID.randomUUID();
        com.mishchuk.onlineschool.controller.dto.CourseUpdateDto dto =
                new com.mishchuk.onlineschool.controller.dto.CourseUpdateDto(
                        "Назва", "Опис",
                        BigDecimal.valueOf(100), null, // price, discountAmount
                        null,   // discountPercentage
                        null,   // accessDuration
                        null,   // status
                        10,     // promotionalDiscountPercentage
                        BigDecimal.valueOf(5), // promotionalDiscountAmount
                        null,   // nextCourseId
                        null,   // moduleIds
                        null);  // deleteCoverImage

        assertThatThrownBy(() -> courseService.updateCourse(id, dto, null))
                .isInstanceOf(BadRequestException.class);
    }


    // --- cloneCourse ---

    @Test
    @DisplayName("cloneCourse — зберігає копію з суфіксом (Copy) та відповідною версією")
    void cloneCourse_savesWithCopySuffix() {
        UUID id = UUID.randomUUID();
        CourseEntity original = new CourseEntity();
        original.setId(id);
        original.setName("Пологи");
        original.setVersion("1.0");

        PersonEntity admin = new PersonEntity();
        when(courseRepository.findById(id)).thenReturn(Optional.of(original));

        courseService.cloneCourse(id);

        ArgumentCaptor<CourseEntity> captor = ArgumentCaptor.forClass(CourseEntity.class);
        verify(courseRepository).save(captor.capture());
        CourseEntity cloned = captor.getValue();
        assertThat(cloned.getName()).endsWith("(Copy)");
        assertThat(cloned.getVersion()).isEqualTo("2.0");
    }
}
