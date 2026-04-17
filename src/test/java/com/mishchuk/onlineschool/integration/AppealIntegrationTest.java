package com.mishchuk.onlineschool.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AppealController.
 *
 * Covered scenarios:
 *  — POST /appeals/public (no auth) → 201
 *  — POST /appeals (no auth) → 401, (auth) → 201
 *  — GET /appeals (ADMIN only) → 403 for USER, 401 without auth
 *  — GET /appeals/{id} (ADMIN only) → 403 for USER
 *  — PATCH /appeals/{id}/status (ADMIN only) → 403 for USER
 *  — DELETE /appeals/{id} (ADMIN only) → 403 for USER
 */
class AppealIntegrationTest extends AbstractIntegrationTest {

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = AuthHelper.randomEmail();
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     email,
                                "password",  "Password1!",
                                "firstName", "Appeal",
                                "lastName",  "User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = body.get("accessToken").asText();
    }

    // ─────────────────────── POST /appeals/public ───────────────────────

    @Test
    @DisplayName("POST /appeals/public — анонімний користувач може надіслати звернення (201)")
    void createPublicAppeal_noAuth_returns201() throws Exception {
        mockMvc.perform(multipart("/appeals/public")
                        .param("name",    "John Doe")
                        .param("email",   "john@example.com")
                        .param("subject", "Question about course")
                        .param("message", "Hello, I have a question.")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }

    // ─────────────────────── POST /appeals ───────────────────────

    @Test
    @DisplayName("POST /appeals — без авторизації повертає 401")
    void createAppeal_noAuth_returns401() throws Exception {
        mockMvc.perform(multipart("/appeals")
                        .param("subject", "My question")
                        .param("message", "I need help.")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /appeals — авторизований може надіслати звернення (201)")
    void createAppeal_authenticated_returns201() throws Exception {
        mockMvc.perform(multipart("/appeals")
                        .param("subject", "Course access issue")
                        .param("message", "I cannot access the lesson.")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }

    // ─────────────────────── GET /appeals (ADMIN only) ───────────────────────

    @Test
    @DisplayName("GET /appeals — без авторизації повертає 401")
    void getAppeals_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/appeals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /appeals — USER роль отримує 403")
    void getAppeals_userRole_returns403() throws Exception {
        mockMvc.perform(get("/appeals")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────── GET /appeals/{id} (ADMIN only) ───────────────────────

    @Test
    @DisplayName("GET /appeals/{id} — USER роль отримує 403")
    void getAppeal_userRole_returns403() throws Exception {
        mockMvc.perform(get("/appeals/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /appeals/{id} — без авторизації повертає 401")
    void getAppeal_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/appeals/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── PATCH /appeals/{id}/status (ADMIN only) ───────────────────────

    @Test
    @DisplayName("PATCH /appeals/{id}/status — USER роль отримує 403")
    void updateStatus_userRole_returns403() throws Exception {
        mockMvc.perform(patch("/appeals/00000000-0000-0000-0000-000000000001/status")
                        .param("status", "RESOLVED")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────── DELETE /appeals/{id} (ADMIN only) ───────────────────────

    @Test
    @DisplayName("DELETE /appeals/{id} — USER роль отримує 403")
    void deleteAppeal_userRole_returns403() throws Exception {
        mockMvc.perform(delete("/appeals/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /appeals/{id} — без авторизації повертає 401")
    void deleteAppeal_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/appeals/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }
}
