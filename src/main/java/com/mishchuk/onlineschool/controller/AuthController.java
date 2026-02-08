package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.AuthRequest;
import com.mishchuk.onlineschool.controller.dto.AuthResponse;
import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import java.util.UUID;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.service.PersonService;
import com.mishchuk.onlineschool.service.RefreshTokenService;
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

                // Повертаємо 200 OK замість 201 Created
                return ResponseEntity.ok(new AuthResponse(accessToken, person.getId(), person.getRole().name()));
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

                return ResponseEntity.ok(new AuthResponse(accessToken, person.getId(), person.getRole().name()));
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

                        return ResponseEntity
                                        .ok(new AuthResponse(newAccessToken, person.id(), person.role()));
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
                                refreshTokenService.deleteByPersonId(tokenEntity.getPersonId());
                                log.info("User logged out successfully");
                        } catch (Exception e) {
                                log.warn("Error during logout: {}", e.getMessage());
                        }
                }

                clearRefreshTokenCookie(response);
                return ResponseEntity.noContent().build();
        }

        // Helper methods for cookie management

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
