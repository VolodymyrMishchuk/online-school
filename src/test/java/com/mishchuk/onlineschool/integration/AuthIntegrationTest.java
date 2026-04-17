package com.mishchuk.onlineschool.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Auth flow.
 *
 * Tests go through the full stack:
 *   HTTP → Security Filter → Controller → Service → JPA → PostgreSQL (Testcontainers)
 *
 * Covered scenarios:
 *  — register: happy path, duplicate email, invalid payload
 *  — login: happy path, wrong password, unknown email
 *  — refresh: valid cookie, missing cookie
 *  — logout: with and without token
 */
class AuthIntegrationTest extends AbstractIntegrationTest {

    // ─────────────────────── register ───────────────────────

    @Test
    @DisplayName("POST /auth/register — успішна реєстрація повертає 200 + accessToken")
    void register_validRequest_returns200WithToken() throws Exception {
        String email = AuthHelper.randomEmail();

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     email,
                                "password",  "Password1!",
                                "firstName", "John",
                                "lastName",  "Doe"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("firstName").asText()).isEqualTo("John");

        // Refresh token повинен бути встановлений як cookie
        String refreshCookie = AuthHelper.extractCookie(result.getResponse(), "refreshToken");
        assertThat(refreshCookie).isNotBlank();
    }

    @Test
    @DisplayName("POST /auth/register — дублікат email повертає 4xx")
    void register_duplicateEmail_returnsError() throws Exception {
        String email = AuthHelper.randomEmail();
        String body = objectMapper.writeValueAsString(Map.of(
                "email",     email,
                "password",  "Password1!",
                "firstName", "John",
                "lastName",  "Doe"
        ));

        // Перша реєстрація — успішна
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Друга реєстрація з тим самим email — має повернути помилку
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /auth/register — відсутній email повертає 400")
    void register_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password",  "Password1!",
                                "firstName", "John",
                                "lastName",  "Doe"
                        ))))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────── login ───────────────────────

    @Test
    @DisplayName("POST /auth/login — успішний логін повертає 200 + accessToken + cookie")
    void login_validCredentials_returns200WithTokenAndCookie() throws Exception {
        String email    = AuthHelper.randomEmail();
        String password = "Password1!";

        // Реєструємось
        AuthHelper.registerAndLogin(mockMvc, objectMapper, email, password);

        // Логінимось
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",    email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String refreshCookie = AuthHelper.extractCookie(result.getResponse(), "refreshToken");
        assertThat(refreshCookie).isNotBlank();
    }

    @Test
    @DisplayName("POST /auth/login — невірний пароль повертає 403")
    void login_wrongPassword_returns403() throws Exception {
        String email = AuthHelper.randomEmail();
        AuthHelper.registerAndLogin(mockMvc, objectMapper, email, "CorrectPass1!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",    email,
                                "password", "WrongPassword!"
                        ))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /auth/login — невідомий email повертає 4xx")
    void login_unknownEmail_returns4xx() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",    "nobody@ghost.com",
                                "password", "Password1!"
                        ))))
                .andExpect(status().is4xxClientError());
    }

    // ─────────────────────── refresh ───────────────────────

    @Test
    @DisplayName("POST /auth/refresh — валідний cookie повертає 200 + новий accessToken")
    void refresh_validCookie_returns200WithNewToken() throws Exception {
        String email    = AuthHelper.randomEmail();
        String password = "Password1!";

        // Реєструємось і отримуємо cookie
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     email,
                                "password",  password,
                                "firstName", "Test",
                                "lastName",  "User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        String refreshCookie = AuthHelper.extractCookie(registerResult.getResponse(), "refreshToken");

        // Використовуємо refreshToken для отримання нового accessToken
        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        assertThat(body.get("accessToken").asText()).isNotBlank();
    }

    @Test
    @DisplayName("POST /auth/refresh — без cookie повертає 401")
    void refresh_noCookie_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/refresh — невалідний refreshToken повертає 401")
    void refresh_invalidCookie_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "invalid.token.value")))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── logout ───────────────────────

    @Test
    @DisplayName("POST /auth/logout — з валідним cookie повертає 204")
    void logout_withValidCookie_returns204() throws Exception {
        String email = AuthHelper.randomEmail();

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     email,
                                "password",  "Password1!",
                                "firstName", "Test",
                                "lastName",  "User"
                        ))))
                .andReturn();

        String refreshCookie = AuthHelper.extractCookie(registerResult.getResponse(), "refreshToken");

        mockMvc.perform(post("/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshCookie)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /auth/logout — без cookie теж повертає 204 (graceful)")
    void logout_withoutCookie_returns204() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /auth/logout — після logout refresh token стає недійсним")
    void logout_thenRefresh_returns401() throws Exception {
        String email = AuthHelper.randomEmail();

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     email,
                                "password",  "Password1!",
                                "firstName", "Test",
                                "lastName",  "User"
                        ))))
                .andReturn();

        String refreshCookie = AuthHelper.extractCookie(registerResult.getResponse(), "refreshToken");

        // Logout
        mockMvc.perform(post("/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshCookie)))
                .andExpect(status().isNoContent());

        // Спроба refresh після logout — токен вже видалений
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshCookie)))
                .andExpect(status().isUnauthorized());
    }
}
