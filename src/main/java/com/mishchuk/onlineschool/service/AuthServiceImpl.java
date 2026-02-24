package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.AuthRequest;
import com.mishchuk.onlineschool.controller.dto.AuthResponse;
import com.mishchuk.onlineschool.controller.dto.AuthResultDto;
import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.scheduler.DemoCleanupScheduler;
import com.mishchuk.onlineschool.repository.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final PersonService personService;
    private final RefreshTokenService refreshTokenService;
    private final com.mishchuk.onlineschool.service.email.EmailService emailService;
    private final NotificationService notificationService;
    private final DemoCleanupScheduler demoCleanupScheduler;

    @Override
    @Transactional
    public AuthResultDto registerUser(PersonCreateDto request) {
        log.info("Registering user: {}", request.email());

        // Create user
        personService.createPerson(request);

        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String accessToken = jwtUtils.generateToken(userDetails);
        PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(request.email());

        // Create refresh token
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(person.getId());

        // Send welcome email
        emailService.sendWelcomeEmail(request.email(), request.firstName() + " " + request.lastName());

        // Notify Admins
        notificationService.broadcastToAdmins(
                "Нова реєстрація",
                "Новий користувач зареєструвався: " + request.firstName() + " " + request.lastName()
                        + " (" + request.email() + ")",
                NotificationType.NEW_USER_REGISTRATION);

        // Notify User
        notificationService.createNotification(
                person.getId(),
                "Ласкаво просимо!",
                "Вітаємо в Svitlo School! Ми раді, що ви з нами. Перегляньте доступні курси.",
                NotificationType.GENERIC);

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                person.getId(),
                person.getRole() != null ? person.getRole().name() : "USER",
                person.getFirstName(),
                person.getLastName());

        return new AuthResultDto(authResponse, refreshToken.getToken());
    }

    @Override
    @Transactional
    public AuthResultDto authenticateUser(AuthRequest request) {
        log.info("Authenticating user: {}", request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String accessToken = jwtUtils.generateToken(userDetails);
        PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(request.getEmail());

        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(person.getId());

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                person.getId(),
                person.getRole() != null ? person.getRole().name() : "USER",
                person.getFirstName(),
                person.getLastName());

        return new AuthResultDto(authResponse, refreshToken.getToken());
    }

    @Override
    @Transactional
    public AuthResultDto refreshToken(String refreshToken) {
        log.info("Refreshing token");

        // Rotate refresh token
        RefreshTokenEntity newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken);

        UUID personId = newRefreshToken.getPersonId();
        com.mishchuk.onlineschool.controller.dto.PersonDto person = personService.getPerson(personId)
                .orElseThrow(() -> new RuntimeException("Person not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(person.email());
        String newAccessToken = jwtUtils.generateToken(userDetails);

        log.info("Tokens refreshed successfully for person: {}", person.id());

        AuthResponse authResponse = new AuthResponse(
                newAccessToken,
                person.id(),
                person.role() != null ? person.role() : "USER",
                person.firstName(),
                person.lastName());

        return new AuthResultDto(authResponse, newRefreshToken.getToken());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        log.info("Logging out user");
        if (refreshToken == null)
            return;

        try {
            RefreshTokenEntity tokenEntity = refreshTokenService.findByToken(refreshToken);

            com.mishchuk.onlineschool.controller.dto.PersonDto person = personService
                    .getPerson(tokenEntity.getPersonId()).orElse(null);
            if (person != null && ("FAKE_ADMIN".equals(person.role()) || "FAKE_USER".equals(person.role()))) {
                demoCleanupScheduler.cleanupDataForUser(person.id());
            }

            refreshTokenService.deleteByPersonId(tokenEntity.getPersonId());
        } catch (Exception e) {
            log.warn("Error during logout (token may be invalid or already deleted): {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public AuthResultDto magicLogin(String token) {
        if (token == null || !jwtUtils.validateMagicToken(token)) {
            return null; // Signals invalid token to controller
        }

        String email = jwtUtils.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String accessToken = jwtUtils.generateToken(userDetails);
        PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(email);

        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(person.getId());

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                person.getId(),
                person.getRole() != null ? person.getRole().name() : "USER",
                person.getFirstName(),
                person.getLastName());

        return new AuthResultDto(authResponse, refreshToken.getToken());
    }
}
