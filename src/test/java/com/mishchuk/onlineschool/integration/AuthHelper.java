package com.mishchuk.onlineschool.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Helper for integration tests that need authenticated HTTP calls.
 *
 * Provides:
 *  - registerAndLogin(email, password) → Bearer token
 *  - extractCookie(response, name)     → cookie value
 *  - randomEmail()                     → unique email per call
 */
public class AuthHelper {

    private AuthHelper() {}

    /**
     * Registers a new user and immediately logs in, returning the Bearer access token.
     * The refresh token is set as a cookie by the server (handled transparently).
     */
    public static String registerAndLogin(MockMvc mockMvc, ObjectMapper objectMapper,
                                          String email, String password) throws Exception {
        // 1. Register
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email",     email,
                "password",  password,
                "firstName", "Test",
                "lastName",  "User"
        ));

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    /**
     * Logs in with existing credentials and returns the Bearer access token.
     */
    public static String login(MockMvc mockMvc, ObjectMapper objectMapper,
                               String email, String password) throws Exception {
        String loginBody = objectMapper.writeValueAsString(Map.of(
                "email",    email,
                "password", password
        ));

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    /**
     * Extracts a cookie value from the MockHttpServletResponse by cookie name.
     */
    public static String extractCookie(MockHttpServletResponse response, String cookieName) {
        if (response.getCookies() == null) return null;
        for (jakarta.servlet.http.Cookie cookie : response.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Generates a unique email address for each test to avoid unique constraint conflicts
     * (tests run against the same shared Testcontainers DB without transaction rollback).
     */
    public static String randomEmail() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@integration.test";
    }
}
