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
 * Integration tests for EnrollmentController.
 *
 * Covered scenarios:
 *  — POST /enrollments (authenticated) → 201 or 400 (already enrolled / bad course)
 *  — POST /enrollments (no auth) → 401
 *  — GET /enrollments (public) → 200
 *  — GET /enrollments?studentId=... (public) → 200
 */
class EnrollmentIntegrationTest extends AbstractIntegrationTest {

    private String userToken;
    private String userId;

    @BeforeEach
    void setUp() throws Exception {
        String email = AuthHelper.randomEmail();
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     email,
                                "password",  "Password1!",
                                "firstName", "Enroll",
                                "lastName",  "User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = body.get("accessToken").asText();
        userId    = body.get("personId").asText();
    }

    // ─────────────────────── GET /enrollments ───────────────────────

    @Test
    @DisplayName("GET /enrollments — публічний ендпоінт повертає 200")
    void getAllEnrollments_public_returns200() throws Exception {
        mockMvc.perform(get("/enrollments"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /enrollments — з авторизацією теж повертає 200")
    void getAllEnrollments_withAuth_returns200() throws Exception {
        mockMvc.perform(get("/enrollments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    // ─────────────────────── GET /enrollments?studentId ───────────────────────

    @Test
    @DisplayName("GET /enrollments?studentId — повертає список зарахувань конкретного студента")
    void getEnrollmentsByStudent_returns200() throws Exception {
        MvcResult result = mockMvc.perform(get("/enrollments")
                        .param("studentId", userId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
    }

    // ─────────────────────── POST /enrollments ───────────────────────

    @Test
    @DisplayName("POST /enrollments — без авторизації повертає 401")
    void createEnrollment_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "personId", userId,
                                "courseId", "00000000-0000-0000-0000-000000000001"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /enrollments — авторизований, неіснуючий курс повертає 400")
    void createEnrollment_nonExistentCourse_returns400() throws Exception {
        mockMvc.perform(post("/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "personId", userId,
                                "courseId", "00000000-0000-0000-0000-000000000001"
                        )))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }
}
