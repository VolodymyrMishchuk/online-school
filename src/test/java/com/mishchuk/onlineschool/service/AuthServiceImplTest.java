package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.LessonRepository;
import com.mishchuk.onlineschool.repository.ModuleRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;
import com.mishchuk.onlineschool.scheduler.DemoCleanupScheduler;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 *
 * NOTE: Тести фокусуються виключно на logout() флоу, бо інші методи (authenticateUser, registerUser,
 * refreshToken, magicLogin) потребують реального AuthenticationManager + UserDetails піплайну,
 * який практичніше перевіряти у інтеграційному тесті з @SpringBootTest.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private PersonService personService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;

    @Mock private PersonRepository personRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ModuleRepository moduleRepository;
    @Mock private LessonRepository lessonRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private final UUID personId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        DemoCleanupScheduler scheduler = new DemoCleanupScheduler(
                personRepository, courseRepository, moduleRepository, lessonRepository);
        ReflectionTestUtils.setField(authService, "demoCleanupScheduler", scheduler);
    }

    // ─────────────────────── logout ───────────────────────

    @Test
    @DisplayName("logout — нічого не робить якщо refreshToken = null")
    void logout_nullToken_doesNothing() {
        authService.logout(null);

        verifyNoInteractions(refreshTokenService, personService,
                personRepository, courseRepository, moduleRepository, lessonRepository);
    }

    @Test
    @DisplayName("logout — видаляє токен за personId для звичайного USER")
    void logout_validToken_regularUser_deletesTokenByPersonId() {
        RefreshTokenEntity tokenEntity = buildToken(personId, "some.refresh.token");
        PersonDto personDto = buildPersonDto(personId, "USER");

        when(refreshTokenService.findByToken("some.refresh.token")).thenReturn(tokenEntity);
        when(personService.getPerson(personId)).thenReturn(Optional.of(personDto));

        authService.logout("some.refresh.token");

        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(refreshTokenService).deleteByPersonId(uuidCaptor.capture());
        assertThat(uuidCaptor.getValue()).isEqualTo(personId);

        verify(personRepository, never()).findById(any());
        verifyNoInteractions(courseRepository, moduleRepository, lessonRepository);
    }

    @Test
    @DisplayName("logout — очищає дані FAKE_USER при виході (виклик cleanupDataForUser)")
    void logout_fakeUser_triggersCleanup() {
        RefreshTokenEntity tokenEntity = buildToken(personId, "fake.token");
        PersonDto fakeUserDto = buildPersonDto(personId, "FAKE_USER");

        when(refreshTokenService.findByToken("fake.token")).thenReturn(tokenEntity);
        when(personService.getPerson(personId)).thenReturn(Optional.of(fakeUserDto));
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        authService.logout("fake.token");

        verify(refreshTokenService).deleteByPersonId(personId);
        verify(personRepository).findById(personId);
    }

    @Test
    @DisplayName("logout — очищає дані FAKE_ADMIN при виході (виклик cleanupDataForUser)")
    void logout_fakeAdmin_triggersCleanup() {
        RefreshTokenEntity tokenEntity = buildToken(personId, "fakeadmin.token");
        PersonDto fakeAdminDto = buildPersonDto(personId, "FAKE_ADMIN");

        when(refreshTokenService.findByToken("fakeadmin.token")).thenReturn(tokenEntity);
        when(personService.getPerson(personId)).thenReturn(Optional.of(fakeAdminDto));
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        authService.logout("fakeadmin.token");

        verify(refreshTokenService).deleteByPersonId(personId);
        verify(personRepository).findById(personId);
    }

    @Test
    @DisplayName("logout — якщо person не знайдено, все одно видаляє токен")
    void logout_personNotFound_stillDeletesToken() {
        RefreshTokenEntity tokenEntity = buildToken(personId, "orphan.token");

        when(refreshTokenService.findByToken("orphan.token")).thenReturn(tokenEntity);
        when(personService.getPerson(personId)).thenReturn(Optional.empty());

        authService.logout("orphan.token");

        verify(refreshTokenService).deleteByPersonId(personId);
        verifyNoInteractions(personRepository, courseRepository, moduleRepository, lessonRepository);
    }

    @Test
    @DisplayName("logout — не кидає виключень якщо refreshTokenService падає з RuntimeException")
    void logout_tokenNotFound_swallowsException() {
        when(refreshTokenService.findByToken("invalid")).thenThrow(new RuntimeException("Not found"));

        authService.logout("invalid");

        verify(refreshTokenService, never()).deleteByPersonId(any());
    }

    // ─────────────────────── helpers ───────────────────────

    private RefreshTokenEntity buildToken(UUID id, String token) {
        return RefreshTokenEntity.builder()
                .token(token)
                .personId(id)
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .build();
    }

    private PersonDto buildPersonDto(UUID id, String role) {
        return new PersonDto(id, "Тест", "Юзер", null, null,
                "user@test.com", "uk", role, null, null, null, null);
    }
}
