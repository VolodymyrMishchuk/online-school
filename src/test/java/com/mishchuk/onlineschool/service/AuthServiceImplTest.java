package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;
import com.mishchuk.onlineschool.scheduler.DemoCleanupScheduler;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.LessonRepository;
import com.mishchuk.onlineschool.repository.ModuleRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 *
 * Note: JwtUtils cannot be mocked on Java 23 (ByteBuddy limitation).
 * Tests focus exclusively on the logout() flow which doesn't require JWT generation:
 * — null token guard
 * — successful token deletion
 * — FAKE_ADMIN demo data cleanup
 * — graceful error handling
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private PersonService personService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;

    // DemoCleanupScheduler is a concrete @Component — construct manually with mocked repositories
    @Mock private PersonRepository personRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ModuleRepository moduleRepository;
    @Mock private LessonRepository lessonRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setup() {
        // Build a real DemoCleanupScheduler with mocked repositories
        DemoCleanupScheduler scheduler = new DemoCleanupScheduler(
                personRepository, courseRepository, moduleRepository, lessonRepository);
        ReflectionTestUtils.setField(authService, "demoCleanupScheduler", scheduler);
    }

    // --- logout ---

    @Test
    @DisplayName("logout — нічого не робить якщо refreshToken = null")
    void logout_nullToken_doesNothing() {
        authService.logout(null);

        verifyNoInteractions(refreshTokenService, personService);
    }

    @Test
    @DisplayName("logout — видаляє токен для звичайного USER")
    void logout_validToken_deletesRefreshToken() {
        UUID personId = UUID.randomUUID();
        RefreshTokenEntity tokenEntity = RefreshTokenEntity.builder()
                .token("some.refresh.token")
                .personId(personId)
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .build();

        PersonDto personDto = new PersonDto(personId, "Іван", "Петрук", null, null,
                "user@test.com", "uk", "USER", null, null, null, null);

        when(refreshTokenService.findByToken("some.refresh.token")).thenReturn(tokenEntity);
        when(personService.getPerson(personId)).thenReturn(Optional.of(personDto));

        authService.logout("some.refresh.token");

        verify(refreshTokenService).deleteByPersonId(personId);
        verifyNoInteractions(personRepository); // no cleanup for regular users
    }

    @Test
    @DisplayName("logout — очищає дані FAKE_USER при виході")
    void logout_fakeUser_triggersCleanup() {
        UUID personId = UUID.randomUUID();
        RefreshTokenEntity tokenEntity = RefreshTokenEntity.builder()
                .token("fake.token")
                .personId(personId)
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .build();

        PersonDto fakeUserDto = new PersonDto(personId, "Фейк", "Юзер", null, null,
                "fake@test.com", "uk", "FAKE_USER", null, null, null, null);

        when(refreshTokenService.findByToken("fake.token")).thenReturn(tokenEntity);
        when(personService.getPerson(personId)).thenReturn(Optional.of(fakeUserDto));
        // scheduler.cleanupDataForUser will call personRepository.findById
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        authService.logout("fake.token");

        verify(refreshTokenService).deleteByPersonId(personId);
        // personRepository was consulted for the cleanup
        verify(personRepository).findById(personId);
    }

    @Test
    @DisplayName("logout — не кидає виключень якщо токен не знайдено")
    void logout_tokenNotFound_doesNotThrow() {
        when(refreshTokenService.findByToken("invalid")).thenThrow(new RuntimeException("Not found"));

        // Should NOT throw — error is caught internally
        authService.logout("invalid");
    }
}
