package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.AuthRequest;
import com.mishchuk.onlineschool.controller.dto.AuthResponse;
import com.mishchuk.onlineschool.controller.dto.AuthResultDto;
import com.mishchuk.onlineschool.controller.dto.ChangePasswordRequest;
import com.mishchuk.onlineschool.controller.dto.ForgotPasswordRequest;
import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.ResetPasswordRequest;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.service.AuthService;
import com.mishchuk.onlineschool.service.PasswordResetService;
import com.mishchuk.onlineschool.service.PersonService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

        private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
        private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

        private final AuthService authService;
        private final PasswordResetService passwordResetService;
        private final PersonService personService;
        private final UserDetailsService userDetailsService;

        @PostMapping("/register")
        public ResponseEntity<AuthResponse> register(
                        @Valid @RequestBody PersonCreateDto request,
                        HttpServletResponse response) {
                log.info("Registration request received for email: {}", request.email());

                AuthResultDto result = authService.registerUser(request);
                setRefreshTokenCookie(response, result.refreshToken());

                return ResponseEntity.ok(result.authResponse());
        }

        @PostMapping("/login")
        public ResponseEntity<AuthResponse> login(
                        @RequestBody AuthRequest request,
                        HttpServletResponse response) {
                log.info("Login request received for email: {}", request.getEmail());

                AuthResultDto result = authService.authenticateUser(request);
                setRefreshTokenCookie(response, result.refreshToken());

                return ResponseEntity.ok(result.authResponse());
        }

        @PostMapping("/refresh")
        public ResponseEntity<AuthResponse> refreshToken(
                        HttpServletRequest request,
                        HttpServletResponse response) {
                log.info("Token refresh request received");

                String refreshToken = getRefreshTokenFromCookie(request);
                if (refreshToken == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                try {
                        AuthResultDto result = authService.refreshToken(refreshToken);
                        setRefreshTokenCookie(response, result.refreshToken());
                        return ResponseEntity.ok(result.authResponse());
                } catch (Exception e) {
                        clearRefreshTokenCookie(response);
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
        }

        @PostMapping("/logout")
        public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
                log.info("Logout request received");

                String refreshToken = getRefreshTokenFromCookie(request);
                authService.logout(refreshToken);

                clearRefreshTokenCookie(response);
                return ResponseEntity.noContent().build();
        }

        @PostMapping("/magic-login")
        public ResponseEntity<AuthResponse> magicLogin(@RequestBody Map<String, String> request,
                        HttpServletResponse response) {
                String token = request.get("token");
                AuthResultDto result = authService.magicLogin(token);

                if (result == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                setRefreshTokenCookie(response, result.refreshToken());
                return ResponseEntity.ok(result.authResponse());
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

        @PostMapping("/change-password")
        public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                        java.security.Principal principal) {
                log.info("Change password request received for user: {}", principal.getName());
                PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(principal.getName());
                personService.changePassword(person.getId(), request.oldPassword(), request.newPassword());
                return ResponseEntity.ok().build();
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
