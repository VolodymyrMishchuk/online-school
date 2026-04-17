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
 * Integration tests for ModuleController and LessonController.
 *
 * ModuleController:
 *  — GET /modules (public) → 200
 *  — GET /modules/{id} (public) → 404 for unknown
 *  — GET /modules/{id}/lessons (public) → 200
 *  — POST /modules (ADMIN only) → 403 for USER, 401 no auth
 *  — PUT /modules/{id} (ADMIN only) → 403 for USER
 *  — DELETE /modules/{id} (ADMIN only) → 403 for USER
 *
 * LessonController:
 *  — GET /lessons/{id} (public) → 404 for unknown
 *  — GET /lessons (ADMIN only) → 403 for USER, 401 no auth
 *  — POST /lessons (ADMIN only) → 403 for USER, 401 no auth
 *  — DELETE /lessons/{id} (ADMIN only) → 403 for USER
 */
class ModuleAndLessonIntegrationTest extends AbstractIntegrationTest {

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = AuthHelper.randomEmail();
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     email,
                                "password",  "Password1!",
                                "firstName", "Module",
                                "lastName",  "User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = body.get("accessToken").asText();
    }

    // ════════════════════════════════════════════════════════
    //  MODULE CONTROLLER
    // ════════════════════════════════════════════════════════

    // ─────────────────────── GET /modules ───────────────────────

    @Test
    @DisplayName("GET /modules — публічний ендпоінт повертає 200")
    void getModules_public_returns200() throws Exception {
        mockMvc.perform(get("/modules"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /modules?courseId — публічний ендпоінт з courseId повертає 200")
    void getModulesByCourse_public_returns200() throws Exception {
        mockMvc.perform(get("/modules")
                        .param("courseId", "00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isOk());
    }

    // ─────────────────────── GET /modules/{id} ───────────────────────

    @Test
    @DisplayName("GET /modules/{id} — неіснуючий модуль повертає 404")
    void getModule_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/modules/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────── GET /modules/{id}/lessons ───────────────────────

    @Test
    @DisplayName("GET /modules/{id}/lessons — публічний ендпоінт повертає 200")
    void getModuleLessons_public_returns200() throws Exception {
        mockMvc.perform(get("/modules/00000000-0000-0000-0000-000000000001/lessons"))
                .andExpect(status().isOk());
    }

    // ─────────────────────── POST /modules (ADMIN only) ───────────────────────

    @Test
    @DisplayName("POST /modules — USER роль отримує 403")
    void createModule_userRole_returns403() throws Exception {
        mockMvc.perform(post("/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name",     "New Module",
                                "courseId", "00000000-0000-0000-0000-000000000001"
                        )))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /modules — без авторизації повертає 401")
    void createModule_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name",     "New Module",
                                "courseId", "00000000-0000-0000-0000-000000000001"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── PUT /modules/{id} (ADMIN only) ───────────────────────

    @Test
    @DisplayName("PUT /modules/{id} — USER роль отримує 403")
    void updateModule_userRole_returns403() throws Exception {
        mockMvc.perform(put("/modules/00000000-0000-0000-0000-000000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Updated")))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────── DELETE /modules/{id} (ADMIN only) ───────────────────────

    @Test
    @DisplayName("DELETE /modules/{id} — USER роль отримує 403")
    void deleteModule_userRole_returns403() throws Exception {
        mockMvc.perform(delete("/modules/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /modules/{id} — без авторизації повертає 401")
    void deleteModule_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/modules/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    // ════════════════════════════════════════════════════════
    //  LESSON CONTROLLER
    // ════════════════════════════════════════════════════════

    // ─────────────────────── GET /lessons/{id} ───────────────────────

    @Test
    @DisplayName("GET /lessons/{id} — неіснуючий урок повертає 404")
    void getLesson_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/lessons/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /lessons/{id} — публічний, не вимагає авторизації")
    void getLesson_public_notReturns401() throws Exception {
        // 404 (немає уроку), але НЕ 401
        mockMvc.perform(get("/lessons/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────── GET /lessons (ADMIN only) ───────────────────────

    @Test
    @DisplayName("GET /lessons — USER роль отримує 403")
    void getAllLessons_userRole_returns403() throws Exception {
        mockMvc.perform(get("/lessons")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /lessons — без авторизації повертає 401")
    void getAllLessons_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/lessons"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── GET /lessons/unassigned (ADMIN only) ───────────────────────

    @Test
    @DisplayName("GET /lessons/unassigned — USER роль отримує 403")
    void getUnassignedLessons_userRole_returns403() throws Exception {
        mockMvc.perform(get("/lessons/unassigned")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────── POST /lessons (ADMIN only) ───────────────────────

    @Test
    @DisplayName("POST /lessons — USER роль отримує 403")
    void createLesson_userRole_returns403() throws Exception {
        mockMvc.perform(post("/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title",   "New Lesson",
                                "content", "Lesson content"
                        )))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /lessons — без авторизації повертає 401")
    void createLesson_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title",   "New Lesson",
                                "content", "Lesson content"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── DELETE /lessons/{id} (ADMIN only) ───────────────────────

    @Test
    @DisplayName("DELETE /lessons/{id} — USER роль отримує 403")
    void deleteLesson_userRole_returns403() throws Exception {
        mockMvc.perform(delete("/lessons/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /lessons/{id} — без авторизації повертає 401")
    void deleteLesson_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/lessons/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── GET /lessons/{id}/files ───────────────────────

    @Test
    @DisplayName("GET /lessons/{id}/files — публічний ендпоінт повертає 200")
    void getLessonFiles_public_returns200() throws Exception {
        mockMvc.perform(get("/lessons/00000000-0000-0000-0000-000000000001/files"))
                .andExpect(status().isOk());
    }
}
