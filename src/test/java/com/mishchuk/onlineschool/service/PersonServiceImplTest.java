package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.exception.EmailAlreadyExistsException;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.PersonMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
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
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @BeforeEach
    void setUp() {
        personId = UUID.randomUUID();
        personEntity = new PersonEntity();
        personEntity.setId(personId);
        personEntity.setEmail("user@test.com");
        personEntity.setFirstName("Тест");
        personEntity.setLastName("Юзер");
        personEntity.setRole(PersonRole.USER);
        personEntity.setPassword("encodedPass");
    }

    private void mockAnonymousContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockAdminContext(PersonEntity admin) {
        var auth = new UsernamePasswordAuthenticationToken("admin@test.com", null);
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
        lenient().when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
    }

    // --- createPerson ---

    @Test
    @DisplayName("createPerson — успішна реєстрація нового користувача")
    void createPerson_success() {
        mockAnonymousContext();
        PersonCreateDto dto = new PersonCreateDto(
                "Тест", "Юзер", null, null, "user@test.com", "Pass1!", "uk", null);

        when(personRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
        when(personMapper.toEntity(dto)).thenReturn(personEntity);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        personService.createPerson(dto);

        verify(personRepository).save(personEntity);
    }

    @Test
    @DisplayName("createPerson — кидає EmailAlreadyExistsException якщо email зайнятий")
    void createPerson_duplicateEmail_throws() {
        PersonCreateDto dto = new PersonCreateDto(
                "Тест", "Юзер", null, null, "user@test.com", "Pass1!", "uk", null);
        when(personRepository.findByEmail(dto.email())).thenReturn(Optional.of(personEntity));

        assertThatThrownBy(() -> personService.createPerson(dto))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    // --- getPerson ---

    @Test
    @DisplayName("getPerson — повертає Optional DTO якщо знайдено")
    void getPerson_found() {
        PersonDto expected = new PersonDto(personId, "Тест", "Юзер", null, null,
                "user@test.com", "uk", "USER", null, null, null, null);
        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));
        when(personMapper.toDto(personEntity)).thenReturn(expected);

        assertThat(personService.getPerson(personId)).isPresent();
    }

    @Test
    @DisplayName("getPerson — повертає empty Optional якщо не знайдено")
    void getPerson_notFound() {
        when(personRepository.findById(personId)).thenReturn(Optional.empty());
        assertThat(personService.getPerson(personId)).isEmpty();
    }

    // --- deletePerson ---

    @Test
    @DisplayName("deletePerson — кидає ResourceNotFoundException якщо не знайдено")
    void deletePerson_notFound_throws() {
        PersonEntity admin = new PersonEntity();
        admin.setRole(PersonRole.ADMIN);
        mockAdminContext(admin);

        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.deletePerson(personId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deletePerson — ADMIN може видалити будь-якого користувача")
    void deletePerson_admin_success() {
        PersonEntity admin = new PersonEntity();
        admin.setRole(PersonRole.ADMIN);
        mockAdminContext(admin);

        when(personRepository.findById(personId)).thenReturn(Optional.of(personEntity));

        personService.deletePerson(personId);

        verify(personRepository).deleteById(personId);
    }

    // --- changePassword ---

    @Test
    @DisplayName("changePassword — успішна зміна пароля")
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
        when(passwordEncoder.matches("wrongOld", "encodedPass")).thenReturn(false);

        assertThatThrownBy(() -> personService.changePassword(personId, "wrongOld", "newPass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid old password");
    }

    @Test
    @DisplayName("changePassword — кидає ResourceNotFoundException якщо особу не знайдено")
    void changePassword_personNotFound_throws() {
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.changePassword(personId, "old", "new"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
