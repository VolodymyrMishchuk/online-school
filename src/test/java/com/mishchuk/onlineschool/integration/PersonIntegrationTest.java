package com.mishchuk.onlineschool.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PersonController.
 *
 * Covered scenarios:
 *  — GET /persons (public) → list all persons
 *  — GET /persons/{id} (public) → 404 for unknown
 *  — PUT /persons/{id} (authenticated) → update own profile
 *  — PATCH /persons/{id}/language (authenticated)
 *  — POST /persons (ADMIN only) → 403 for USER
 *  — DELETE /persons/{id} (ADMIN only) → 403 for USER
 *  — GET /persons/paginated (public)
 *  — GET /persons/with-enrollments (public)
 */
class PersonIntegrationTest extends AbstractIntegrationTest {

    private String userToken;
    private String userId;
    private String userEmail;

    @BeforeEach
    void setUp() throws Exception {
        userEmail = AuthHelper.randomEmail();

        // Register + login → get token
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     userEmail,
                                "password",  "Password1!",
                                "firstName", "Test",
                                "lastName",  "User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        userToken = body.get("accessToken").asText();
        userId    = body.get("personId").asText();
    }

    // ─────────────────────── GET /persons ───────────────────────

    @Test
    @DisplayName("GET /persons — публічний, повертає список (2xx)")
    void getAllPersons_public_returns2xx() throws Exception {
        mockMvc.perform(get("/persons"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("GET /persons — з авторизацією теж повертає 2xx")
    void getAllPersons_withAuth_returns2xx() throws Exception {
        mockMvc.perform(get("/persons")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().is2xxSuccessful());
    }

    // ─────────────────────── GET /persons/{id} ───────────────────────

    @Test
    @DisplayName("GET /persons/{id} — існуючий користувач повертає 200")
    void getPerson_existing_returns200() throws Exception {
        MvcResult result = mockMvc.perform(get("/persons/" + userId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("id").asText()).isEqualTo(userId);
        assertThat(body.get("email").asText()).isEqualTo(userEmail);
    }

    @Test
    @DisplayName("GET /persons/{id} — неіснуючий id повертає 404")
    void getPerson_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/persons/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────── PUT /persons/{id} ───────────────────────

    @Test
    @DisplayName("PUT /persons/{id} — авторизований користувач може оновити профіль (204)")
    void updatePerson_authenticated_returns204() throws Exception {
        mockMvc.perform(put("/persons/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Updated",
                                "lastName",  "Name"
                        )))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /persons/{id} — без авторизації повертає 401")
    void updatePerson_noAuth_returns401() throws Exception {
        mockMvc.perform(put("/persons/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Updated",
                                "lastName",  "Name"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── PATCH /persons/{id}/language ───────────────────────

    @Test
    @DisplayName("PATCH /persons/{id}/language — авторизований користувач може змінити мову (204)")
    void updateLanguage_authenticated_returns204() throws Exception {
        mockMvc.perform(patch("/persons/" + userId + "/language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("language", "uk")))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /persons/{id}/language — без авторизації повертає 401")
    void updateLanguage_noAuth_returns401() throws Exception {
        mockMvc.perform(patch("/persons/" + userId + "/language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("language", "uk"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /persons/{id}/language — відсутній параметр language повертає 400")
    void updateLanguage_missingField_returns400() throws Exception {
        mockMvc.perform(patch("/persons/" + userId + "/language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────── POST /persons (ADMIN only) ───────────────────────

    @Test
    @DisplayName("POST /persons — USER роль отримує 403")
    void createPerson_userRole_returns403() throws Exception {
        mockMvc.perform(post("/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     AuthHelper.randomEmail(),
                                "password",  "Password1!",
                                "firstName", "New",
                                "lastName",  "Person"
                        )))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /persons — без авторизації повертає 401")
    void createPerson_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     AuthHelper.randomEmail(),
                                "password",  "Password1!",
                                "firstName", "New",
                                "lastName",  "Person"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── DELETE /persons/{id} (ADMIN only) ───────────────────────

    @Test
    @DisplayName("DELETE /persons/{id} — USER роль отримує 403")
    void deletePerson_userRole_returns403() throws Exception {
        mockMvc.perform(delete("/persons/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /persons/{id} — без авторизації повертає 401")
    void deletePerson_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/persons/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── GET /persons/paginated ───────────────────────

    @Test
    @DisplayName("GET /persons/paginated — публічний, повертає сторінку")
    void getPaginatedPersons_public_returns200() throws Exception {
        mockMvc.perform(get("/persons/paginated")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ─────────────────────── GET /persons/with-enrollments ───────────────────────

    @Test
    @DisplayName("GET /persons/with-enrollments — публічний, повертає список")
    void getPersonsWithEnrollments_public_returns200() throws Exception {
        mockMvc.perform(get("/persons/with-enrollments"))
                .andExpect(status().isOk());
    }

    // ─────────────────────── PATCH /persons/{id}/status (ADMIN only) ───────────────────────

    @Test
    @DisplayName("PATCH /persons/{id}/status — USER роль отримує 403")
    void updatePersonStatus_userRole_returns403() throws Exception {
        mockMvc.perform(patch("/persons/" + userId + "/status")
                        .param("status", "BLOCKED")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /persons/{id}/status — без авторизації повертає 401")
    void updatePersonStatus_noAuth_returns401() throws Exception {
        mockMvc.perform(patch("/persons/" + userId + "/status")
                        .param("status", "BLOCKED"))
                .andExpect(status().isUnauthorized());
    }
}
