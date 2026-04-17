package com.mishchuk.onlineschool.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PromoCodeController.
 *
 * Covered scenarios:
 *  — GET /promo-codes/paginated (ADMIN only) → 403 USER, 401 no auth
 *  — POST /promo-codes (ADMIN only) → 403 USER, 401 no auth
 *  — GET /promo-codes/check (authenticated) → requires auth
 *  — POST /promo-codes/use (authenticated) → requires auth
 *  — PUT /promo-codes/{id} (ADMIN only) → 403 USER
 *  — DELETE /promo-codes/{id} (ADMIN only) → 403 USER, 401 no auth
 */
class PromoCodeIntegrationTest extends AbstractIntegrationTest {

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = AuthHelper.randomEmail();
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",     email,
                                "password",  "Password1!",
                                "firstName", "Promo",
                                "lastName",  "User"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        userToken = body.get("accessToken").asText();
    }

    // ─────────────────────── GET /promo-codes/paginated (ADMIN only) ───────────────────────

    @Test
    @DisplayName("GET /promo-codes/paginated — USER роль отримує 403")
    void getPaginated_userRole_returns403() throws Exception {
        mockMvc.perform(get("/promo-codes/paginated")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /promo-codes/paginated — без авторизації повертає 401")
    void getPaginated_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/promo-codes/paginated"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── POST /promo-codes (ADMIN only) ───────────────────────

    @Test
    @DisplayName("POST /promo-codes — USER роль отримує 403")
    void createPromoCode_userRole_returns403() throws Exception {
        mockMvc.perform(post("/promo-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code",            "TESTCODE",
                                "discountPercent", 10,
                                "maxUsages",       100
                        )))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /promo-codes — без авторизації повертає 401")
    void createPromoCode_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/promo-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code",            "TESTCODE",
                                "discountPercent", 10,
                                "maxUsages",       100
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── GET /promo-codes/check ───────────────────────

    @Test
    @DisplayName("GET /promo-codes/check — без авторизації повертає 401")
    void checkPromoCode_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/promo-codes/check")
                        .param("code", "NONEXISTENT"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /promo-codes/check — авторизований, неіснуючий код повертає 4xx")
    void checkPromoCode_authenticated_nonExistentCode_returns4xx() throws Exception {
        mockMvc.perform(get("/promo-codes/check")
                        .param("code", "NONEXISTENT_CODE_XYZ")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().is4xxClientError());
    }

    // ─────────────────────── POST /promo-codes/use ───────────────────────

    @Test
    @DisplayName("POST /promo-codes/use — без авторизації повертає 401")
    void usePromoCode_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/promo-codes/use")
                        .param("code",     "NONEXISTENT")
                        .param("courseId", "00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────── PUT /promo-codes/{id} (ADMIN only) ───────────────────────

    @Test
    @DisplayName("PUT /promo-codes/{id} — USER роль отримує 403")
    void updatePromoCode_userRole_returns403() throws Exception {
        mockMvc.perform(put("/promo-codes/00000000-0000-0000-0000-000000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code",            "UPDATED",
                                "discountPercent", 20,
                                "maxUsages",       50
                        )))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────── DELETE /promo-codes/{id} (ADMIN only) ───────────────────────

    @Test
    @DisplayName("DELETE /promo-codes/{id} — USER роль отримує 403")
    void deletePromoCode_userRole_returns403() throws Exception {
        mockMvc.perform(delete("/promo-codes/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /promo-codes/{id} — без авторизації повертає 401")
    void deletePromoCode_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/promo-codes/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }
}
