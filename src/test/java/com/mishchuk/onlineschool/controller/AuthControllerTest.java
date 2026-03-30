package com.mishchuk.onlineschool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishchuk.onlineschool.controller.dto.*;
import com.mishchuk.onlineschool.exception.GlobalExceptionHandler;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.service.AuthService;
import com.mishchuk.onlineschool.service.PasswordResetService;
import com.mishchuk.onlineschool.service.PersonService;
import jakarta.servlet.http.Cookie;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private PersonService personService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    // СЕКЦІЯ: POST /auth/register

    @Test
    @DisplayName("POST /auth/register — валідний запит → 200 OK + accessToken + HttpOnly cookie")
    void register_validRequest_returns200WithTokenAndCookie() throws Exception {
        AuthResultDto result = authResult();
        when(authService.registerUser(any())).thenReturn(result);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personCreateDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.userId").value("00000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(cookie().value("refreshToken", "refresh-token-456"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/"));

        verify(authService, times(1)).registerUser(any());
    }

    @Test
    @DisplayName("POST /auth/register — невалідний запит (порожній email) → 400 Bad Request")
    void register_blankEmail_returns400() throws Exception {
        PersonCreateDto invalid = new PersonCreateDto(
                "Doe",
                "John",
                null,
                "+380971234567",
                null,
                "Password1!",
                "eng",
                null
        );;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/register — анонімний запит дозволено (permitAll) → 200 OK")
    void register_anonymous_isPermitted() throws Exception {
        when(authService.registerUser(any())).thenReturn(authResult());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(personCreateDto())))
                .andExpect(status().isOk());
    }

    // СЕКЦІЯ: POST /auth/login

    @Test
    @DisplayName("POST /auth/login — коректні дані → 200 OK + accessToken + cookie")
    void login_validCredentials_returns200WithTokenAndCookie() throws Exception {
        when(authService.authenticateUser(any())).thenReturn(authResult());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(cookie().value("refreshToken", "refresh-token-456"))
                .andExpect(cookie().httpOnly("refreshToken", true));

        verify(authService, times(1)).authenticateUser(any());
    }

    @Test
    @DisplayName("POST /auth/login — невірні credentials (BadCredentialsException) → 401 Unauthorized")
    void login_badCredentials_returns401() throws Exception {
        when(authService.authenticateUser(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login — анонімний запит дозволено (permitAll) → 200 OK")
    void login_anonymous_isPermitted() throws Exception {
        when(authService.authenticateUser(any())).thenReturn(authResult());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest())))
                .andExpect(status().isOk());
    }

    // СЕКЦІЯ: POST /auth/refresh

    @Test
    @DisplayName("POST /auth/refresh — валідний cookie → 200 OK + оновлений токен і cookie")
    void refresh_validCookie_returns200WithNewToken() throws Exception {
        AuthResultDto result = new AuthResultDto(
                new AuthResponse("new-access-token", UUID.fromString("00000000-0000-0000-0000-000000000001"), "USER", "John", "Doe", "en"),
                "new-refresh-token"
        );
        when(authService.refreshToken("valid-refresh")).thenReturn(result);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(cookie().value("refreshToken", "new-refresh-token"));

        verify(authService, times(1)).refreshToken("valid-refresh");
    }

    @Test
    @DisplayName("POST /auth/refresh — відсутній cookie → 401 Unauthorized")
    void refresh_missingCookie_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/refresh — протермінований токен (RuntimeException) → 401 + cookie очищено (maxAge=0)")
    void refresh_expiredToken_returns401AndClearsCookie() throws Exception {
        when(authService.refreshToken(anyString()))
                .thenThrow(new RuntimeException("Token expired"));

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "expired-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    // СЕКЦІЯ: POST /auth/logout

    @Test
    @DisplayName("POST /auth/logout — з cookie → 204 No Content + cookie очищено (maxAge=0)")
    void logout_withCookie_returns204AndClearsCookie() throws Exception {
        doNothing().when(authService).logout("some-refresh-token");

        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", "some-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refreshToken", 0));

        verify(authService, times(1)).logout("some-refresh-token");
    }

    @Test
    @DisplayName("POST /auth/logout — без cookie → 204 No Content (logout все одно виконується з null)")
    void logout_withoutCookie_returns204() throws Exception {
        doNothing().when(authService).logout(null);

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent());

        verify(authService, times(1)).logout(null);
    }

    // СЕКЦІЯ: POST /auth/magic-login

    @Test
    @DisplayName("POST /auth/magic-login — валідний токен → 200 OK + cookie")
    void magicLogin_validToken_returns200() throws Exception {
        when(authService.magicLogin("valid-magic-token")).thenReturn(authResult());

        mockMvc.perform(post("/auth/magic-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "valid-magic-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(cookie().value("refreshToken", "refresh-token-456"));

        verify(authService, times(1)).magicLogin("valid-magic-token");
    }

    @Test
    @DisplayName("POST /auth/magic-login — сервіс повертає null → 401 Unauthorized")
    void magicLogin_serviceReturnsNull_returns401() throws Exception {
        when(authService.magicLogin(anyString())).thenReturn(null);

        mockMvc.perform(post("/auth/magic-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "invalid-token"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // СЕКЦІЯ: POST /auth/forgot-password

    @Test
    @DisplayName("POST /auth/forgot-password — валідний email → 200 OK")
    void forgotPassword_validEmail_returns200() throws Exception {
        doNothing().when(passwordResetService).initiatePasswordReset("user@example.com");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("user@example.com"))))
                .andExpect(status().isOk());

        verify(passwordResetService, times(1)).initiatePasswordReset("user@example.com");
    }

    @Test
    @DisplayName("POST /auth/forgot-password — невалідний email (@Email) → 400 Bad Request")
    void forgotPassword_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest("not-an-email"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }

    @Test
    @DisplayName("POST /auth/forgot-password — порожній email (@NotBlank) → 400 Bad Request")
    void forgotPassword_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest(""))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }

    // СЕКЦІЯ: POST /auth/reset-password

    @Test
    @DisplayName("POST /auth/reset-password — валідний запит → 200 OK")
    void resetPassword_validRequest_returns200() throws Exception {
        doNothing().when(passwordResetService).resetPassword("reset-token", "NewPass1!");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("reset-token", "NewPass1!"))))
                .andExpect(status().isOk());

        verify(passwordResetService, times(1)).resetPassword("reset-token", "NewPass1!");
    }

    @Test
    @DisplayName("POST /auth/reset-password — порожній токен (@NotBlank) → 400 Bad Request")
    void resetPassword_blankToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("", "NewPass1!"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }

    @Test
    @DisplayName("POST /auth/reset-password — занадто короткий пароль (@Size min=6) → 400 Bad Request")
    void resetPassword_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("reset-token", "abc"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }

    @Test
    @DisplayName("POST /auth/reset-password — протермінований токен (IllegalArgumentException) → 400 Bad Request")
    void resetPassword_expiredToken_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Token expired"))
                .when(passwordResetService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest("expired-token", "NewPass1!"))))
                .andExpect(status().isBadRequest());
    }

    // СЕКЦІЯ: POST /auth/change-password

    @Test
    @DisplayName("POST /auth/change-password — авторизований користувач → 200 OK")
    @WithMockUser(username = "user@example.com")
    void changePassword_authenticatedUser_returns200() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        PersonEntity person = mock(PersonEntity.class);
        when(person.getId()).thenReturn(userId);
        when(userDetailsService.getPerson("user@example.com"))
                .thenReturn(person);
        doNothing().when(personService).changePassword(eq(userId), eq("OldPass1!"), eq("NewPass1!"));

        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("OldPass1!", "NewPass1!"))))
                .andExpect(status().isOk());

        verify(personService, times(1)).changePassword(eq(userId), eq("OldPass1!"), eq("NewPass1!"));
    }

    @Test
    @DisplayName("POST /auth/change-password — анонімний → 401 Unauthorized (authenticated() у SecurityConfig)")
    void changePassword_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("OldPass1!", "NewPass1!"))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(personService);
    }

    @Test
    @DisplayName("POST /auth/change-password — порожній oldPassword (@NotBlank) → 400 Bad Request")
    @WithMockUser(username = "user@example.com")
    void changePassword_blankOldPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("", "NewPass1!"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(personService);
    }

    @Test
    @DisplayName("POST /auth/change-password — занадто короткий newPassword (@Size min=6) → 400 Bad Request")
    @WithMockUser(username = "user@example.com")
    void changePassword_shortNewPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("OldPass1!", "abc"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(personService);
    }

    @Test
    @DisplayName("POST /auth/change-password — невірний старий пароль (IllegalArgumentException) → 400 Bad Request")
    @WithMockUser(username = "user@example.com")
    void changePassword_wrongOldPassword_returns400() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        PersonEntity person = mock(PersonEntity.class);
        when(person.getId()).thenReturn(userId);
        when(userDetailsService.getPerson("user@example.com"))
                .thenReturn(person);
        doThrow(new IllegalArgumentException("Wrong password"))
                .when(personService).changePassword(any(), anyString(), anyString());

        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("WrongPass1!", "NewPass1!"))))
                .andExpect(status().isBadRequest());
    }

    // ХЕЛПЕРИ (FACTORY METHODS)

    @NotNull
    @Contract(" -> new")
    private AuthResultDto authResult() {
        AuthResponse authResponse = new AuthResponse(
                "access-token-123",
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "USER",
                "John",
                "Doe",
                "en"
        );
        return new AuthResultDto(authResponse, "refresh-token-456");
    }

    @NotNull
    @Contract(" -> new")
    private AuthRequest authRequest() {
        return AuthRequest.builder()
                .email("user@example.com")
                .password("Password1!")
                .build();
    }

    @NotNull
    @Contract(" -> new")
    private PersonCreateDto personCreateDto() {
        return new PersonCreateDto(
                "Doe",
                "John",
                OffsetDateTime.now(),
                "+380971234567",
                "user@example.com",
                "Password1!",
                "en",
                List.of(UUID.fromString("00000000-0000-0000-0000-000000000099"))
        );
    }
}
