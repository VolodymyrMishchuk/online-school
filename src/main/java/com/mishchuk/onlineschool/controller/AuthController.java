package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.AuthRequest;
import com.mishchuk.onlineschool.controller.dto.AuthResponse;
import com.mishchuk.onlineschool.controller.dto.ChangePasswordRequest;
import com.mishchuk.onlineschool.controller.dto.ForgotPasswordRequest;
import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.ResetPasswordRequest;
import java.util.UUID;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.service.PersonService;
import com.mishchuk.onlineschool.service.RefreshTokenService;
import com.mishchuk.onlineschool.scheduler.DemoCleanupScheduler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

        private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
        private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

        private final AuthenticationManager authenticationManager;
        private final UserDetailsService userDetailsService;
        private final JwtUtils jwtUtils;
        private final PersonService personService;
        private final RefreshTokenService refreshTokenService;
        private final com.mishchuk.onlineschool.service.email.EmailService emailService;
        private final com.mishchuk.onlineschool.service.PasswordResetService passwordResetService;
        private final com.mishchuk.onlineschool.service.NotificationService notificationService;
        private final DemoCleanupScheduler demoCleanupScheduler;

        @PostMapping("/register")
        public ResponseEntity<AuthResponse> register(
                        @Valid @RequestBody PersonCreateDto request,
                        HttpServletResponse response) {
                log.info("Registration request received for email: {}", request.email());

                // Створюємо користувача
                personService.createPerson(request);

                // Автентифікуємо щойно створеного користувача
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

                final UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
                final String accessToken = jwtUtils.generateToken(userDetails);
                PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(request.email());

                // Створюємо refresh token
                RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(person.getId());
                setRefreshTokenCookie(response, refreshToken.getToken());

                log.info("User registered and authenticated successfully: {}", request.email());

                // Send welcome email
                emailService.sendWelcomeEmail(request.email(), request.firstName() + " " + request.lastName());

                // Notify Admins
                notificationService.broadcastToAdmins(
                                "Нова реєстрація",
                                "Новий користувач зареєструвався: " + request.firstName() + " " + request.lastName()
                                                + " (" + request.email() + ")",
                                com.mishchuk.onlineschool.repository.entity.NotificationType.NEW_USER_REGISTRATION);

                // Notify User
                notificationService.createNotification(
                                person.getId(),
                                "Ласкаво просимо!",
                                "Вітаємо в Svitlo School! Ми раді, що ви з нами. Перегляньте доступні курси.",
                                com.mishchuk.onlineschool.repository.entity.NotificationType.GENERIC);

                // Повертаємо 200 OK замість 201 Created
                return ResponseEntity.ok(new AuthResponse(accessToken, person.getId(),
                                person.getRole() != null ? person.getRole().name() : "USER",
                                person.getFirstName(), person.getLastName()));
        }

        @PostMapping("/login")
        public ResponseEntity<AuthResponse> login(
                        @RequestBody AuthRequest request,
                        HttpServletResponse response) {
                log.info("Login request received for email: {}", request.getEmail());

                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(), request.getPassword()));

                final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
                final String accessToken = jwtUtils.generateToken(userDetails);
                PersonEntity person = ((CustomUserDetailsService) userDetailsService)
                                .getPerson(request.getEmail());

                // Створюємо refresh token
                RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(person.getId());
                setRefreshTokenCookie(response, refreshToken.getToken());

                log.info("User logged in successfully: {}", request.getEmail());

                return ResponseEntity.ok(new AuthResponse(accessToken, person.getId(),
                                person.getRole() != null ? person.getRole().name() : "USER",
                                person.getFirstName(), person.getLastName()));
        }

        @PostMapping("/refresh")
        public ResponseEntity<AuthResponse> refreshToken(
                        HttpServletRequest request,
                        HttpServletResponse response) {
                log.info("Token refresh request received");

                String refreshToken = getRefreshTokenFromCookie(request);
                if (refreshToken == null) {
                        log.warn("Refresh token not found in cookies");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                try {
                        // Rotate refresh token (старий видаляється, створюється новий)
                        RefreshTokenEntity newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken);

                        // Генеруємо новий access token
                        // Генеруємо новий access token
                        UUID personId = newRefreshToken.getPersonId();
                        com.mishchuk.onlineschool.controller.dto.PersonDto person = personService.getPerson(personId)
                                        .orElseThrow(() -> new RuntimeException("Person not found"));

                        UserDetails userDetails = userDetailsService.loadUserByUsername(person.email());
                        String newAccessToken = jwtUtils.generateToken(userDetails);

                        // Встановлюємо новий refresh token cookie
                        setRefreshTokenCookie(response, newRefreshToken.getToken());

                        log.info("Tokens refreshed successfully for person: {}", person.id());

                        return ResponseEntity.ok(new AuthResponse(newAccessToken, person.id(),
                                        person.role() != null ? person.role() : "USER",
                                        person.firstName(), person.lastName()));
                } catch (Exception e) {
                        log.error("Token refresh failed: {}", e.getMessage());
                        clearRefreshTokenCookie(response);
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
        }

        @PostMapping("/logout")
        public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
                log.info("Logout request received");

                String refreshToken = getRefreshTokenFromCookie(request);
                if (refreshToken != null) {
                        try {
                                RefreshTokenEntity tokenEntity = refreshTokenService.findByToken(refreshToken);

                                com.mishchuk.onlineschool.controller.dto.PersonDto person = personService
                                                .getPerson(tokenEntity.getPersonId()).orElse(null);
                                if (person != null && ("FAKE_ADMIN".equals(person.role())
                                                || "FAKE_USER".equals(person.role()))) {
                                        demoCleanupScheduler.cleanupDataForUser(person.id());
                                }

                                refreshTokenService.deleteByPersonId(tokenEntity.getPersonId());
                                log.info("User logged out successfully");
                        } catch (Exception e) {
                                log.warn("Error during logout: {}", e.getMessage());
                        }
                }

                clearRefreshTokenCookie(response);
                return ResponseEntity.noContent().build();
        }

        @PostMapping("/magic-login")
        public ResponseEntity<AuthResponse> magicLogin(@RequestBody java.util.Map<String, String> request,
                        HttpServletResponse response) {
                String token = request.get("token");
                if (token == null || !jwtUtils.validateMagicToken(token)) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                String email = jwtUtils.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                final String accessToken = jwtUtils.generateToken(userDetails);
                PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(email);

                RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(person.getId());
                setRefreshTokenCookie(response, refreshToken.getToken());

                return ResponseEntity.ok(new AuthResponse(accessToken, person.getId(),
                                person.getRole() != null ? person.getRole().name() : "USER",
                                person.getFirstName(), person.getLastName()));
        }

        // Password Reset Endpoints

        @PostMapping("/forgot-password")
        public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
                log.info("Forgot password request for email: {}", request.email());
                passwordResetService.initiatePasswordReset(request.email());
                return ResponseEntity.ok().build();
        }

        @PostMapping("/reset-password")
        public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
                log.info("Reset password request received");
                passwordResetService.resetPassword(request.token(), request.newPassword());
                return ResponseEntity.ok().build();
        }

        // Helper methods for cookie management

        @PostMapping("/change-password")
        public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                        java.security.Principal principal) {
                log.info("Change password request received for user: {}", principal.getName());
                PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(principal.getName());
                personService.changePassword(person.getId(), request.oldPassword(), request.newPassword());
                return ResponseEntity.ok().build();
        }

        private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
                Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
                cookie.setHttpOnly(true);
                cookie.setSecure(false); // Set to true in production with HTTPS
                cookie.setPath("/");
                cookie.setMaxAge(REFRESH_TOKEN_COOKIE_MAX_AGE);
                cookie.setAttribute("SameSite", "Lax");
                response.addCookie(cookie);
        }

        private String getRefreshTokenFromCookie(HttpServletRequest request) {
                if (request.getCookies() == null) {
                        return null;
                }
                return Arrays.stream(request.getCookies())
                                .filter(cookie -> REFRESH_TOKEN_COOKIE.equals(cookie.getName()))
                                .map(Cookie::getValue)
                                .findFirst()
                                .orElse(null);
        }

        private void clearRefreshTokenCookie(HttpServletResponse response) {
                Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, null);
                cookie.setHttpOnly(true);
                cookie.setSecure(false);
                cookie.setPath("/");
                cookie.setMaxAge(0);
                response.addCookie(cookie);
        }
}
