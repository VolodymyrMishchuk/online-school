package com.mishchuk.onlineschool.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ReferenceController and FileController (security layer).
 *
 * ReferenceController:
 *  — GET /references/roles (public) → 200
 *  — GET /references/statuses (public) → 200
 *
 * FileController:
 *  — GET /files/my-files (no auth) → 401, (auth) → 200
 *  — GET /files/entity/{type}/{id} (public) → 200
 *  — POST /files/upload (no auth) → still works (optionally auth), but MinIO mocked
 *  — DELETE /files/{id} (no real file) → 404 or 2xx depending on impl
 */
class ReferenceAndFileIntegrationTest extends AbstractIntegrationTest {

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = AuthHelper.randomEmail();
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     email,
                                "password",  "Password1!",
                                "firstName", "File",
                                "lastName",  "User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = body.get("accessToken").asText();
    }

    // ════════════════════════════════════════════════════════
    //  REFERENCE CONTROLLER
    // ════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /references/roles — публічний ендпоінт повертає 200 зі списком ролей")
    void getRoles_public_returns200() throws Exception {
        mockMvc.perform(get("/references/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /references/statuses — публічний ендпоінт повертає 200 зі списком статусів")
    void getStatuses_public_returns200() throws Exception {
        mockMvc.perform(get("/references/statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ════════════════════════════════════════════════════════
    //  FILE CONTROLLER
    // ════════════════════════════════════════════════════════

    // ─────────────────────── GET /files/my-files ───────────────────────

    @Test
    @DisplayName("GET /files/my-files — без авторизації повертає 401")
    void getMyFiles_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/files/my-files"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /files/my-files — авторизований повертає 200 зі списком файлів")
    void getMyFiles_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/files/my-files")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ─────────────────────── GET /files/entity/{type}/{id} ───────────────────────

    @Test
    @DisplayName("GET /files/entity/{type}/{id} — публічний ендпоінт повертає 200")
    void getFilesForEntity_public_returns200() throws Exception {
        mockMvc.perform(get("/files/entity/lesson/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isOk());
    }
}
