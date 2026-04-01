package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;
import com.mishchuk.onlineschool.controller.dto.PersonWithEnrollmentsDto;
import com.mishchuk.onlineschool.exception.EmailAlreadyExistsException;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.PersonMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.repository.entity.PersonStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceImplTest {

    @Mock private PersonRepository personRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private PersonMapper personMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private PersonServiceImpl personService;

    private UUID personId;
    private PersonEntity personEntity;
    private PersonEntity adminUser;
    private PersonEntity fakeAdmin;

    @BeforeEach
    void setUp() {
        personId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        personEntity = new PersonEntity();
        personEntity.setId(personId);
        personEntity.setEmail("user@test.com");
        personEntity.setFirstName("Іван");
        personEntity.setLastName("Тест");
        personEntity.setRole(PersonRole.USER);
        personEntity.setPassword("encodedPass");

        adminUser = new PersonEntity();
        adminUser.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(PersonRole.ADMIN);

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
        var auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    // ─────────────────────── createPerson ───────────────────────

    @Test
    @DisplayName("createPerson — успішна реєстрація анонімним користувачем")
    void createPerson_anonymous_success() {
        SecurityContextHolder.clearContext();
        PersonCreateDto dto = new PersonCreateDto(
                "Іван", "Тест", null, null, "new@test.com", "Pass1!", "uk", null);

        when(personRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(personMapper.toEntity(dto)).thenReturn(personEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        personService.createPerson(dto);

        verify(personRepository).save(personEntity);
        verify(emailService).sendWelcomeEmail(eq(personEntity.getEmail()), eq(personEntity.getFirstName()));
    }

    @Test
    @DisplayName("createPerson — ADMIN створює користувача: поле createdBy встановлюється")
    void createPerson_byAdmin_setsCreatedBy() {
        PersonCreateDto dto = new PersonCreateDto(
                "Іван", "Тест", null, null, "new@test.com", "Pass1!", "uk", null);

        when(personRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(personMapper.toEntity(dto)).thenReturn(personEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        personService.createPerson(dto);

        assertThat(personEntity.getCreatedBy()).isSameAs(adminUser);
        verify(personRepository).save(personEntity);
    }

    @Test
    @DisplayName("createPerson — кидає EmailAlreadyExistsException якщо email вже зайнятий")
    void createPerson_duplicateEmail_throws() {
        PersonCreateDto dto = new PersonCreateDto(
                "Іван", "Тест", null, null, "user@test.com", "Pass1!", "uk", null);
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(personEntity));

        assertThatThrownBy(() -> personService.createPerson(dto))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(personRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("createPerson — помилка email не перериває реєстрацію (graceful degradation)")
    void createPerson_emailFails_doesNotThrow() {
        PersonCreateDto dto = new PersonCreateDto(
                "Іван", "Тест", null, null, "new@test.com", "Pass1!", "uk", null);

        when(personRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(personMapper.toEntity(dto)).thenReturn(personEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendWelcomeEmail(anyString(), anyString());

        personService.createPerson(dto);

        verify(personRepository).save(personEntity);
    }

    @Test
    @DisplayName("createPerson — помилка notification не перериває реєстрацію (graceful degradation)")
    void createPerson_notificationFails_doesNotThrow() {
        PersonCreateDto dto = new PersonCreateDto(
                "Іван", "Тест", null, null, "new@test.com", "Pass1!", "uk", null);

        when(personRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(personMapper.toEntity(dto)).thenReturn(personEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        doThrow(new RuntimeException("Notification service down")).when(notificationService)
                .broadcastToAdmins(anyString(), anyString(), any());

        personService.createPerson(dto);

        verify(personRepository).save(personEntity);
    }

    // ─────────────────────── getPerson ───────────────────────

    @Test
    @DisplayName("getPerson — повертає Optional<PersonDto> якщо знайдено")
    void getPerson_found_returnsMappedDto() {
        PersonDto expected = new PersonDto(personId, "Іван", "Тест", null, null,
                "user@test.com", "uk", "USER", null, null, null, null);
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));
        when(personMapper.toDto(personEntity)).thenReturn(expected);

        Optional<PersonDto> result = personService.getPerson(personId);

        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(expected);
    }

    @Test
    @DisplayName("getPerson — повертає empty Optional якщо особу не знайдено")
    void getPerson_notFound_returnsEmpty() {
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThat(personService.getPerson(personId)).isEmpty();
        verify(personMapper, never()).toDto(any());
    }

    // ─────────────────────── getAllPersons ───────────────────────

    @Test
    @DisplayName("getAllPersons — повертає mapped список всіх осіб")
    void getAllPersons_returnsMappedList() {
        PersonDto dto = new PersonDto(personId, "Іван", "Тест", null, null,
                "user@test.com", "uk", "USER", null, null, null, null);

        when(personRepository.findAll()).thenReturn(List.of(personEntity));
        when(personMapper.toDto(personEntity)).thenReturn(dto);

        List<PersonDto> result = personService.getAllPersons();

        assertThat(result).hasSize(1).containsExactly(dto);
    }

    @Test
    @DisplayName("getAllPersons — повертає порожній список якщо осіб немає")
    void getAllPersons_empty_returnsEmptyList() {
        when(personRepository.findAll()).thenReturn(List.of());

        assertThat(personService.getAllPersons()).isEmpty();
        verify(personMapper, never()).toDto(any());
    }

    // ─────────────────────── getAllPersonsWithEnrollments ───────────────────────

    @Test
    @DisplayName("getAllPersonsWithEnrollments — повертає mapped список з зарахуваннями")
    void getAllPersonsWithEnrollments_returnsMappedList() {
        PersonWithEnrollmentsDto dto = new PersonWithEnrollmentsDto(
                personId, "Іван", "Тест", null, null, "user@test.com", "uk", "USER", "ACTIVE",
                List.of(), null, null, null);

        when(personRepository.findAll()).thenReturn(List.of(personEntity));
        when(personMapper.toDtoWithEnrollments(personEntity)).thenReturn(dto);

        List<PersonWithEnrollmentsDto> result = personService.getAllPersonsWithEnrollments();

        assertThat(result).hasSize(1).containsExactly(dto);
    }

    // ─────────────────────── updatePerson ───────────────────────

    @Test
    @DisplayName("updatePerson — ADMIN успішно оновлює будь-яку особу")
    void updatePerson_admin_success() {
        PersonUpdateDto dto = new PersonUpdateDto("USER", "Новий", "Ім'я",
                null, null, "user@test.com", null, null, "uk");

        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        personService.updatePerson(personId, dto);

        verify(personMapper).updateEntityFromDto(dto, personEntity);
        verify(personRepository).save(personEntity);
    }

    @Test
    @DisplayName("updatePerson — FAKE_ADMIN успішно оновлює свого створеного користувача")
    void updatePerson_fakeAdmin_ownCreatedUser_success() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        personEntity.setCreatedBy(fakeAdmin);
        PersonUpdateDto dto = new PersonUpdateDto(null, "Оновлений", null,
                null, null, null, null, null, null);

        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        personService.updatePerson(personId, dto);

        verify(personMapper).updateEntityFromDto(dto, personEntity);
        verify(personRepository).save(personEntity);
    }

    @Test
    @DisplayName("updatePerson — FAKE_ADMIN кидає AccessDeniedException для чужої особи")
    void updatePerson_fakeAdmin_otherUser_throws() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        personEntity.setCreatedBy(adminUser);
        PersonUpdateDto dto = new PersonUpdateDto(null, "Зміна", null,
                null, null, null, null, null, null);

        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        assertThatThrownBy(() -> personService.updatePerson(personId, dto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("modify their own");

        verify(personRepository, never()).save(any());
        verify(personMapper, never()).updateEntityFromDto(any(), any());
    }

    @Test
    @DisplayName("updatePerson — кидає ResourceNotFoundException якщо особу не знайдено")
    void updatePerson_personNotFound_throws() {
        PersonUpdateDto dto = new PersonUpdateDto(null, "X", null,
                null, null, null, null, null, null);

        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.updatePerson(personId, dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(personRepository, never()).save(any());
    }

    // ─────────────────────── deletePerson ───────────────────────

    @Test
    @DisplayName("deletePerson — ADMIN успішно видаляє будь-яку особу")
    void deletePerson_admin_success() {
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        personService.deletePerson(personId);

        verify(personRepository).deleteById(personId);
    }

    @Test
    @DisplayName("deletePerson — FAKE_ADMIN успішно видаляє свого створеного користувача")
    void deletePerson_fakeAdmin_ownCreatedUser_success() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        personEntity.setCreatedBy(fakeAdmin);
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        personService.deletePerson(personId);

        verify(personRepository).deleteById(personId);
    }

    @Test
    @DisplayName("deletePerson — FAKE_ADMIN кидає AccessDeniedException для чужої особи")
    void deletePerson_fakeAdmin_otherUser_throws() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        personEntity.setCreatedBy(adminUser); // створено адміном, не fakeAdmin
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        assertThatThrownBy(() -> personService.deletePerson(personId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("delete their own");

        verify(personRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deletePerson — кидає ResourceNotFoundException якщо особу не знайдено")
    void deletePerson_notFound_throws() {
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.deletePerson(personId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(personRepository, never()).deleteById(any());
    }

    // ─────────────────────── updatePersonStatus ───────────────────────

    @Test
    @DisplayName("updatePersonStatus — ADMIN успішно встановлює BLOCKED, надсилає сповіщення")
    void updatePersonStatus_admin_blocked_success() {
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        personService.updatePersonStatus(personId, "BLOCKED");

        assertThat(personEntity.getStatus()).isEqualTo(PersonStatus.BLOCKED);
        verify(personRepository).save(personEntity);
        verify(notificationService).createNotification(
                eq(personId), anyString(), anyString(), any());
        verify(notificationService).broadcastToAdmins(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("updatePersonStatus — ADMIN успішно встановлює ACTIVE")
    void updatePersonStatus_admin_active_success() {
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        personService.updatePersonStatus(personId, "ACTIVE");

        assertThat(personEntity.getStatus()).isEqualTo(PersonStatus.ACTIVE);
        verify(personRepository).save(personEntity);
    }

    @Test
    @DisplayName("updatePersonStatus — USER кидає AccessDeniedException")
    void updatePersonStatus_regularUser_throws() {
        setSecurityContext("user@test.com", "ROLE_USER");
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(personEntity));
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        assertThatThrownBy(() -> personService.updatePersonStatus(personId, "BLOCKED"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Regular users");

        verify(personRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePersonStatus — FAKE_ADMIN кидає AccessDeniedException для чужої особи")
    void updatePersonStatus_fakeAdmin_otherUser_throws() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        personEntity.setCreatedBy(adminUser);
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        assertThatThrownBy(() -> personService.updatePersonStatus(personId, "BLOCKED"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("FAKE_ADMIN");

        verify(personRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePersonStatus — кидає IllegalArgumentException якщо статус невалідний")
    void updatePersonStatus_invalidStatus_throws() {
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        assertThatThrownBy(() -> personService.updatePersonStatus(personId, "UNKNOWN_STATUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");

        verify(personRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePersonStatus — помилка notification не перериває зміну статусу")
    void updatePersonStatus_notificationFails_doesNotThrow() {
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));
        doThrow(new RuntimeException("notif down")).when(notificationService)
                .createNotification(any(), any(), any(), any());

        personService.updatePersonStatus(personId, "BLOCKED");

        // Статус таки збережений
        assertThat(personEntity.getStatus()).isEqualTo(PersonStatus.BLOCKED);
        verify(personRepository).save(personEntity);
    }

    // ─────────────────────── addCourseAccess ───────────────────────

    @Test
    @DisplayName("addCourseAccess — ADMIN успішно зараховує користувача на курс")
    void addCourseAccess_admin_success() {
        UUID courseId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        CourseEntity course = new CourseEntity();
        course.setId(courseId);
        course.setName("Java Spring Boot");

        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(personId, courseId))
                .thenReturn(Optional.empty());

        personService.addCourseAccess(personId, courseId);

        ArgumentCaptor<EnrollmentEntity> captor = ArgumentCaptor.forClass(EnrollmentEntity.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getStudent()).isSameAs(personEntity);
        assertThat(captor.getValue().getCourse()).isSameAs(course);
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        verify(emailService).sendCourseAccessGrantedEmail(
                eq(personEntity.getEmail()), eq(personEntity.getFirstName()), eq("Java Spring Boot"));
    }

    @Test
    @DisplayName("addCourseAccess — кидає IllegalArgumentException якщо вже зарахований")
    void addCourseAccess_alreadyEnrolled_throws() {
        UUID courseId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(courseId);

        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(personId, courseId))
                .thenReturn(Optional.of(new EnrollmentEntity()));

        assertThatThrownBy(() -> personService.addCourseAccess(personId, courseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already enrolled");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("addCourseAccess — кидає ResourceNotFoundException якщо курс не існує")
    void addCourseAccess_courseNotFound_throws() {
        UUID courseId = UUID.randomUUID();

        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.addCourseAccess(personId, courseId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course not found");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("addCourseAccess — FAKE_ADMIN кидає AccessDeniedException для чужого студента")
    void addCourseAccess_fakeAdmin_otherUser_throws() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        UUID courseId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(courseId);

        personEntity.setCreatedBy(adminUser); // не fakeAdmin
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> personService.addCourseAccess(personId, courseId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("FAKE_ADMIN");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("addCourseAccess — помилка email не перериває зарахування")
    void addCourseAccess_emailFails_doesNotThrow() {
        UUID courseId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(courseId);
        course.setName("Курс");

        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(personId, courseId))
                .thenReturn(Optional.empty());
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendCourseAccessGrantedEmail(anyString(), anyString(), anyString());

        personService.addCourseAccess(personId, courseId);

        verify(enrollmentRepository).save(any());
    }

    // ─────────────────────── removeCourseAccess ───────────────────────

    @Test
    @DisplayName("removeCourseAccess — ADMIN успішно видаляє зарахування")
    void removeCourseAccess_admin_success() {
        UUID courseId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(courseId);
        course.setName("Курс");

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStudent(personEntity);
        enrollment.setCourse(course);

        when(enrollmentRepository.findByStudentIdAndCourseId(personId, courseId))
                .thenReturn(Optional.of(enrollment));

        personService.removeCourseAccess(personId, courseId);

        verify(enrollmentRepository).delete(enrollment);
        verify(emailService).sendCourseAccessRevokedEmail(
                eq(personEntity.getEmail()), eq(personEntity.getFirstName()), eq("Курс"));
    }

    @Test
    @DisplayName("removeCourseAccess — кидає ResourceNotFoundException якщо зарахування не існує")
    void removeCourseAccess_enrollmentNotFound_throws() {
        UUID courseId = UUID.randomUUID();
        when(enrollmentRepository.findByStudentIdAndCourseId(personId, courseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.removeCourseAccess(personId, courseId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Enrollment not found");

        verify(enrollmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removeCourseAccess — FAKE_ADMIN кидає AccessDeniedException для чужого студента")
    void removeCourseAccess_fakeAdmin_otherUser_throws() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        UUID courseId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(courseId);

        PersonEntity student = new PersonEntity();
        student.setId(personId);
        student.setCreatedBy(adminUser); // не fakeAdmin

        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStudent(student);
        enrollment.setCourse(course);

        when(enrollmentRepository.findByStudentIdAndCourseId(personId, courseId))
                .thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> personService.removeCourseAccess(personId, courseId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("FAKE_ADMIN");

        verify(enrollmentRepository, never()).delete(any());
    }

    // ─────────────────────── changePassword ───────────────────────

    @Test
    @DisplayName("changePassword — успішна зміна пароля за правильним старим паролем")
    void changePassword_success() {
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));
        when(passwordEncoder.matches("oldPass", "encodedPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newEncoded");

        personService.changePassword(personId, "oldPass", "newPass");

        assertThat(personEntity.getPassword()).isEqualTo("newEncoded");
        verify(personRepository).save(personEntity);
    }

    @Test
    @DisplayName("changePassword — кидає IllegalArgumentException якщо старий пароль невірний")
    void changePassword_wrongOldPassword_throws() {
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));
        when(passwordEncoder.matches("wrong", "encodedPass")).thenReturn(false);

        assertThatThrownBy(() -> personService.changePassword(personId, "wrong", "newPass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid old password");

        verify(personRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword — кидає ResourceNotFoundException якщо особу не знайдено")
    void changePassword_personNotFound_throws() {
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.changePassword(personId, "old", "new"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(personRepository, never()).save(any());
    }
}
