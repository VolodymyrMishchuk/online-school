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
 * Integration tests for NotificationController.
 *
 * Covered scenarios:
 *  — GET /notifications — без авторизації 401, з авторизацією 200
 *  — GET /notifications/unread-count — 401 / 200
 *  — PUT /notifications/read-all — 401 / 200
 *  — PUT /notifications/unread-all — 401 / 200
 *  — DELETE /notifications/all — 401 / 204
 *  — POST /notifications/broadcast — USER 403, без авторизації NPE/4xx
 */
class NotificationIntegrationTest extends AbstractIntegrationTest {

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = AuthHelper.randomEmail();
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     email,
                                "password",  "Password1!",
                                "firstName", "Notify",
                                "lastName",  "User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = body.get("accessToken").asText();
    }

    // ─────────────────────── GET /notifications ───────────────────────

    @Test
    @DisplayName("GET /notifications — без авторизації повертає 401")
    void getNotifications_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /notifications — авторизований повертає 200 зі списком")
    void getNotifications_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ─────────────────────── GET /notifications/unread-count ───────────────────────

    @Test
    @DisplayName("GET /notifications/unread-count — без авторизації повертає 401")
    void getUnreadCount_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /notifications/unread-count — авторизований повертає 200 з числом")
    void getUnreadCount_authenticated_returns200() throws Exception {
        MvcResult result = mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(Long.parseLong(body)).isGreaterThanOrEqualTo(0L);
    }

    // ─────────────────────── PUT /notifications/read-all ───────────────────────

    @Test
    @DisplayName("PUT /notifications/read-all — без авторизації повертає 401")
    void markAllAsRead_noAuth_returns401() throws Exception {
        mockMvc.perform(put("/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /notifications/read-all — авторизований повертає 200")
    void markAllAsRead_authenticated_returns200() throws Exception {
        mockMvc.perform(put("/notifications/read-all")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    // ─────────────────────── PUT /notifications/unread-all ───────────────────────

    @Test
    @DisplayName("PUT /notifications/unread-all — без авторизації повертає 401")
    void markAllAsUnread_noAuth_returns401() throws Exception {
        mockMvc.perform(put("/notifications/unread-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /notifications/unread-all — авторизований повертає 200")
    void markAllAsUnread_authenticated_returns200() throws Exception {
        mockMvc.perform(put("/notifications/unread-all")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    // ─────────────────────── DELETE /notifications/all ───────────────────────

    @Test
    @DisplayName("DELETE /notifications/all — без авторизації повертає 401")
    void deleteAll_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/notifications/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /notifications/all — авторизований повертає 204")
    void deleteAll_authenticated_returns204() throws Exception {
        mockMvc.perform(delete("/notifications/all")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    // ─────────────────────── POST /notifications/broadcast (ADMIN only) ───────────────────────

    @Test
    @DisplayName("POST /notifications/broadcast — USER роль отримує 403")
    void broadcast_userRole_returns403() throws Exception {
        mockMvc.perform(post("/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title",   "Test notification",
                                "message", "Hello all users"
                        )))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /notifications/broadcast — без авторизації повертає 401")
    void broadcast_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title",   "Test notification",
                                "message", "Hello all users"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── POST /notifications/send-to-users (ADMIN only) ───────────────────────

    @Test
    @DisplayName("POST /notifications/send-to-users — USER роль отримує 403")
    void sendToUsers_userRole_returns403() throws Exception {
        mockMvc.perform(post("/notifications/send-to-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title",   "Hello",
                                "message", "World",
                                "userIds", new String[]{}
                        )))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
