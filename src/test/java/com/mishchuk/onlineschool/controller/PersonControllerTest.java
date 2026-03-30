package com.mishchuk.onlineschool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishchuk.onlineschool.controller.dto.*;
import com.mishchuk.onlineschool.exception.GlobalExceptionHandler;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.service.PersonService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PersonController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PersonService personService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    // СЕКЦІЯ: POST /persons

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("POST /persons — авторизована роль → 201 Created")
    void createPerson_authorizedRole_returns201(String role) throws Exception {
        doNothing().when(personService).createPerson(any());

        mockMvc.perform(post("/persons")
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isCreated());

        verify(personService, times(1)).createPerson(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("POST /persons — неавторизована роль → 403 Forbidden")
    void createPerson_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(post("/persons")
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(personService);
    }

    @Test
    @DisplayName("POST /persons — анонімний → 403 Forbidden")
    void createPerson_anonymous_returns403() throws Exception {
        mockMvc.perform(post("/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: GET /persons/{id}

    @Test
    @DisplayName("GET /persons/{id} — знайдено → 200 OK")
    @WithMockUser
    void getPerson_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.getPerson(eq(id))).thenReturn(Optional.of(personDto(id, "John")));

        mockMvc.perform(get("/persons/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @DisplayName("GET /persons/{id} — не знайдено → 404 Not Found")
    @WithMockUser
    void getPerson_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.getPerson(eq(id))).thenReturn(Optional.empty());

        mockMvc.perform(get("/persons/{id}", id))
                .andExpect(status().isNotFound());
    }

    // СЕКЦІЯ: GET /persons

    @Test
    @DisplayName("GET /persons — повертає список → 200 OK")
    @WithMockUser
    void getAllPersons_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.getAllPersons()).thenReturn(List.of(personDto(id, "John")));

        mockMvc.perform(get("/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    @DisplayName("GET /persons — порожній список → 204 No Content")
    @WithMockUser
    void getAllPersons_empty_returns204() throws Exception {
        when(personService.getAllPersons()).thenReturn(List.of());

        mockMvc.perform(get("/persons"))
                .andExpect(status().isNoContent());
    }

    // СЕКЦІЯ: PUT /persons/{id}

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("PUT /persons/{id} — авторизована роль → 204 No Content")
    void updatePerson_authorizedRole_returns204(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(personService).updatePerson(eq(id), any());

        mockMvc.perform(put("/persons/{id}", id)
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isNoContent());

        verify(personService, times(1)).updatePerson(eq(id), any());
    }

    @Test
    @DisplayName("PUT /persons/{id} — не знайдено → 404 Not Found")
    @WithMockUser(roles = "ADMIN")
    void updatePerson_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("Not found")).when(personService).updatePerson(any(), any());

        mockMvc.perform(put("/persons/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("PUT /persons/{id} — неавторизована роль → 403 Forbidden")
    void updatePerson_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(put("/persons/{id}", UUID.randomUUID())
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(personService);
    }

    @Test
    @DisplayName("PUT /persons/{id} — анонімний → 403 Forbidden")
    void updatePerson_anonymous_returns403() throws Exception {
        mockMvc.perform(put("/persons/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: DELETE /persons/{id}

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("DELETE /persons/{id} — авторизована роль → 204 No Content")
    void deletePerson_authorizedRole_returns204(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(personService).deletePerson(id);

        mockMvc.perform(delete("/persons/{id}", id)
                        .with(user("u").roles(role)))
                .andExpect(status().isNoContent());

        verify(personService, times(1)).deletePerson(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("DELETE /persons/{id} — неавторизована роль → 403 Forbidden")
    void deletePerson_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(delete("/persons/{id}", UUID.randomUUID())
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(personService);
    }

    @Test
    @DisplayName("DELETE /persons/{id} — анонімний → 403 Forbidden")
    void deletePerson_anonymous_returns403() throws Exception {
        mockMvc.perform(delete("/persons/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: GET /persons/with-enrollments

    @Test
    @DisplayName("GET /persons/with-enrollments — авторизований → 200 OK")
    @WithMockUser
    void getAllPersonsWithEnrollments_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.getAllPersonsWithEnrollments()).thenReturn(List.of(personWithEnrollmentsDto(id, "John")));

        mockMvc.perform(get("/persons/with-enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    // СЕКЦІЯ: GET /persons/paginated

    @Test
    @DisplayName("GET /persons/paginated — авторизований → 200 OK")
    @WithMockUser
    void getPaginatedPersons_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Page<PersonWithEnrollmentsDto> page = new PageImpl<>(List.of(personWithEnrollmentsDto(id, "John")));
        when(personService.getPaginatedPersons(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/persons/paginated")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].firstName").value("John"));
    }

    // СЕКЦІЯ: PATCH /persons/{id}/status

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("PATCH /persons/{id}/status — авторизована роль → 204 No Content")
    void updatePersonStatus_authorizedRole_returns204(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(personService).updatePersonStatus(id, "ACTIVE");

        mockMvc.perform(patch("/persons/{id}/status", id)
                        .param("status", "ACTIVE")
                        .with(user("u").roles(role)))
                .andExpect(status().isNoContent());

        verify(personService, times(1)).updatePersonStatus(id, "ACTIVE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("PATCH /persons/{id}/status — некоректний статус → 400 Bad Request")
    void updatePersonStatus_badRequest_returns400(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Invalid status")).when(personService).updatePersonStatus(id, "INVALID");

        mockMvc.perform(patch("/persons/{id}/status", id)
                        .param("status", "INVALID")
                        .with(user("u").roles(role)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("PATCH /persons/{id}/status — неавторизована роль → 403 Forbidden")
    void updatePersonStatus_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(patch("/persons/{id}/status", UUID.randomUUID())
                        .param("status", "ACTIVE")
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(personService);
    }

    @Test
    @DisplayName("PATCH /persons/{id}/status — анонімний → 403 Forbidden")
    void updatePersonStatus_anonymous_returns403() throws Exception {
        mockMvc.perform(patch("/persons/{id}/status", UUID.randomUUID())
                        .param("status", "ACTIVE"))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: POST /persons/{id}/enrollments/{courseId}

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("POST /persons/{id}/enrollments/{courseId} — авторизована роль → 201 Created")
    void addCourseAccess_authorizedRole_returns201(String role) throws Exception {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        doNothing().when(personService).addCourseAccess(userId, courseId);

        mockMvc.perform(post("/persons/{id}/enrollments/{courseId}", userId, courseId)
                        .with(user("u").roles(role)))
                .andExpect(status().isCreated());

        verify(personService, times(1)).addCourseAccess(userId, courseId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("POST /persons/{id}/enrollments/{courseId} — неавторизована роль → 403 Forbidden")
    void addCourseAccess_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(post("/persons/{id}/enrollments/{courseId}", UUID.randomUUID(), UUID.randomUUID())
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(personService);
    }

    @Test
    @DisplayName("POST /persons/{id}/enrollments/{courseId} — анонімний → 403 Forbidden")
    void addCourseAccess_anonymous_returns403() throws Exception {
        mockMvc.perform(post("/persons/{id}/enrollments/{courseId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: DELETE /persons/{id}/enrollments/{courseId}

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("DELETE /persons/{id}/enrollments/{courseId} — авторизована роль → 204 No Content")
    void removeCourseAccess_authorizedRole_returns204(String role) throws Exception {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        doNothing().when(personService).removeCourseAccess(userId, courseId);

        mockMvc.perform(delete("/persons/{id}/enrollments/{courseId}", userId, courseId)
                        .with(user("u").roles(role)))
                .andExpect(status().isNoContent());

        verify(personService, times(1)).removeCourseAccess(userId, courseId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("DELETE /persons/{id}/enrollments/{courseId} — неавторизована роль → 403 Forbidden")
    void removeCourseAccess_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(delete("/persons/{id}/enrollments/{courseId}", UUID.randomUUID(), UUID.randomUUID())
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(personService);
    }

    @Test
    @DisplayName("DELETE /persons/{id}/enrollments/{courseId} — анонімний → 403 Forbidden")
    void removeCourseAccess_anonymous_returns403() throws Exception {
        mockMvc.perform(delete("/persons/{id}/enrollments/{courseId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    // ХЕЛПЕРИ (FACTORY METHODS)

    @NotNull
    @Contract("_, _ -> new")
    private PersonDto personDto(UUID id, String firstName) {
        return new PersonDto(id, firstName, "LastName", null, "123", "test@test.com", "UK", "USER", "ACTIVE", null, null, null);
    }

    @NotNull
    @Contract("_, _ -> new")
    private PersonWithEnrollmentsDto personWithEnrollmentsDto(UUID id, String firstName) {
        return new PersonWithEnrollmentsDto(id, firstName, "LastName", null, "123", "test@test.com", "UK", "USER", "ACTIVE", List.of(), null, null, null);
    }

    @NotNull
    @Contract(" -> new")
    private PersonCreateDto createDto() {
        return new PersonCreateDto("John", "Doe", null, "123", "test@test.com", "pass", "UK", List.of(UUID.randomUUID()));
    }

    @NotNull
    @Contract(" -> new")
    private PersonUpdateDto updateDto() {
        return new PersonUpdateDto("USER", "John", "Doe", null, "123", "test@test.com", "pass", "ACTIVE", "UK");
    }
}
