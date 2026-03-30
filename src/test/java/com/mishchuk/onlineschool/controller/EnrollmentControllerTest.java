package com.mishchuk.onlineschool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishchuk.onlineschool.controller.dto.EnrollmentCreateDto;
import com.mishchuk.onlineschool.controller.dto.EnrollmentDto;
import com.mishchuk.onlineschool.exception.GlobalExceptionHandler;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.service.EnrollmentService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnrollmentService enrollmentService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    // СЕКЦІЯ: POST /enrollments

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("POST /enrollments — авторизована роль → 201 Created")
    void createEnrollment_authorizedRole_returns201(String role) throws Exception {
        doNothing().when(enrollmentService).createEnrollment(any());

        mockMvc.perform(post("/enrollments")
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isCreated());

        verify(enrollmentService, times(1)).createEnrollment(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("POST /enrollments — помилка при створенні → 400 Bad Request")
    void createEnrollment_badRequest_returns400(String role) throws Exception {
        doThrow(new RuntimeException("Already enrolled")).when(enrollmentService).createEnrollment(any());

        mockMvc.perform(post("/enrollments")
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("POST /enrollments — неавторизована роль → 403 Forbidden")
    void createEnrollment_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(post("/enrollments")
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(enrollmentService);
    }

    @Test
    @DisplayName("POST /enrollments — анонімний → 403 Forbidden")
    void createEnrollment_anonymous_returns403() throws Exception {
        mockMvc.perform(post("/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: GET /enrollments

    @Test
    @DisplayName("GET /enrollments — всі зарахування (авторизований) → 200 OK")
    @WithMockUser
    void getAllEnrollments_returns200() throws Exception {
        when(enrollmentService.getAllEnrollments()).thenReturn(List.of(enrollmentDto()));

        mockMvc.perform(get("/enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].courseName").value("Test Course"));

        verify(enrollmentService, times(1)).getAllEnrollments();
    }

    @Test
    @DisplayName("GET /enrollments?studentId={id} — записи студента (авторизований) → 200 OK")
    @WithMockUser
    void getEnrollmentsByStudent_returns200() throws Exception {
        UUID studentId = UUID.randomUUID();
        when(enrollmentService.getEnrollmentsByStudent(eq(studentId))).thenReturn(List.of(enrollmentDto()));

        mockMvc.perform(get("/enrollments").param("studentId", studentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseName").value("Test Course"));

        verify(enrollmentService, times(1)).getEnrollmentsByStudent(eq(studentId));
    }

    @Test
    @DisplayName("GET /enrollments — неавторизований (анонімний) → 200 OK (TestSecurityConfig permits all unless method secured)")
    void getAllEnrollments_anonymous_returns200() throws Exception {
        mockMvc.perform(get("/enrollments"))
                .andExpect(status().isOk());
    }

    // ХЕЛПЕРИ (FACTORY METHODS)

    @NotNull
    @Contract(" -> new")
    private EnrollmentDto enrollmentDto() {
        return new EnrollmentDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test Course",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    @NotNull
    @Contract(" -> new")
    private EnrollmentCreateDto createDto() {
        return new EnrollmentCreateDto(UUID.randomUUID(), UUID.randomUUID());
    }
}
