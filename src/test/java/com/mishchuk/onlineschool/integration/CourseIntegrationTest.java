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
 * Integration tests for Courses and Enrollments.
 *
 * Covered scenarios:
 *  — GET /courses (public) → no auth required
 *  — GET /courses/{id} (public) → 404 for non-existent
 *  — GET /courses?userId=... → requires auth, own data or admin
 *  — Security: protected endpoints return 401/403 without token
 */
class CourseIntegrationTest extends AbstractIntegrationTest {

    private String userToken;
    private String userEmail;

    @BeforeEach
    void setUp() throws Exception {
        userEmail = AuthHelper.randomEmail();
        userToken = AuthHelper.registerAndLogin(mockMvc, objectMapper, userEmail, "Password1!");
    }

    // ─────────────────────── GET /courses (public) ───────────────────────

    @Test
    @DisplayName("GET /courses — публічний ендпоінт не вимагає авторизації")
    void getCourses_noAuth_returns2xx() throws Exception {
        // Без токена — може повернути 200 (є курси) або 204 (немає курсів)
        mockMvc.perform(get("/courses"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("GET /courses — з авторизацією теж повертає 2xx")
    void getCourses_withAuth_returns2xx() throws Exception {
        mockMvc.perform(get("/courses")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().is2xxSuccessful());
    }

    // ─────────────────────── GET /courses/{id} ───────────────────────

    @Test
    @DisplayName("GET /courses/{id} — неіснуючий курс повертає 404")
    void getCourse_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/courses/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courses/{id} — публічний ендпоінт не вимагає авторизації")
    void getCourse_noAuth_notReturns401() throws Exception {
        // 404 (немає курсу) — але НЕ 401 (не вимагає авторизації)
        mockMvc.perform(get("/courses/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────── GET /courses?userId ───────────────────────

    @Test
    @DisplayName("GET /courses?userId — без авторизації повертає 401")
    void getCoursesByUserId_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/courses")
                        .param("userId", "00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /courses?userId — власні дані (userId = свій id) повертає 2xx")
    void getCoursesByUserId_ownData_returns2xx() throws Exception {
        // Отримуємо свій userId з токена (через login response)
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",    userEmail,
                                "password", "Password1!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String userId = body.get("personId").asText();

        mockMvc.perform(get("/courses")
                        .param("userId", userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("GET /courses?userId — чужий userId повертає 403")
    void getCoursesByUserId_otherUser_returns403() throws Exception {
        mockMvc.perform(get("/courses")
                        .param("userId", "00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────── POST /courses (ADMIN only) ───────────────────────

    @Test
    @DisplayName("POST /courses — USER роль отримує 403")
    void createCourse_userRole_returns403() throws Exception {
        mockMvc.perform(multipart("/courses")
                        .param("course", objectMapper.writeValueAsString(Map.of(
                                "name", "Test Course",
                                "description", "Test"
                        )))
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /courses — без авторизації повертає 401")
    void createCourse_noAuth_returns401() throws Exception {
        mockMvc.perform(multipart("/courses")
                        .param("course", "{\"name\":\"Test\"}")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── DELETE /courses/{id} (ADMIN only) ───────────────────────

    @Test
    @DisplayName("DELETE /courses/{id} — USER роль отримує 403")
    void deleteCourse_userRole_returns403() throws Exception {
        mockMvc.perform(delete("/courses/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /courses/{id} — без авторизації повертає 401")
    void deleteCourse_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/courses/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }
}
