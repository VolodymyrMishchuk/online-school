package com.mishchuk.onlineschool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.repository.entity.PersonStatus;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReferenceController.class)
@Import(TestSecurityConfig.class)
class ReferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    // СЕКЦІЯ: GET /references/roles

    @Test
    @DisplayName("GET /references/roles — повертає список ролей → 200 OK")
    @WithMockUser
    void getRoles_returns200() throws Exception {
        mockMvc.perform(get("/references/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value(PersonRole.USER.name()));
    }

    @Test
    @DisplayName("GET /references/roles — анонімний доступ (дозволено) → 200 OK")
    void getRoles_anonymous_returns200() throws Exception {
        mockMvc.perform(get("/references/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // СЕКЦІЯ: GET /references/statuses

    @Test
    @DisplayName("GET /references/statuses — повертає список статусів → 200 OK")
    @WithMockUser
    void getStatuses_returns200() throws Exception {
        mockMvc.perform(get("/references/statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value(PersonStatus.ACTIVE.name()));
    }

    @Test
    @DisplayName("GET /references/statuses — анонімний доступ (дозволено) → 200 OK")
    void getStatuses_anonymous_returns200() throws Exception {
        mockMvc.perform(get("/references/statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
